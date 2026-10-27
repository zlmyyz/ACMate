package com.itnoduck.acmate.user.service;

import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.config.LoginProperties;
import com.itnoduck.acmate.security.DatabaseUserDetailsService;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import com.itnoduck.acmate.user.entity.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AlternativeLoginServiceTest {
    final LoginProperties properties = new LoginProperties();
    @SuppressWarnings("unchecked") final ObjectProvider<JavaMailSender> mail = mock(ObjectProvider.class);
    final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked") final HashOperations<String,Object,Object> hashes = mock(HashOperations.class);
    final AppUserMapper users = mock(AppUserMapper.class);
    final DatabaseUserDetailsService details = mock(DatabaseUserDetailsService.class);
    final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    final AlternativeLoginService service = new AlternativeLoginService(properties, mail, redis, users, encoder, details,
            mock(SecurityContextRepository.class), mock(SessionAuthenticationStrategy.class));
    void enable() {
        properties.setEmailEnabled(true); properties.setMailFrom("site@example.com");
        when(mail.getIfAvailable()).thenReturn(mock(JavaMailSender.class));
        when(redis.opsForHash()).thenReturn(hashes);
    }
    @SuppressWarnings("unchecked")
    void scripts(Long... outcomes) {
        when(redis.execute(any(DefaultRedisScript.class), anyList(), any(Object[].class))).thenReturn(outcomes[0], java.util.Arrays.copyOfRange(outcomes,1,outcomes.length));
    }
    @Test void missingConfigurationFailsBeforeTouchingRedis() {
        assertEquals(503,assertThrows(BusinessException.class,()->service.sendCode("user@example.com","127.0.0.1")).getCode());
        verifyNoInteractions(redis,users);
    }
    @Test void missingOrMalformedCodesFailWithoutRedisAccess() {
        assertEquals(401,assertThrows(BusinessException.class,()->service.verifyCode("user@example.com",null,"RESET")).getCode());
        assertThrows(BusinessException.class,()->service.verifyCode("user@example.com","12345x","RESET"));
        verifyNoInteractions(redis);
    }
    @Test void expiredCodeDoesNotReadHashOrCreateAccount() {
        enable(); scripts(0L);
        assertThrows(BusinessException.class,()->service.emailLogin("user@example.com","123456",null,null));
        verifyNoInteractions(hashes,users,details);
    }
    @Test void wrongCodeCannotBeConsumedOrLogin() {
        enable(); scripts(1L);
        when(hashes.get(anyString(),eq("codeHash"))).thenReturn(encoder.encode("654321"));
        assertThrows(BusinessException.class,()->service.emailLogin("user@example.com","123456",null,null));
        verify(redis,times(1)).execute(any(DefaultRedisScript.class),anyList(),any(Object[].class));
        verifyNoInteractions(users,details);
    }
    @Test void concurrentConsumptionCannotReuseCode() {
        enable(); scripts(1L,0L);
        when(hashes.get(anyString(),eq("codeHash"))).thenReturn(encoder.encode("123456"));
        assertThrows(BusinessException.class,()->service.verifyCode("user@example.com","123456","RESET"));
        verifyNoInteractions(users,details);
    }
    @Test void validCodeIsAtomicallyConsumedInItsPurposeNamespace() {
        enable(); scripts(1L,1L);
        when(hashes.get(anyString(),eq("codeHash"))).thenReturn(encoder.encode("123456"));
        service.verifyCode(" USER@example.com ","123456","BIND:12");
        verify(hashes).get(startsWith("acmate:login:email-code:BIND:12:"),eq("codeHash"));
        verify(redis,times(2)).execute(any(DefaultRedisScript.class),argThat(keys->keys.size()==1&&((String)keys.get(0)).contains(":BIND:12:")),any(Object[].class));
        verifyNoInteractions(users,details);
    }
    @Test void resetCodeDoesNotConsultLoginChallenge() {
        enable(); scripts(0L);
        assertThrows(BusinessException.class,()->service.verifyCode("user@example.com","123456","RESET"));
        verify(redis).execute(any(DefaultRedisScript.class),argThat(keys->((String)keys.get(0)).startsWith("acmate:login:email-code:RESET:")),any(Object[].class));
    }
    @Test void redisFailureReturnsServiceUnavailable() {
        enable();
        when(redis.execute(any(DefaultRedisScript.class),anyList(),any(Object[].class))).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("offline"));
        assertEquals(503,assertThrows(BusinessException.class,()->service.verifyCode("user@example.com","123456","REGISTER")).getCode());
        verifyNoInteractions(users,details);
    }
    @Test void newEmailAccountIsCreatedOnlyAfterVerification() {
        enable(); scripts(1L,1L);
        when(hashes.get(anyString(),eq("codeHash"))).thenReturn(encoder.encode("123456"));
        when(users.insert(any(AppUser.class))).thenAnswer(inv->{ ((AppUser)inv.getArgument(0)).setId(12L); return 1; });
        // Session creation is deliberately stopped after account creation to inspect the verified identity.
        when(details.loadUserByUsername(anyString())).thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("test-stop"));
        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,()->service.emailLogin("New@Example.com","123456",null,null));
        verify(users).insert(argThat((AppUser user)->"new@example.com".equals(user.getEmail())&&user.getStatus()==1&&user.getIsAdmin()==0));
    }
}
