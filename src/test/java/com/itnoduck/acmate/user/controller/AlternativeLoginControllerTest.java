package com.itnoduck.acmate.user.controller;

import com.itnoduck.acmate.common.exception.GlobalExceptionHandler;
import com.itnoduck.acmate.config.SecurityConfig;
import com.itnoduck.acmate.security.DatabaseUserDetailsService;
import com.itnoduck.acmate.user.service.AlternativeLoginService;
import com.itnoduck.acmate.user.service.GithubLoginService;
import com.itnoduck.acmate.user.dto.GithubProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AlternativeLoginController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class,com.itnoduck.acmate.security.ClientIpResolver.class})
class AlternativeLoginControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AlternativeLoginService login;
    @MockitoBean GithubLoginService github;
    @MockitoBean com.itnoduck.acmate.user.service.AccountSecurityService accounts;
    @MockitoBean DatabaseUserDetailsService details;

    @Test void methodsArePublicAndDoNotExposeSecrets() throws Exception {
        mvc.perform(get("/api/auth/methods")).andExpect(status().isOk())
                .andExpect(content().json("{\"email\":false,\"github\":false}"))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test void newPostEndpointsRequireCsrf() throws Exception {
        for (String path : new String[]{"/email/code", "/email/login", "/github/authorize"})
            mvc.perform(post("/api/auth" + path).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        verifyNoInteractions(login, github);
    }

    @Test void invalidEmailAndCodeAreRejected() throws Exception {
        mvc.perform(post("/api/auth/email/code").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"invalid\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/email/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\",\"code\":\"12345x\"}")).andExpect(status().isBadRequest());
        verifyNoInteractions(login);
    }

    @Test void codeRequestIsPublicWithCsrf() throws Exception {
        mvc.perform(post("/api/auth/email/code").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\"}")).andExpect(status().isOk());
        verify(login).sendCode(eq("user@example.com"), anyString());
    }

    @Test void forgedGithubCallbackDoesNotContactProvider() throws Exception {
        mvc.perform(get("/api/auth/github/callback").param("state", "forged").param("code", "code"))
                .andExpect(status().isFound()).andExpect(redirectedUrl("/login?github=failed"))
                .andExpect(header().string("Location", "/login?github=failed"));
        verifyNoInteractions(login, github);
    }

    @Test void githubStateIsSessionBoundAndOneTime() throws Exception {
        when(github.authorizeUrl(anyString(), anyString())).thenAnswer(inv -> "https://github.com/login/oauth/authorize?state=" + inv.getArgument(0));
        var result = mvc.perform(post("/api/auth/github/authorize").with(csrf())).andExpect(status().isOk()).andReturn();
        var session = (org.springframework.mock.web.MockHttpSession) result.getRequest().getSession(false);
        String body = result.getResponse().getContentAsString();
        String state = body.substring(body.indexOf("state=") + 6, body.lastIndexOf('"'));
        GithubProfile profile = new GithubProfile(12345L, "github-user", "GitHub Nickname");
        when(github.exchangeProfile(eq("code"), anyString())).thenReturn(profile);
        when(github.resolveUsername(profile)).thenReturn("gh_user");
        mvc.perform(get("/api/auth/github/callback").session(session).param("state", state).param("code", "code"))
                .andExpect(redirectedUrl("/"))
                .andExpect(header().string("Location", "/"));
        mvc.perform(get("/api/auth/github/callback").session(session).param("state", state).param("code", "code"))
                .andExpect(redirectedUrl("/login?github=failed"));
        verify(github, times(1)).exchangeProfile(eq("code"), matches("[a-f0-9]{64}"));
        verify(login).signIn(eq("gh_user"), any(), any());
    }
}
