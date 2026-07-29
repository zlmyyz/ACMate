package com.itnoduck.acmate.auditlog.controller;

import com.itnoduck.acmate.auditlog.dto.AuditLogListResponse;
import com.itnoduck.acmate.auditlog.dto.AuditLogResponse;
import com.itnoduck.acmate.auditlog.service.AuditLogService;
import com.itnoduck.acmate.config.SecurityConfig;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditLogController.class)
@Import(SecurityConfig.class)
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditLogService auditLogService;

    @MockitoBean
    private DatabaseUserDetailsService databaseUserDetailsService;
    @MockitoBean
    private UserRegistrationService userRegistrationService;
    @MockitoBean
    private UserAuthenticationService userAuthenticationService;

    private AuthenticatedUser buildAdmin() {
        return new AuthenticatedUser(1L, "admin", "hash", "Admin",
                "admin@test.com", null, null, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private AuthenticatedUser buildNormalUser() {
        return new AuthenticatedUser(2L, "user", "hash", "User",
                null, null, null, false, true,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    // ── 1. admin query success ──

    @Test
    void adminShouldListLogs() throws Exception {
        when(auditLogService.listLogs(anyInt(), anyInt(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(new AuditLogListResponse(List.of(
                        new AuditLogResponse(1L, "USER_DEACTIVATED", 1L, "admin", "Admin",
                                "USER", 2L, "ACTIVE", "DEACTIVATED", "bad", "2026-07-29T12:00:00")),
                        1, 1, 20));

        mockMvc.perform(get("/api/admin/audit-logs")
                        .with(user(buildAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].actionType").value("USER_DEACTIVATED"))
                .andExpect(jsonPath("$.items[0].actorUsername").value("admin"))
                .andExpect(jsonPath("$.total").value(1));
    }

    // ── 2. unauthenticated 401 ──

    @Test
    void unauthenticatedShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs"))
                .andExpect(status().isUnauthorized());
    }

    // ── 3. normal user 403 ──

    @Test
    void normalUserShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs")
                        .with(user(buildNormalUser())))
                .andExpect(status().isForbidden());
    }

    // ── 4. pagination ──

    @Test
    void paginationParamsShouldBePassed() throws Exception {
        when(auditLogService.listLogs(anyInt(), anyInt(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(new AuditLogListResponse(List.of(), 0, 2, 10));

        mockMvc.perform(get("/api/admin/audit-logs")
                        .param("page", "2")
                        .param("size", "10")
                        .with(user(buildAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2));
    }

    // ── 5. actionType filter ──

    @Test
    void actionTypeFilterShouldBePassed() throws Exception {
        when(auditLogService.listLogs(anyInt(), anyInt(), isNull(), eq("USER_DEACTIVATED"), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(new AuditLogListResponse(List.of(), 0, 1, 20));

        mockMvc.perform(get("/api/admin/audit-logs")
                        .param("actionType", "USER_DEACTIVATED")
                        .with(user(buildAdmin())))
                .andExpect(status().isOk());
    }

    // ── 6. targetType filter ──

    @Test
    void targetTypeFilterShouldBePassed() throws Exception {
        when(auditLogService.listLogs(anyInt(), anyInt(), isNull(), isNull(), eq("USER"), isNull(), isNull(), isNull(), any()))
                .thenReturn(new AuditLogListResponse(List.of(), 0, 1, 20));

        mockMvc.perform(get("/api/admin/audit-logs")
                        .param("targetType", "USER")
                        .with(user(buildAdmin())))
                .andExpect(status().isOk());
    }

    // ── 7. invalid page/size clamped ──

    @Test
    void invalidPageSizeShouldBeClamped() throws Exception {
        when(auditLogService.listLogs(anyInt(), anyInt(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(new AuditLogListResponse(List.of(), 0, 1, 20));

        mockMvc.perform(get("/api/admin/audit-logs")
                        .param("page", "0")
                        .param("size", "200")
                        .with(user(buildAdmin())))
                .andExpect(status().isOk());
    }

    // ── 8. response contains correct fields ──

    @Test
    void responseShouldContainCorrectFields() throws Exception {
        when(auditLogService.listLogs(anyInt(), anyInt(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(new AuditLogListResponse(List.of(
                        new AuditLogResponse(1L, "USER_DEACTIVATED", 1L, "admin", "Admin",
                                "USER", 2L, "ACTIVE", "DEACTIVATED", "bad", "2026-07-29T12:00:00")),
                        1, 1, 20));

        mockMvc.perform(get("/api/admin/audit-logs")
                        .with(user(buildAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").exists())
                .andExpect(jsonPath("$.items[0].actionType").value("USER_DEACTIVATED"))
                .andExpect(jsonPath("$.items[0].actorUsername").value("admin"))
                .andExpect(jsonPath("$.items[0].actorNickname").value("Admin"))
                .andExpect(jsonPath("$.items[0].targetType").value("USER"))
                .andExpect(jsonPath("$.items[0].reason").value("bad"))
                .andExpect(jsonPath("$.items[0].beforeState").value("ACTIVE"))
                .andExpect(jsonPath("$.items[0].afterState").value("DEACTIVATED"))
                .andExpect(jsonPath("$.items[0].createTime").exists());
    }

    // ── 9. keyword and time params ──

    @Test
    void keywordAndTimeParamsShouldBePassed() throws Exception {
        when(auditLogService.listLogs(anyInt(), anyInt(), eq("admin"), isNull(), isNull(), isNull(), eq("2026-07-01T00:00:00"), eq("2026-07-29T23:59:59"), any()))
                .thenReturn(new AuditLogListResponse(List.of(), 0, 1, 20));

        mockMvc.perform(get("/api/admin/audit-logs")
                        .param("actorKeyword", "admin")
                        .param("startTime", "2026-07-01T00:00:00")
                        .param("endTime", "2026-07-29T23:59:59")
                        .with(user(buildAdmin())))
                .andExpect(status().isOk());
    }
}
