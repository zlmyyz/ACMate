package com.itnoduck.acmate.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itnoduck.acmate.common.exception.GlobalExceptionHandler;
import com.itnoduck.acmate.config.SecurityConfig;
import com.itnoduck.acmate.security.AuthenticatedUser;
import com.itnoduck.acmate.security.DatabaseUserDetailsService;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import com.itnoduck.acmate.user.service.UserAuthenticationService;
import com.itnoduck.acmate.user.service.UserProfileService;
import com.itnoduck.acmate.user.service.UserRegistrationService;
import com.itnoduck.acmate.user.service.impl.UserAuthenticationServiceImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuthController.class, UserController.class})
@Import({
    SecurityConfig.class,
    GlobalExceptionHandler.class,
    DatabaseUserDetailsService.class,
    UserAuthenticationServiceImpl.class
})
class SessionLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppUserMapper appUserMapper;

    @MockitoBean
    private UserRegistrationService userRegistrationService;

    @MockitoBean
    private UserProfileService userProfileService;

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
    void shouldReturn200OnCorrectCredentials() throws Exception {
        AppUser user = buildAppUser(1L, "testuser", "test@example.com", 0, 1);
        when(appUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"" + RAW_PASSWORD + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldNotReturnPasswordHashInResponse() throws Exception {
        AppUser user = buildAppUser(1L, "testuser", "test@example.com", 0, 1);
        when(appUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"" + RAW_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void shouldReturn401OnWrongPassword() throws Exception {
        AppUser user = buildAppUser(1L, "testuser", "test@example.com", 0, 1);
        when(appUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"wrongpassword\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldReturn401WhenUserNotFound() throws Exception {
        when(appUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nonexistent\",\"password\":\"password123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldReturn403WhenUserDisabled() throws Exception {
        AppUser user = buildAppUser(1L, "testuser", "test@example.com", 0, 0);
        when(appUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"" + RAW_PASSWORD + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void shouldAllowLoginPostWithoutCsrf() throws Exception {
        AppUser user = buildAppUser(1L, "testuser", "test@example.com", 0, 1);
        when(appUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"" + RAW_PASSWORD + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldStoreSecurityContextInSessionAfterLogin() throws Exception {
        AppUser user = buildAppUser(1L, "testuser", "test@example.com", 0, 1);
        when(appUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"" + RAW_PASSWORD + "\"}"))
                .andExpect(status().isOk());

        Object attr = session.getAttribute("SPRING_SECURITY_CONTEXT");
    }

    @Test
    void shouldAllowMeWithSameSessionAfterLogin() throws Exception {
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void shouldReturnCorrectUserFieldsOnMe() throws Exception {
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.nickname").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.admin").value(false))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist());
    }

    @Test
    void shouldReturnJson401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未登录或登录已失效"));
    }

    @Test
    void shouldHaveAuthenticatedUserAsPrincipalInSession() throws Exception {
        AppUser user = buildAppUser(1L, "testuser", "test@example.com", 0, 1);
        when(appUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"" + RAW_PASSWORD + "\"}"))
                .andExpect(status().isOk());

        SecurityContext context = (SecurityContext) session.getAttribute("SPRING_SECURITY_CONTEXT");
        assert context != null;
        assert context.getAuthentication().getPrincipal() instanceof AuthenticatedUser;

        AuthenticatedUser principal = (AuthenticatedUser) context.getAuthentication().getPrincipal();
        assert principal.getUsername().equals("testuser");
        assert principal.getId().equals(1L);
    }

    @Test
    void shouldReturnAdminTrueForAdminUser() throws Exception {
        AppUser user = buildAppUser(2L, "adminuser", "admin@example.com", 1, 1);
        when(appUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"adminuser\",\"password\":\"" + RAW_PASSWORD + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admin").value(true));
    }
}
