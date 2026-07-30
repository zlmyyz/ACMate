package com.itnoduck.acmate.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itnoduck.acmate.common.exception.GlobalExceptionHandler;
import com.itnoduck.acmate.config.SecurityConfig;
import com.itnoduck.acmate.security.DatabaseUserDetailsService;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import com.itnoduck.acmate.problem.service.ProblemQueryService;
import com.itnoduck.acmate.training.service.TrainingPlanService;
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
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
class DisabledUserSessionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionRegistry sessionRegistry;

    @MockitoBean
    private AppUserMapper appUserMapper;

    @MockitoBean
    private UserRegistrationService userRegistrationService;

    @MockitoBean
    private UserProfileService userProfileService;

    @MockitoBean
    private ProblemQueryService problemQueryService;

    @MockitoBean
    private TrainingPlanService trainingPlanService;

    private static final String RAW_PASSWORD = "password123";
    private static String PASSWORD_HASH;

    @BeforeAll
    static void setUpHash() {
        PASSWORD_HASH = new BCryptPasswordEncoder().encode(RAW_PASSWORD);
    }

    private AppUser buildAppUser(Long id, String username, int status) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash(PASSWORD_HASH);
        user.setNickname("Test");
        user.setEmail(username + "@test.com");
        user.setIsAdmin(0);
        user.setStatus(status);
        return user;
    }

    private MockHttpSession login(String username) throws Exception {
        AppUser user = buildAppUser(1L, username, 1);
        when(appUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/auth/login")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + RAW_PASSWORD + "\"}"))
                .andExpect(status().isOk());
        return session;
    }

    @Test
    void shouldAccessMeAfterLogin() throws Exception {
        MockHttpSession session = login("testuser");
        mockMvc.perform(get("/api/users/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void shouldRejectWhenSessionExpired() throws Exception {
        MockHttpSession session = login("testuser");
        mockMvc.perform(get("/api/users/me").session(session)).andExpect(status().isOk());

        for (Object principal : sessionRegistry.getAllPrincipals()) {
            sessionRegistry.getAllSessions(principal, false).forEach(s -> s.expireNow());
        }

        mockMvc.perform(get("/api/users/me").session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void shouldRejectLoginWhenDisabled() throws Exception {
        AppUser disabledUser = buildAppUser(1L, "disabled", 0);
        when(appUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(disabledUser);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"disabled\",\"password\":\"" + RAW_PASSWORD + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void shouldAllowLoginAfterReenabled() throws Exception {
        AppUser disabledUser = buildAppUser(1L, "testuser", 0);
        when(appUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(disabledUser);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"password\":\"" + RAW_PASSWORD + "\"}"))
                .andExpect(status().isForbidden());

        AppUser enabledUser = buildAppUser(1L, "testuser", 1);
        when(appUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(enabledUser);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"password\":\"" + RAW_PASSWORD + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldNotAffectOtherUsersSessions() throws Exception {
        MockHttpSession userASession = login("userA");

        AppUser userB = buildAppUser(2L, "userB", 1);
        when(appUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(userB);
        MockHttpSession userBSession = new MockHttpSession();
        mockMvc.perform(post("/api/auth/login")
                .session(userBSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"userB\",\"password\":\"" + RAW_PASSWORD + "\"}"))
                .andExpect(status().isOk());

        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails u
                    && u.getUsername().equals("userA")) {
                sessionRegistry.getAllSessions(principal, false).forEach(s -> s.expireNow());
            }
        }

        mockMvc.perform(get("/api/users/me").session(userASession))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users/me").session(userBSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("userB"));
    }
}
