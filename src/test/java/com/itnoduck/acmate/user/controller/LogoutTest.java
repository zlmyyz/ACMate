package com.itnoduck.acmate.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itnoduck.acmate.common.exception.GlobalExceptionHandler;
import com.itnoduck.acmate.config.SecurityConfig;
import com.itnoduck.acmate.security.DatabaseUserDetailsService;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import com.itnoduck.acmate.user.service.UserRegistrationService;
import com.itnoduck.acmate.user.service.impl.UserAuthenticationServiceImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AuthController.class, UserController.class})
@Import({
    SecurityConfig.class,
    GlobalExceptionHandler.class,
    DatabaseUserDetailsService.class,
    UserAuthenticationServiceImpl.class
})
class LogoutTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppUserMapper appUserMapper;

    @MockitoBean
    private UserRegistrationService userRegistrationService;

    private static final String RAW_PASSWORD = "password123";
    private static String PASSWORD_HASH;

    @BeforeAll
    static void setUpHash() {
        PASSWORD_HASH = new BCryptPasswordEncoder().encode(RAW_PASSWORD);
    }

    private AppUser buildAppUser(Long id, String username, String email, int isAdmin, int status) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash(PASSWORD_HASH);
        user.setNickname("Test User");
        user.setEmail(email);
        user.setAvatarUrl("https://example.com/avatar.png");
        user.setIsAdmin(isAdmin);
        user.setStatus(status);
        return user;
    }

    @Test
    void shouldReturn403WhenLogoutWithoutCsrf() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .with(user("testuser")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权执行该操作"));
    }

    @Test
    void shouldReturn204WhenLogoutWithCsrf() throws Exception {
        AppUser user = buildAppUser(1L, "testuser", "test@example.com", 0, 1);
        when(appUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"" + RAW_PASSWORD + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldInvalidateSessionAfterLogout() throws Exception {
        AppUser user = buildAppUser(1L, "testuser", "test@example.com", 0, 1);
        when(appUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"" + RAW_PASSWORD + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assert session.isInvalid();
    }

    @Test
    void shouldReturn401OnMeAfterLogout() throws Exception {
        AppUser user = buildAppUser(1L, "testuser", "test@example.com", 0, 1);
        when(appUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"" + RAW_PASSWORD + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me")
                        .session(session))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/me")
                        .session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldClearSecurityContextAfterLogout() throws Exception {
        AppUser user = buildAppUser(1L, "testuser", "test@example.com", 0, 1);
        when(appUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"" + RAW_PASSWORD + "\"}"))
                .andExpect(status().isOk());

        assert session.getAttribute("SPRING_SECURITY_CONTEXT") != null;

        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assert session.isInvalid();
    }

    @Test
    void shouldReturn401WhenUnauthenticatedLogout() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldHaveEmptyResponseBodyOnLogout() throws Exception {
        AppUser user = buildAppUser(1L, "testuser", "test@example.com", 0, 1);
        when(appUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"" + RAW_PASSWORD + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void shouldStillAllowRegisterAndLoginWithoutCsrf() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ab\",\"password\":\"123\",\"nickname\":\"a\"}"))
                .andExpect(status().isBadRequest());

        AppUser user = buildAppUser(1L, "testuser", "test@example.com", 0, 1);
        when(appUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"" + RAW_PASSWORD + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn403ForOtherPostWithoutCsrf() throws Exception {
        mockMvc.perform(post("/api/security-test")
                        .with(user("testuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void shouldReturnCsrfTokenOnGet() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.headerName").exists())
                .andExpect(jsonPath("$.parameterName").exists());
    }

    @Test
    void shouldReturnNonEmptyToken() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())));
    }

    @Test
    void shouldNotContainSessionIdInCsrfResponse() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").doesNotExist())
                .andExpect(jsonPath("$.cookie").doesNotExist());
    }

    @Test
    void shouldLogoutWithRealCsrfToken() throws Exception {
        AppUser user = buildAppUser(1L, "testuser", "test@example.com", 0, 1);
        when(appUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"" + RAW_PASSWORD + "\"}"))
                .andExpect(status().isOk());

        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = csrfResult.getResponse().getContentAsString();
        String token = JsonPath.read(responseJson, "$.token");
        String headerName = JsonPath.read(responseJson, "$.headerName");

        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .header(headerName, token))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn403WithWrongCsrfToken() throws Exception {
        AppUser user = buildAppUser(1L, "testuser", "test@example.com", 0, 1);
        when(appUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"" + RAW_PASSWORD + "\"}"))
                .andExpect(status().isOk());

        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        String headerName = JsonPath.read(csrfResult.getResponse().getContentAsString(), "$.headerName");

        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .header(headerName, "wrong-token-value"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void shouldLogoutAndThenMeReturns401WithRealCsrfFlow() throws Exception {
        AppUser user = buildAppUser(1L, "testuser", "test@example.com", 0, 1);
        when(appUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"" + RAW_PASSWORD + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me")
                        .session(session))
                .andExpect(status().isOk());

        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = csrfResult.getResponse().getContentAsString();
        String token = JsonPath.read(responseJson, "$.token");
        String headerName = JsonPath.read(responseJson, "$.headerName");

        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .header(headerName, token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/me")
                        .session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }
}
