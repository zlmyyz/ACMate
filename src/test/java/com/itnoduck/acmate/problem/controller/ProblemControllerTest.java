package com.itnoduck.acmate.problem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itnoduck.acmate.common.dto.PageResponse;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.common.exception.GlobalExceptionHandler;
import com.itnoduck.acmate.config.MybatisPlusConfig;
import com.itnoduck.acmate.config.SecurityConfig;
import com.itnoduck.acmate.problem.dto.ProblemDetailResponse;
import com.itnoduck.acmate.problem.dto.ProblemSummaryResponse;
import com.itnoduck.acmate.problem.service.ProblemQueryService;
import com.itnoduck.acmate.security.DatabaseUserDetailsService;
import com.itnoduck.acmate.user.service.UserAuthenticationService;
import com.itnoduck.acmate.user.service.UserRegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
