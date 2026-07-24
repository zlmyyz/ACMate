package com.itnoduck.acmate.user.controller;

import com.itnoduck.acmate.common.exception.GlobalExceptionHandler;
import com.itnoduck.acmate.config.SecurityConfig;
import com.itnoduck.acmate.user.dto.RegisterRequest;
import com.itnoduck.acmate.user.dto.RegisterResponse;
import com.itnoduck.acmate.user.service.UserRegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({
    SecurityConfig.class,
    GlobalExceptionHandler.class
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRegistrationService userRegistrationService;

    @Test
    void shouldReturn201OnValidRegister() throws Exception {
        when(userRegistrationService.register(any(RegisterRequest.class)))
                .thenReturn(new RegisterResponse(1L, "testuser", "Test User", "test@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"password123\",\"nickname\":\"Test User\",\"email\":\"test@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void shouldReturn400WhenNicknameTooShortAfterTrim() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"password123\",\"nickname\":\" a \",\"email\":\"test@example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("nickname"));
    }

    @Test
    void shouldAcceptAndNormalizeTrimmedUsername() throws Exception {
        when(userRegistrationService.register(any(RegisterRequest.class)))
                .thenReturn(new RegisterResponse(1L, "testuser", "Test User", "test@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\" TestUser \",\"password\":\"password123\",\"nickname\":\"Test User\",\"email\":\"test@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void shouldNormalizeEmailTrimAndCase() throws Exception {
        when(userRegistrationService.register(any(RegisterRequest.class)))
                .thenReturn(new RegisterResponse(1L, "testuser", "Test User", "test@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"password123\",\"nickname\":\"Test User\",\"email\":\"  TEST@EXAMPLE.COM  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void shouldNormalizeBlankEmailToNull() throws Exception {
        when(userRegistrationService.register(any(RegisterRequest.class)))
                .thenReturn(new RegisterResponse(1L, "testuser", "Test User", null));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"password123\",\"nickname\":\"Test User\",\"email\":\"   \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void shouldReturn400WhenUsernameTooShortAfterTrim() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\" ab \",\"password\":\"password123\",\"nickname\":\"Test User\",\"email\":\"test@example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("username"));
    }

    @Test
    void shouldIgnoreIsAdminAndStatusInRequestBody() throws Exception {
        when(userRegistrationService.register(any(RegisterRequest.class)))
                .thenReturn(new RegisterResponse(1L, "testuser", "Test User", "test@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"password123\",\"nickname\":\"Test User\",\"email\":\"test@example.com\",\"isAdmin\":1,\"status\":0}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void shouldReturn400WhenPasswordTooShort() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"123\",\"nickname\":\"Test User\",\"email\":\"test@example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"));
    }

    @Test
    void shouldAllowRegisterPostWithoutCsrf() throws Exception {
        when(userRegistrationService.register(any(RegisterRequest.class)))
                .thenReturn(new RegisterResponse(1L, "testuser", "Test User", "test@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"password123\",\"nickname\":\"Test User\",\"email\":\"test@example.com\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldRequireCsrfForPutRegister() throws Exception {
        mockMvc.perform(put("/api/auth/register")
                        .with(user("testuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"password123\",\"nickname\":\"Test User\",\"email\":\"test@example.com\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRequireCsrfForOtherPost() throws Exception {
        mockMvc.perform(post("/api/security-test")
                        .with(user("testuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowOtherPostWithCsrf() throws Exception {
        mockMvc.perform(post("/api/security-test")
                        .with(user("testuser"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }
}
