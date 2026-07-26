package com.itnoduck.acmate.problem.controller;

import com.itnoduck.acmate.common.dto.PageResponse;
import com.itnoduck.acmate.config.MybatisPlusConfig;
import com.itnoduck.acmate.config.SecurityConfig;
import com.itnoduck.acmate.problem.dto.AdminProblemSummaryResponse;
import com.itnoduck.acmate.problem.dto.MineProblemStatusFilter;
import com.itnoduck.acmate.problem.dto.ProblemStatusView;
import com.itnoduck.acmate.problem.service.AdminProblemQueryService;
import com.itnoduck.acmate.security.AuthenticatedUser;
import com.itnoduck.acmate.security.DatabaseUserDetailsService;
import com.itnoduck.acmate.user.service.UserAuthenticationService;
import com.itnoduck.acmate.user.service.UserRegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminProblemController.class)
@Import({
    SecurityConfig.class,
    MybatisPlusConfig.class
})
class AdminProblemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminProblemQueryService adminProblemQueryService;

    @MockitoBean
    private DatabaseUserDetailsService databaseUserDetailsService;

    @MockitoBean
    private UserRegistrationService userRegistrationService;

    @MockitoBean
    private UserAuthenticationService userAuthenticationService;

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
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/problems"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenNormalUser() throws Exception {
        mockMvc.perform(get("/api/admin/problems")
                        .with(user(buildNormalUser())))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn200WhenAdmin() throws Exception {
        PageResponse<AdminProblemSummaryResponse> page = new PageResponse<>(1, 20, 0, 0, List.of());
        when(adminProblemQueryService.listProblems(any(), any(MineProblemStatusFilter.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/problems")
                        .with(user(buildAdminUser())))
                .andExpect(status().isOk());
    }

    @Test
    void shouldBindStatusDefaultAll() throws Exception {
        PageResponse<AdminProblemSummaryResponse> page = new PageResponse<>(1, 20, 0, 0, List.of());
        when(adminProblemQueryService.listProblems(any(), eq(MineProblemStatusFilter.ALL)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/problems")
                        .with(user(buildAdminUser())))
                .andExpect(status().isOk());

        verify(adminProblemQueryService).listProblems(any(), eq(MineProblemStatusFilter.ALL));
    }

    @Test
    void shouldBindStatusActive() throws Exception {
        PageResponse<AdminProblemSummaryResponse> page = new PageResponse<>(1, 20, 0, 0, List.of());
        when(adminProblemQueryService.listProblems(any(), eq(MineProblemStatusFilter.ACTIVE)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/problems?status=ACTIVE")
                        .with(user(buildAdminUser())))
                .andExpect(status().isOk());

        verify(adminProblemQueryService).listProblems(any(), eq(MineProblemStatusFilter.ACTIVE));
    }

    @Test
    void shouldBindStatusInactive() throws Exception {
        PageResponse<AdminProblemSummaryResponse> page = new PageResponse<>(1, 20, 0, 0, List.of());
        when(adminProblemQueryService.listProblems(any(), eq(MineProblemStatusFilter.INACTIVE)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/problems?status=INACTIVE")
                        .with(user(buildAdminUser())))
                .andExpect(status().isOk());

        verify(adminProblemQueryService).listProblems(any(), eq(MineProblemStatusFilter.INACTIVE));
    }

    @Test
    void shouldReturn400WhenInvalidStatus() throws Exception {
        mockMvc.perform(get("/api/admin/problems?status=INVALID")
                        .with(user(buildAdminUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnCreatorInfoInResponse() throws Exception {
        PageResponse<AdminProblemSummaryResponse> page = new PageResponse<>(1, 20, 1, 1,
                List.of(new AdminProblemSummaryResponse(1L, "CUSTOM", "EXT-1", "Test",
                        "https://example.com", "800", "dp", ProblemStatusView.ACTIVE,
                        10L, "testuser", "Test User",
                        LocalDateTime.of(2026, 7, 25, 12, 0),
                        LocalDateTime.of(2026, 7, 26, 12, 0))));
        when(adminProblemQueryService.listProblems(any(), any(MineProblemStatusFilter.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/problems")
                        .with(user(buildAdminUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].creatorUserId").value(10))
                .andExpect(jsonPath("$.records[0].creatorUsername").value("testuser"))
                .andExpect(jsonPath("$.records[0].creatorNickname").value("Test User"))
                .andExpect(jsonPath("$.records[0].status").value("ACTIVE"));
    }

    @Test
    void shouldNotRequireCsrf() throws Exception {
        PageResponse<AdminProblemSummaryResponse> page = new PageResponse<>(1, 20, 0, 0, List.of());
        when(adminProblemQueryService.listProblems(any(), any(MineProblemStatusFilter.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/problems")
                        .with(user(buildAdminUser())))
                .andExpect(status().isOk());
    }
}
