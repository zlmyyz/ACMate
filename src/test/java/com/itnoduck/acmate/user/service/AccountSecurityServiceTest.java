package com.itnoduck.acmate.user.service;

import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.dto.GithubProfile;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;

class AccountSecurityServiceTest {
    AppUser passwordUser() {
        var current=user(1,"mail@example.com");current.setPasswordHash(encoder.encode("old-password"));
        when(users.selectById(1L)).thenReturn(current);return current;
    }
    @Test void changingPasswordRejectsWrongCurrentPasswordWithoutWriting() {
        passwordUser();
        assertThrows(BusinessException.class,()->service.changePassword(1,"wrong-password","new-password"));
        verify(jdbc,never()).update(anyString(),any(Object[].class));verifyNoInteractions(merger,email);
    }
    @Test void changingPasswordRejectsReusingTheSamePassword() {
        passwordUser();
        assertThrows(BusinessException.class,()->service.changePassword(1,"old-password","old-password"));
        verify(jdbc,never()).update(anyString(),any(Object[].class));verifyNoInteractions(merger);
    }
    @Test void changingPasswordHashesTheNewPasswordAndExpiresExistingSessions() {
        passwordUser();
        when(jdbc.update(eq("UPDATE app_user SET password_hash=? WHERE id=? AND status=1"),anyString(),eq(1L))).thenReturn(1);
        service.changePassword(1,"old-password","new-password");
        var hash=org.mockito.ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(eq("UPDATE app_user SET password_hash=? WHERE id=? AND status=1"),hash.capture(),eq(1L));
        assertTrue(encoder.matches("new-password",hash.getValue()));assertFalse(encoder.matches("old-password",hash.getValue()));
        verify(merger).expireSessions(1);verifyNoInteractions(email);
    }
    @Test void changingPasswordRejectsOversizedUnicodePasswordsBeforeHashing() {
        passwordUser();
        assertThrows(BusinessException.class,()->service.changePassword(1,"old-password","密".repeat(30)));
        verify(jdbc,never()).update(anyString(),any(Object[].class));
    }
    @Test void cannotUnlinkTheOnlyAvailableLoginMethod() {
        when(users.selectById(1L)).thenReturn(user(1,null));
        assertThrows(BusinessException.class,()->service.unlinkGithub(1));
        verify(jdbc,never()).update(anyString(),any(Object[].class));
        verifyNoInteractions(merger);
    }
    @Test void unlinkRequiresAnAvailableMailboxLogin() {
        when(users.selectById(1L)).thenReturn(user(1,"mail@example.com"));
        when(email.emailAvailable()).thenReturn(false);
        assertThrows(BusinessException.class,()->service.unlinkGithub(1));
        verify(jdbc,never()).update(anyString(),any(Object[].class));
    }
    @Test void unlinkDeletesOnlyCurrentUsersBindingAndExpiresOldSessions() {
        when(users.selectById(1L)).thenReturn(user(1,"mail@example.com"));
        when(email.emailAvailable()).thenReturn(true);
        when(jdbc.update("DELETE FROM user_login_identity WHERE provider='GITHUB' AND user_id=?",1L)).thenReturn(1);
        assertEquals("user1",service.unlinkGithub(1).username());
        verify(jdbc).update("DELETE FROM user_login_identity WHERE provider='GITHUB' AND user_id=?",1L);
        verify(merger).expireSessions(1);
    }
    final AppUserMapper users=mock(AppUserMapper.class);
    final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    final AlternativeLoginService email=mock(AlternativeLoginService.class);
    final AccountMergeService merger=mock(AccountMergeService.class);
    final BCryptPasswordEncoder encoder=new BCryptPasswordEncoder(4);
    final AccountSecurityService service=new AccountSecurityService(users,jdbc,email,encoder,merger);
    AppUser user(long id,String address) { var user=new AppUser(); user.setId(id);user.setUsername("user"+id);user.setEmail(address);user.setStatus(1);return user; }

    @Test void invalidResetCodeCannotChangePasswordOrLookUpUser() {
        doThrow(new BusinessException(401,"invalid")).when(email).verifyCode("mail@example.com","123456","RESET");
        assertThrows(BusinessException.class,()->service.resetPassword("mail@example.com","123456","new-password"));
        verifyNoInteractions(users,jdbc,merger);
    }
    @Test void resetNeverAutoRegistersMissingEmail() {
        assertThrows(BusinessException.class,()->service.resetPassword("missing@example.com","123456","new-password"));
        verify(users,never()).insert(any(AppUser.class)); verifyNoInteractions(merger);
        verify(jdbc,never()).update(anyString(),any(Object[].class));
    }
    @Test void successfulResetHashesPasswordAndExpiresAllUserSessions() {
        when(users.findByLoginEmail(anyString())).thenReturn(user(9,"mail@example.com"));
        service.resetPassword("mail@example.com","123456","new-password");
        verify(email).verifyCode("mail@example.com","123456","RESET");
        var hash=org.mockito.ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(eq("UPDATE app_user SET password_hash=? WHERE id=?"),hash.capture(),eq(9L));
        assertTrue(encoder.matches("new-password",hash.getValue())); verify(merger).expireSessions(9);
    }
    @Test void githubConflictCannotMergeIfOwnerChangedSinceAuthorization() {
        when(users.selectById(1L)).thenReturn(user(1,"mail@example.com"));
        when(jdbc.queryForList(contains("SELECT user_id FROM user_login_identity"),eq(Long.class),eq(123L))).thenReturn(List.of(3L));
        assertThrows(BusinessException.class,()->service.bindGithub(1,new GithubProfile(123L,"github-user",null),2L));
        verifyNoInteractions(merger); verify(jdbc,never()).update(anyString(),any(Object[].class));
    }
    @Test void emailConflictRetainsEmailAccountAndMovesGithubAccountIntoIt() {
        when(users.selectById(1L)).thenReturn(user(1,null));
        when(users.findByLoginEmail(anyString())).thenReturn(user(2,"mail@example.com"));
        var result=service.bindEmail(1,"mail@example.com",2L,null);
        verify(merger).merge(1,2); assertEquals(2L,result.userId()); assertEquals("user2",result.username());
    }
    @Test void twoDifferentBoundMailboxesAreNotSilentlyDiscarded() {
        when(users.selectById(1L)).thenReturn(user(1,"old@example.com"));
        when(users.findByLoginEmail(anyString())).thenReturn(user(2,"new@example.com"));
        assertThrows(BusinessException.class,()->service.bindEmail(1,"new@example.com",2L,"old@example.com")); verifyNoInteractions(merger);
    }
}
