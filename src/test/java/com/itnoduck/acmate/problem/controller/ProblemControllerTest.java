package com.itnoduck.acmate.problem.controller;

import com.itnoduck.acmate.common.dto.PageResponse;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.common.exception.GlobalExceptionHandler;
import com.itnoduck.acmate.config.MybatisPlusConfig;
import com.itnoduck.acmate.config.SecurityConfig;
import com.itnoduck.acmate.problem.dto.MineProblemStatusFilter;
import com.itnoduck.acmate.problem.dto.MyProblemSummaryResponse;
import com.itnoduck.acmate.problem.dto.ProblemDetailResponse;
import com.itnoduck.acmate.problem.dto.ProblemStatusView;
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    private AuthenticatedUser buildAdminUser() {
        return new AuthenticatedUser(1L, "admin", "hash", "Admin",
                null, null, null, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private AuthenticatedUser buildNormalUser() {
        return new AuthenticatedUser(2L, "user", "hash", "User",
                null, null, null, false, true,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    // --- GET /api/problems (public list) ---

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

    // --- GET /api/problems/{id} ---

    @Test
    void shouldReturnDetailForAuthenticatedUser() throws Exception {
        ProblemDetailResponse detail = new ProblemDetailResponse(1L, "CUSTOM", "EXT-1",
                "Two Sum", "https://example.com", "800", "dp,greedy",
                "## Content", 1L,
                LocalDateTime.of(2026, 7, 20, 12, 0),
                LocalDateTime.of(2026, 7, 21, 12, 0));
        when(problemQueryService.getProblem(eq(1L), anyLong(), anyBoolean())).thenReturn(detail);

        mockMvc.perform(get("/api/problems/1")
                        .with(user(buildNormalUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Two Sum"))
                .andExpect(jsonPath("$.contentMd").value("## Content"))
                .andExpect(jsonPath("$.status").doesNotExist());
    }

    @Test
    void shouldReturn404WhenProblemNotFound() throws Exception {
        when(problemQueryService.getProblem(anyLong(), anyLong(), anyBoolean()))
                .thenThrow(new BusinessException(404, "题目不存在"));

        mockMvc.perform(get("/api/problems/999")
                        .with(user(buildNormalUser())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("题目不存在"));
    }

    @Test
    void shouldPassViewerIdentityToGetProblemService() throws Exception {
        ProblemDetailResponse detail = new ProblemDetailResponse(1L, "CUSTOM", "EXT-1",
                "Two Sum", "https://example.com", "800", "dp,greedy",
                "## Content", 1L,
                LocalDateTime.of(2026, 7, 20, 12, 0),
                LocalDateTime.of(2026, 7, 21, 12, 0));
        when(problemQueryService.getProblem(eq(1L), eq(2L), eq(false))).thenReturn(detail);

        mockMvc.perform(get("/api/problems/1")
                        .with(user(buildNormalUser())))
                .andExpect(status().isOk());

        verify(problemQueryService).getProblem(eq(1L), eq(2L), eq(false));
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

    // --- GET /api/problems/mine ---

    @Test
    void shouldReturn401WhenUnauthenticatedForMine() throws Exception {
        mockMvc.perform(get("/api/problems/mine"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldReturn200ForAuthenticatedUserMine() throws Exception {
        PageResponse<MyProblemSummaryResponse> page = new PageResponse<>(1, 20, 1, 1, List.of());
        when(problemQueryService.listMyProblems(any(), any(MineProblemStatusFilter.class), eq(2L)))
                .thenReturn(page);

        mockMvc.perform(get("/api/problems/mine")
                        .with(user(buildNormalUser())))
                .andExpect(status().isOk());
    }

    @Test
    void shouldPassCurrentUserIdToMineService() throws Exception {
        PageResponse<MyProblemSummaryResponse> page = new PageResponse<>(1, 20, 0, 0, List.of());
        when(problemQueryService.listMyProblems(any(), any(MineProblemStatusFilter.class), eq(2L)))
                .thenReturn(page);

        mockMvc.perform(get("/api/problems/mine")
                        .with(user(buildNormalUser())))
                .andExpect(status().isOk());

        verify(problemQueryService).listMyProblems(any(), any(MineProblemStatusFilter.class), eq(2L));
    }

    @Test
    void shouldBindStatusAllByDefault() throws Exception {
        PageResponse<MyProblemSummaryResponse> page = new PageResponse<>(1, 20, 0, 0, List.of());
        when(problemQueryService.listMyProblems(any(), eq(MineProblemStatusFilter.ALL), eq(2L)))
                .thenReturn(page);

        mockMvc.perform(get("/api/problems/mine")
                        .with(user(buildNormalUser())))
                .andExpect(status().isOk());

        verify(problemQueryService).listMyProblems(any(), eq(MineProblemStatusFilter.ALL), eq(2L));
    }

    @Test
    void shouldBindStatusActive() throws Exception {
        PageResponse<MyProblemSummaryResponse> page = new PageResponse<>(1, 20, 0, 0, List.of());
        when(problemQueryService.listMyProblems(any(), eq(MineProblemStatusFilter.ACTIVE), eq(2L)))
                .thenReturn(page);

        mockMvc.perform(get("/api/problems/mine?status=ACTIVE")
                        .with(user(buildNormalUser())))
                .andExpect(status().isOk());

        verify(problemQueryService).listMyProblems(any(), eq(MineProblemStatusFilter.ACTIVE), eq(2L));
    }

    @Test
    void shouldBindStatusInactive() throws Exception {
        PageResponse<MyProblemSummaryResponse> page = new PageResponse<>(1, 20, 0, 0, List.of());
        when(problemQueryService.listMyProblems(any(), eq(MineProblemStatusFilter.INACTIVE), eq(2L)))
                .thenReturn(page);

        mockMvc.perform(get("/api/problems/mine?status=INACTIVE")
                        .with(user(buildNormalUser())))
                .andExpect(status().isOk());

        verify(problemQueryService).listMyProblems(any(), eq(MineProblemStatusFilter.INACTIVE), eq(2L));
    }

    @Test
    void shouldReturn400WhenInvalidStatus() throws Exception {
        mockMvc.perform(get("/api/problems/mine?status=INVALID")
                        .with(user(buildNormalUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldNotRequireCsrfForMine() throws Exception {
        PageResponse<MyProblemSummaryResponse> page = new PageResponse<>(1, 20, 0, 0, List.of());
        when(problemQueryService.listMyProblems(any(), any(MineProblemStatusFilter.class), eq(2L)))
                .thenReturn(page);

        mockMvc.perform(get("/api/problems/mine")
                        .with(user(buildNormalUser())))
                .andExpect(status().isOk());
    }

    @Test
    void mineShouldReturnStatusField() throws Exception {
        PageResponse<MyProblemSummaryResponse> page = new PageResponse<>(1, 20, 1, 1,
                List.of(new MyProblemSummaryResponse(1L, "CUSTOM", "EXT-1", "Two Sum",
                        "https://example.com", "800", "dp,greedy", ProblemStatusView.ACTIVE,
                        LocalDateTime.of(2026, 7, 20, 12, 0),
                        LocalDateTime.of(2026, 7, 21, 12, 0))));
        when(problemQueryService.listMyProblems(any(), any(MineProblemStatusFilter.class), eq(2L)))
                .thenReturn(page);

        mockMvc.perform(get("/api/problems/mine")
                        .with(user(buildNormalUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].status").value("ACTIVE"));
    }

    // --- POST /api/problems ---

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
    void shouldReturn201WhenNormalUserPostsWithCsrf() throws Exception {
        ProblemDetailResponse detail = new ProblemDetailResponse(1L, "CUSTOM", null,
                "Test", null, "800", "dp", "## Content", 2L,
                LocalDateTime.of(2026, 7, 25, 12, 0),
                LocalDateTime.of(2026, 7, 25, 12, 0));
        when(problemCommandService.createProblem(any(), eq(2L))).thenReturn(detail);

        mockMvc.perform(post("/api/problems")
                        .with(user(buildNormalUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platform\":\"CUSTOM\",\"title\":\"Test\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.platform").value("CUSTOM"))
                .andExpect(jsonPath("$.title").value("Test"))
                .andExpect(jsonPath("$.creatorUserId").value(2));

        verify(problemCommandService).createProblem(any(), eq(2L));
    }

    @Test
    void shouldReturn403WhenNormalUserPostsWithoutCsrf() throws Exception {
        mockMvc.perform(post("/api/problems")
                        .with(user(buildNormalUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platform\":\"CUSTOM\",\"title\":\"Test\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verify(problemCommandService, never()).createProblem(any(), anyLong());
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
        PageResponse<ProblemSummaryResponse> page = new PageResponse<>(1, 20, 0, 0, List.of());
        when(problemQueryService.listProblems(any())).thenReturn(page);

        mockMvc.perform(get("/api/problems")
                        .with(user(buildNormalUser())))
                .andExpect(status().isOk());
    }

    // --- PUT /api/problems/{id} ---

    private ProblemDetailResponse buildUpdateResponse() {
        return new ProblemDetailResponse(1L, "CUSTOM", "EXT-1",
                "Updated", "https://example.com", "900", "dp",
                "## Content", 2L,
                LocalDateTime.of(2026, 7, 25, 12, 0),
                LocalDateTime.of(2026, 7, 26, 12, 0));
    }

    @Test
    void shouldReturn401WhenUnauthenticatedPutWithCsrf() throws Exception {
        mockMvc.perform(put("/api/problems/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platform\":\"CUSTOM\",\"title\":\"Test\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldReturn403WhenUnauthenticatedPutWithoutCsrf() throws Exception {
        mockMvc.perform(put("/api/problems/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platform\":\"CUSTOM\",\"title\":\"Test\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void shouldAllowOwnerToUpdate() throws Exception {
        ProblemDetailResponse detail = buildUpdateResponse();
        when(problemCommandService.updateProblem(eq(1L), any(), eq(2L), eq(false)))
                .thenReturn(detail);

        mockMvc.perform(put("/api/problems/1")
                        .with(user(buildNormalUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platform\":\"CUSTOM\",\"title\":\"Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Updated"))
                .andExpect(jsonPath("$.creatorUserId").value(2));
    }

    @Test
    void shouldPassOwnerUserIdAndAdminFalseToService() throws Exception {
        ProblemDetailResponse detail = buildUpdateResponse();
        when(problemCommandService.updateProblem(eq(1L), any(), eq(2L), eq(false)))
                .thenReturn(detail);

        mockMvc.perform(put("/api/problems/1")
                        .with(user(buildNormalUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platform\":\"CUSTOM\",\"title\":\"Updated\"}"))
                .andExpect(status().isOk());

        verify(problemCommandService).updateProblem(eq(1L), any(), eq(2L), eq(false));
    }

    @Test
    void shouldReturn403WhenNonOwnerNormalUserUpdates() throws Exception {
        // 非创建者普通用户：SecurityFilterChain 只检查登录，Service 判断权限后抛 403
        when(problemCommandService.updateProblem(eq(1L), any(), eq(2L), eq(false)))
                .thenThrow(new BusinessException(403, "无权修改该题目"));

        mockMvc.perform(put("/api/problems/1")
                        .with(user(buildNormalUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platform\":\"CUSTOM\",\"title\":\"Updated\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权修改该题目"));
    }

    @Test
    void shouldAllowAdminToUpdate() throws Exception {
        ProblemDetailResponse detail = buildUpdateResponse();
        when(problemCommandService.updateProblem(eq(1L), any(), eq(1L), eq(true)))
                .thenReturn(detail);

        mockMvc.perform(put("/api/problems/1")
                        .with(user(buildAdminUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platform\":\"CUSTOM\",\"title\":\"Updated\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldPassAdminUserIdAndAdminTrueToService() throws Exception {
        ProblemDetailResponse detail = buildUpdateResponse();
        when(problemCommandService.updateProblem(eq(1L), any(), eq(1L), eq(true)))
                .thenReturn(detail);

        mockMvc.perform(put("/api/problems/1")
                        .with(user(buildAdminUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platform\":\"CUSTOM\",\"title\":\"Updated\"}"))
                .andExpect(status().isOk());

        verify(problemCommandService).updateProblem(eq(1L), any(), eq(1L), eq(true));
    }

    @Test
    void shouldReturn403WhenAdminPutsWithoutCsrf() throws Exception {
        mockMvc.perform(put("/api/problems/1")
                        .with(user(buildAdminUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platform\":\"CUSTOM\",\"title\":\"Test\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verify(problemCommandService, never()).updateProblem(anyLong(), any(), anyLong(), anyBoolean());
    }

    @Test
    void shouldReturn403WhenNormalUserPutsWithoutCsrf() throws Exception {
        mockMvc.perform(put("/api/problems/1")
                        .with(user(buildNormalUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platform\":\"CUSTOM\",\"title\":\"Test\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verify(problemCommandService, never()).updateProblem(anyLong(), any(), anyLong(), anyBoolean());
    }

    @Test
    void shouldIgnoreInjectedFieldsInPutRequestBody() throws Exception {
        ProblemDetailResponse detail = buildUpdateResponse();
        when(problemCommandService.updateProblem(eq(1L), any(), eq(1L), eq(true)))
                .thenReturn(detail);

        mockMvc.perform(put("/api/problems/1")
                        .with(user(buildAdminUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platform\":\"CUSTOM\",\"title\":\"Updated\","
                                + "\"creatorUserId\":999,\"status\":0,"
                                + "\"operatorUserId\":888,\"operatorAdmin\":true}"))
                .andExpect(status().isOk());

        verify(problemCommandService).updateProblem(eq(1L), any(), eq(1L), eq(true));
    }

    @Test
    void shouldReturnUpdateResponseWithRequiredFields() throws Exception {
        ProblemDetailResponse detail = buildUpdateResponse();
        when(problemCommandService.updateProblem(eq(1L), any(), eq(1L), eq(true)))
                .thenReturn(detail);

        mockMvc.perform(put("/api/problems/1")
                        .with(user(buildAdminUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platform\":\"CUSTOM\",\"title\":\"Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.platform").value("CUSTOM"))
                .andExpect(jsonPath("$.externalProblemKey").value("EXT-1"))
                .andExpect(jsonPath("$.title").value("Updated"))
                .andExpect(jsonPath("$.creatorUserId").value(2))
                .andExpect(jsonPath("$.updateTime").value("2026-07-26T12:00:00"));
    }

    @Test
    void shouldNotReturnStatusInUpdateResponse() throws Exception {
        ProblemDetailResponse detail = buildUpdateResponse();
        when(problemCommandService.updateProblem(eq(1L), any(), eq(1L), eq(true)))
                .thenReturn(detail);

        mockMvc.perform(put("/api/problems/1")
                        .with(user(buildAdminUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platform\":\"CUSTOM\",\"title\":\"Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").doesNotExist());
    }

    @Test
    void shouldPassPathIdToUpdateService() throws Exception {
        ProblemDetailResponse detail = buildUpdateResponse();
        when(problemCommandService.updateProblem(eq(42L), any(), eq(1L), eq(true)))
                .thenReturn(detail);

        mockMvc.perform(put("/api/problems/42")
                        .with(user(buildAdminUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platform\":\"CUSTOM\",\"title\":\"Updated\"}"))
                .andExpect(status().isOk());

        verify(problemCommandService).updateProblem(eq(42L), any(), eq(1L), eq(true));
    }

    @Test
    void shouldReturn400WhenPutValidationFails() throws Exception {
        mockMvc.perform(put("/api/problems/1")
                        .with(user(buildAdminUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platform\":\"INVALID\",\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void putShouldStillRequireCsrf() throws Exception {
        // POST and PUT are both write operations needing CSRF
        mockMvc.perform(put("/api/problems/1")
                        .with(user(buildNormalUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platform\":\"CUSTOM\",\"title\":\"Test\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldStillAllowPostForNormalUserAfterPutEndpoint() throws Exception {
        ProblemDetailResponse detail = new ProblemDetailResponse(1L, "CUSTOM", null,
                "Test", null, "800", "dp", "## Content", 2L,
                LocalDateTime.of(2026, 7, 25, 12, 0),
                LocalDateTime.of(2026, 7, 25, 12, 0));
        when(problemCommandService.createProblem(any(), eq(2L))).thenReturn(detail);

        mockMvc.perform(post("/api/problems")
                        .with(user(buildNormalUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"platform\":\"CUSTOM\",\"title\":\"Test\"}"))
                .andExpect(status().isCreated());
    }

    // --- POST /api/problems/{id}/deactivate ---

    @Test
    void deactivateShouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/problems/1/deactivate")
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void deactivateShouldReturn403WhenUnauthenticatedWithoutCsrf() throws Exception {
        mockMvc.perform(post("/api/problems/1/deactivate"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void deactivateShouldReturn403WhenMissingCsrf() throws Exception {
        mockMvc.perform(post("/api/problems/1/deactivate")
                        .with(user(buildNormalUser())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verify(problemCommandService, never()).deactivateProblem(anyLong(), anyLong(), anyBoolean());
    }

    @Test
    void shouldAllowOwnerToDeactivate() throws Exception {
        doNothing().when(problemCommandService).deactivateProblem(eq(1L), eq(2L), eq(false));

        mockMvc.perform(post("/api/problems/1/deactivate")
                        .with(user(buildNormalUser()))
                        .with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$").doesNotExist());

        verify(problemCommandService).deactivateProblem(eq(1L), eq(2L), eq(false));
    }

    @Test
    void shouldAllowAdminToDeactivate() throws Exception {
        doNothing().when(problemCommandService).deactivateProblem(eq(1L), eq(1L), eq(true));

        mockMvc.perform(post("/api/problems/1/deactivate")
                        .with(user(buildAdminUser()))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(problemCommandService).deactivateProblem(eq(1L), eq(1L), eq(true));
    }

    @Test
    void deactivateShouldReturn403WhenNonOwner() throws Exception {
        doThrow(new BusinessException(403, "无权管理该题目"))
                .when(problemCommandService).deactivateProblem(eq(1L), eq(2L), eq(false));

        mockMvc.perform(post("/api/problems/1/deactivate")
                        .with(user(buildNormalUser()))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void deactivateShouldReturn404WhenProblemNotFound() throws Exception {
        doThrow(new BusinessException(404, "题目不存在"))
                .when(problemCommandService).deactivateProblem(eq(1L), eq(2L), eq(false));

        mockMvc.perform(post("/api/problems/1/deactivate")
                        .with(user(buildNormalUser()))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void deactivateShouldReturnEmptyBody() throws Exception {
        doNothing().when(problemCommandService).deactivateProblem(eq(1L), eq(2L), eq(false));

        String content = mockMvc.perform(post("/api/problems/1/deactivate")
                        .with(user(buildNormalUser()))
                        .with(csrf()))
                .andExpect(status().isNoContent())
                .andReturn().getResponse().getContentAsString();

        assertTrue(content.isEmpty());
    }

    // --- POST /api/problems/{id}/restore ---

    @Test
    void restoreShouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/problems/1/restore")
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void restoreShouldReturn403WhenUnauthenticatedWithoutCsrf() throws Exception {
        mockMvc.perform(post("/api/problems/1/restore"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void restoreShouldReturn403WhenMissingCsrf() throws Exception {
        mockMvc.perform(post("/api/problems/1/restore")
                        .with(user(buildNormalUser())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verify(problemCommandService, never()).restoreProblem(anyLong(), anyLong(), anyBoolean());
    }

    @Test
    void shouldAllowOwnerToRestore() throws Exception {
        doNothing().when(problemCommandService).restoreProblem(eq(1L), eq(2L), eq(false));

        mockMvc.perform(post("/api/problems/1/restore")
                        .with(user(buildNormalUser()))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(problemCommandService).restoreProblem(eq(1L), eq(2L), eq(false));
    }

    @Test
    void shouldAllowAdminToRestore() throws Exception {
        doNothing().when(problemCommandService).restoreProblem(eq(1L), eq(1L), eq(true));

        mockMvc.perform(post("/api/problems/1/restore")
                        .with(user(buildAdminUser()))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(problemCommandService).restoreProblem(eq(1L), eq(1L), eq(true));
    }

    @Test
    void restoreShouldReturn403WhenNonOwner() throws Exception {
        doThrow(new BusinessException(403, "无权管理该题目"))
                .when(problemCommandService).restoreProblem(eq(1L), eq(2L), eq(false));

        mockMvc.perform(post("/api/problems/1/restore")
                        .with(user(buildNormalUser()))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void restoreShouldReturnEmptyBody() throws Exception {
        doNothing().when(problemCommandService).restoreProblem(eq(1L), eq(2L), eq(false));

        String content = mockMvc.perform(post("/api/problems/1/restore")
                        .with(user(buildNormalUser()))
                        .with(csrf()))
                .andExpect(status().isNoContent())
                .andReturn().getResponse().getContentAsString();

        assertTrue(content.isEmpty());
    }
}
