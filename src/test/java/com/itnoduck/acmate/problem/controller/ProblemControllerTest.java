package com.itnoduck.acmate.problem.controller;

import com.itnoduck.acmate.common.dto.PageResponse;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.common.exception.GlobalExceptionHandler;
import com.itnoduck.acmate.config.MybatisPlusConfig;
import com.itnoduck.acmate.config.SecurityConfig;
import com.itnoduck.acmate.problem.dto.ProblemDetailResponse;
import com.itnoduck.acmate.problem.dto.ProblemSummaryResponse;
import com.itnoduck.acmate.problem.service.ProblemCommandService;
import com.itnoduck.acmate.problem.service.ProblemQueryService;
import com.itnoduck.acmate.security.AuthenticatedUser;
import com.itnoduck.acmate.security.DatabaseUserDetailsService;
import com.itnoduck.acmate.user.service.UserAuthenticationService;
import com.itnoduck.acmate.user.service.UserRegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProblemController.class)
@Import({
    SecurityConfig.class,
    GlobalExceptionHandler.class,
    MybatisPlusConfig.class
})
class ProblemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProblemQueryService problemQueryService;

    @MockitoBean
    private ProblemCommandService problemCommandService;

    @MockitoBean
    private DatabaseUserDetailsService databaseUserDetailsService;

    @MockitoBean
    private UserRegistrationService userRegistrationService;

    @MockitoBean
    private UserAuthenticationService userAuthenticationService;

    @Test
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/problems"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldReturnListForAuthenticatedUser() throws Exception {
        PageResponse<ProblemSummaryResponse> page = new PageResponse<>(1, 20, 1, 1,
                List.of(new ProblemSummaryResponse(1L, "CUSTOM", "EXT-1", "Two Sum",
                        "https://example.com", "800", "dp,greedy", 1L,
                        LocalDateTime.of(2026, 7, 20, 12, 0))));
        when(problemQueryService.listProblems(any())).thenReturn(page);

        mockMvc.perform(get("/api/problems")
                        .with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.records[0].title").value("Two Sum"))
                .andExpect(jsonPath("$.records[0].platform").value("CUSTOM"));
    }

    @Test
    void shouldReturnDetailForAuthenticatedUser() throws Exception {
        ProblemDetailResponse detail = new ProblemDetailResponse(1L, "CUSTOM", "EXT-1",
                "Two Sum", "https://example.com", "800", "dp,greedy",
                "## Content", 1L,
                LocalDateTime.of(2026, 7, 20, 12, 0),
                LocalDateTime.of(2026, 7, 21, 12, 0));
        when(problemQueryService.getProblem(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/problems/1")
                        .with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Two Sum"))
                .andExpect(jsonPath("$.contentMd").value("## Content"))
                .andExpect(jsonPath("$.status").doesNotExist());
    }

    @Test
    void shouldReturn404WhenProblemNotFound() throws Exception {
        when(problemQueryService.getProblem(anyLong()))
                .thenThrow(new BusinessException(404, "题目不存在"));

        mockMvc.perform(get("/api/problems/999")
                        .with(user("testuser")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("题目不存在"));
    }

    @Test
    void shouldReturn400WhenPageIsZero() throws Exception {
        mockMvc.perform(get("/api/problems?page=0")
                        .with(user("testuser")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldReturn400WhenSizeExceeds100() throws Exception {
        mockMvc.perform(get("/api/problems?size=101")
                        .with(user("testuser")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldReturn400WhenPlatformInvalid() throws Exception {
        mockMvc.perform(get("/api/problems?platform=INVALID")
                        .with(user("testuser")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    // --- POST /api/problems ---

    private AuthenticatedUser buildAdminUser() {
        return new AuthenticatedUser(1L, "admin", "hash", "Admin",
                null, null, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private AuthenticatedUser buildNormalUser() {
        return new AuthenticatedUser(2L, "user", "hash", "User",
                null, null, false, true,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void shouldReturn401WhenUnauthenticatedPostWithCsrf() throws Exception {
        mockMvc.perform(post("/api/problems")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platform\":\"CUSTOM\",\"title\":\"Test\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldReturn403WhenUnauthenticatedPostWithoutCsrf() throws Exception {
        // CSRF Filter 先于认证检查拦截
        mockMvc.perform(post("/api/problems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platform\":\"CUSTOM\",\"title\":\"Test\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void shouldReturn403WhenNormalUserPostsWithCsrf() throws Exception {
        mockMvc.perform(post("/api/problems")
                        .with(user(buildNormalUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platform\":\"CUSTOM\",\"title\":\"Test\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void shouldReturn403WhenAdminPostsWithoutCsrf() throws Exception {
        mockMvc.perform(post("/api/problems")
                        .with(user(buildAdminUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platform\":\"CUSTOM\",\"title\":\"Test\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void shouldReturn201WhenAdminPostsWithCsrf() throws Exception {
        ProblemDetailResponse detail = new ProblemDetailResponse(1L, "CUSTOM", null,
                "Test", null, "800", "dp", "## Content", 1L,
                LocalDateTime.of(2026, 7, 25, 12, 0),
                LocalDateTime.of(2026, 7, 25, 12, 0));
        when(problemCommandService.createProblem(any(), eq(1L))).thenReturn(detail);

        mockMvc.perform(post("/api/problems")
                        .with(user(buildAdminUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platform\":\"CUSTOM\",\"title\":\"Test\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.platform").value("CUSTOM"))
                .andExpect(jsonPath("$.title").value("Test"))
                .andExpect(jsonPath("$.creatorUserId").value(1));
    }

    @Test
    void shouldNotReturnStatusInCreateResponse() throws Exception {
        ProblemDetailResponse detail = new ProblemDetailResponse(1L, "CUSTOM", null,
                "Test", null, null, null, "## Content", 1L,
                LocalDateTime.of(2026, 7, 25, 12, 0),
                LocalDateTime.of(2026, 7, 25, 12, 0));
        when(problemCommandService.createProblem(any(), eq(1L))).thenReturn(detail);

        mockMvc.perform(post("/api/problems")
                        .with(user(buildAdminUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platform\":\"CUSTOM\",\"title\":\"Test\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").doesNotExist());
    }

    @Test
    void shouldPassAuthenticatedUserIdToService() throws Exception {
        ProblemDetailResponse detail = new ProblemDetailResponse(1L, "CUSTOM", null,
                "Test", null, null, null, null, 1L,
                LocalDateTime.of(2026, 7, 25, 12, 0),
                LocalDateTime.of(2026, 7, 25, 12, 0));
        when(problemCommandService.createProblem(any(), eq(1L))).thenReturn(detail);

        mockMvc.perform(post("/api/problems")
                        .with(user(buildAdminUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platform\":\"CUSTOM\",\"title\":\"Test\"}"))
                .andExpect(status().isCreated());

        verify(problemCommandService).createProblem(any(), eq(1L));
    }

    @Test
    void shouldIgnoreCreatorUserIdAndStatusInRequestBody() throws Exception {
        ProblemDetailResponse detail = new ProblemDetailResponse(1L, "CUSTOM", null,
                "Test", null, null, null, null, 1L,
                LocalDateTime.of(2026, 7, 25, 12, 0),
                LocalDateTime.of(2026, 7, 25, 12, 0));
        when(problemCommandService.createProblem(any(), eq(1L))).thenReturn(detail);

        // 请求体尝试注入 creatorUserId=999 和 status=0，但应被忽略
        mockMvc.perform(post("/api/problems")
                        .with(user(buildAdminUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platform\":\"CUSTOM\",\"title\":\"Test\",\"creatorUserId\":999,\"status\":0}"))
                .andExpect(status().isCreated());

        verify(problemCommandService).createProblem(any(), eq(1L));
    }

    @Test
    void shouldReturn400WhenValidationFails() throws Exception {
        mockMvc.perform(post("/api/problems")
                        .with(user(buildAdminUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platform\":\"INVALID\",\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldStillAllowGetForNormalUser() throws Exception {
        // 验证 POST ADMIN 规则不影响 GET 查询
        PageResponse<ProblemSummaryResponse> page = new PageResponse<>(1, 20, 0, 0, List.of());
        when(problemQueryService.listProblems(any())).thenReturn(page);

        mockMvc.perform(get("/api/problems")
                        .with(user(buildNormalUser())))
                .andExpect(status().isOk());
    }
}
