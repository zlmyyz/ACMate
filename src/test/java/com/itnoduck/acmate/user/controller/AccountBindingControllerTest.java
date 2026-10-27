package com.itnoduck.acmate.user.controller;

import com.itnoduck.acmate.config.SecurityConfig;
import com.itnoduck.acmate.common.exception.GlobalExceptionHandler;
import com.itnoduck.acmate.security.AuthenticatedUser;
import com.itnoduck.acmate.security.DatabaseUserDetailsService;
import com.itnoduck.acmate.user.service.*;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.dto.GithubProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountBindingController.class)
@Import({SecurityConfig.class,GlobalExceptionHandler.class,com.itnoduck.acmate.security.ClientIpResolver.class})
class AccountBindingControllerTest {
    @Test void changingPasswordRequiresAuthenticationAndCsrf() throws Exception {
        String body="{\"currentPassword\":\"old-password\",\"newPassword\":\"new-password\",\"confirmPassword\":\"new-password\"}";
        mvc.perform(post("/api/users/me/password").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/users/me/password").with(user(principal)).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        verifyNoInteractions(accounts);
    }
    @Test void changingPasswordRejectsMismatchedConfirmation() throws Exception {
        mvc.perform(post("/api/users/me/password").with(user(principal)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"old-password\",\"newPassword\":\"new-password\",\"confirmPassword\":\"different-password\"}"))
                .andExpect(status().isBadRequest());verifyNoInteractions(accounts);
    }
    @Test void changingPasswordRejectsMissingOldPasswordAndShortNewPassword() throws Exception {
        mvc.perform(post("/api/users/me/password").with(user(principal)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"\",\"newPassword\":\"short\",\"confirmPassword\":\"short\"}"))
                .andExpect(status().isBadRequest());verifyNoInteractions(accounts);
    }
    @Test void changingPasswordOnlyUsesTheAuthenticatedAccountAndLogsOutCurrentSession() throws Exception {
        var session=new MockHttpSession();
        mvc.perform(post("/api/users/me/password").with(user(principal)).with(csrf()).session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":999,\"currentPassword\":\"old-password\",\"newPassword\":\"new-password\",\"confirmPassword\":\"new-password\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("密码已修改，请使用新密码重新登录"));
        verify(accounts).changePassword(1,"old-password","new-password");
        org.junit.jupiter.api.Assertions.assertTrue(session.isInvalid());
    }
    @Test void unlinkRequiresAuthenticationAndCsrf() throws Exception {
        mvc.perform(post("/api/users/me/github/unlink").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"confirm\":true}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/users/me/github/unlink").with(user(principal)).contentType(MediaType.APPLICATION_JSON).content("{\"confirm\":true}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(accounts);
    }
    @Test void unlinkRequiresExplicitConfirmation() throws Exception {
        mvc.perform(post("/api/users/me/github/unlink").with(user(principal)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"confirm\":false}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(accounts);
    }
    @Test void unlinkUsesAuthenticatedIdAndClearsPendingOAuthState() throws Exception {
        when(accounts.unlinkGithub(1)).thenReturn(new AccountSecurityService.BindingResult("user1",1L));
        var session=new MockHttpSession();session.setAttribute("githubLoginState","old");session.setAttribute("pendingGithubBinding","old");
        mvc.perform(post("/api/users/me/github/unlink").with(user(principal)).with(csrf()).session(session)
                .contentType(MediaType.APPLICATION_JSON).content("{\"confirm\":true,\"userId\":999}"))
                .andExpect(status().isOk());
        verify(accounts).unlinkGithub(1);verify(email).signIn(eq("user1"),any(),any());
        org.junit.jupiter.api.Assertions.assertNull(session.getAttribute("githubLoginState"));
        org.junit.jupiter.api.Assertions.assertNull(session.getAttribute("pendingGithubBinding"));
    }
    @Autowired MockMvc mvc;
    @MockitoBean AlternativeLoginService email;
    @MockitoBean GithubLoginService github;
    @MockitoBean AccountSecurityService accounts;
    @MockitoBean AlternativeLoginController oauth;
    @MockitoBean DatabaseUserDetailsService details;
    final AuthenticatedUser principal=new AuthenticatedUser(1L,"user1","hash","User",null,null,null,false,true,List.of());
    @Test void privateBindingStatusRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/users/me/bindings")).andExpect(status().isUnauthorized());
        verifyNoInteractions(accounts);
    }
    @Test void resetAndBindingWritesRequireCsrf() throws Exception {
        for(String path:List.of("/api/auth/password/reset","/api/auth/password/code","/api/auth/github/link/confirm","/api/users/me/email/confirm"))
            mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        verifyNoInteractions(email,accounts);
    }
    @Test void resetIsAccessibleToGuestsWithValidCsrfAndInputs() throws Exception {
        mvc.perform(post("/api/auth/password/reset").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"mail@example.com\",\"code\":\"123456\",\"password\":\"new-password\"}"))
                .andExpect(status().isOk());
        verify(accounts).resetPassword("mail@example.com","123456","new-password");
    }
    @Test void resetRejectsShortPasswordBeforeCallingService() throws Exception {
        mvc.perform(post("/api/auth/password/reset").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"mail@example.com\",\"code\":\"123456\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest()); verifyNoInteractions(accounts);
    }
    @Test void githubConfirmCannotUseAnotherUsersPendingProof() throws Exception {
        var session=new MockHttpSession();
        session.setAttribute("pendingGithubBinding",new AlternativeLoginController.PendingGithub(new GithubProfile(123L,"github-user",null),2L,null,null,System.currentTimeMillis()+60_000));
        mvc.perform(post("/api/users/me/github/confirm").with(user(principal)).with(csrf()).session(session)
                .contentType(MediaType.APPLICATION_JSON).content("{\"confirm\":true}"))
                .andExpect(status().isForbidden()); verifyNoInteractions(accounts);
    }
    @Test void githubConfirmRequiresExplicitConsent() throws Exception {
        mvc.perform(post("/api/users/me/github/confirm").with(user(principal)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"confirm\":false}"))
                .andExpect(status().isBadRequest()); verifyNoInteractions(accounts);
    }
    @Test void expiredPendingEmailCannotBind() throws Exception {
        var session=new MockHttpSession();
        session.setAttribute("pendingEmailBinding",new AccountBindingController.PendingEmail(1,null,"mail@example.com",2L,1));
        mvc.perform(post("/api/users/me/email/confirm").with(user(principal)).with(csrf()).session(session)
                .contentType(MediaType.APPLICATION_JSON).content("{\"confirm\":true}"))
                .andExpect(status().isGone()); verifyNoInteractions(accounts);
    }
    @Test void changingMailboxRequiresOldMailboxProofBeforeNewProof() throws Exception {
        var current=new AppUser(); current.setId(1L);current.setEmail("old@example.com");
        when(accounts.active(1)).thenReturn(current);
        doThrow(new com.itnoduck.acmate.common.exception.BusinessException(401,"invalid")).when(email).verifyCode("old@example.com",null,"CURRENT:1");
        mvc.perform(post("/api/users/me/email/prepare").with(user(principal)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"new@example.com\",\"code\":\"123456\"}"))
                .andExpect(status().isUnauthorized());
        verify(email,never()).verifyCode("new@example.com","123456","BIND:1");
        verify(accounts,never()).bindEmail(anyLong(),anyString(),any(),any());
    }
}
