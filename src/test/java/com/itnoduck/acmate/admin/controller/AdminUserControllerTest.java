package com.itnoduck.acmate.admin.controller;

import com.itnoduck.acmate.admin.service.AdminUserService;
import com.itnoduck.acmate.config.SecurityConfig;
import com.itnoduck.acmate.security.AuthenticatedUser;
import com.itnoduck.acmate.security.DatabaseUserDetailsService;
import com.itnoduck.acmate.user.service.UserAuthenticationService;
import com.itnoduck.acmate.user.service.UserRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserController.class)
@Import(SecurityConfig.class)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    @MockitoBean
    private DatabaseUserDetailsService databaseUserDetailsService;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.reset(databaseUserDetailsService);
    }

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

    private HashMap<String, Object> userMap(Long id, String username, String nickname, boolean admin, int status) {
        var m = new HashMap<String, Object>();
        m.put("id", id);
        m.put("username", username);
        m.put("nickname", nickname);
        m.put("email", null);
        m.put("avatarUrl", null);
        m.put("bio", null);
        m.put("admin", admin);
        m.put("status", status);
        m.put("createTime", "2026-01-01T00:00:00");
        m.put("lastLoginTime", null);
        return m;
    }

    // ── List tests ──

    @Test
    void listShouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void listShouldReturn403WhenNormalUser() throws Exception {
        mockMvc.perform(get("/api/admin/users").with(user(buildNormalUser())))
                .andExpect(status().isForbidden());
    }

    @Test
    void listShouldReturn200WhenAdmin() throws Exception {
        var resp = new HashMap<String, Object>();
        resp.put("users", List.of());
        resp.put("total", 0L);
        resp.put("page", 1);
        resp.put("size", 20);
        when(adminUserService.listUsers(anyInt(), anyInt(), anyString(), any(), any(), any())).thenReturn(resp);

        mockMvc.perform(get("/api/admin/users").with(user(buildAdmin())))
                .andExpect(status().isOk());
    }

    @Test
    void listShouldFilterByKeyword() throws Exception {
        var resp = new HashMap<String, Object>();
        resp.put("users", List.of(userMap(1L, "testuser", "Test", false, 1)));
        resp.put("total", 1L);
        resp.put("page", 1);
        resp.put("size", 20);
        when(adminUserService.listUsers(anyInt(), anyInt(), eq("test"), any(), any(), any())).thenReturn(resp);

        mockMvc.perform(get("/api/admin/users?keyword=test").with(user(buildAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[0].username").value("testuser"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void listShouldFilterByStatus() throws Exception {
        var resp = new HashMap<String, Object>();
        resp.put("users", List.of());
        resp.put("total", 0L);
        resp.put("page", 1);
        resp.put("size", 20);
        when(adminUserService.listUsers(anyInt(), anyInt(), anyString(), eq("ACTIVE"), any(), any())).thenReturn(resp);

        mockMvc.perform(get("/api/admin/users?status=ACTIVE").with(user(buildAdmin()))).andExpect(status().isOk());
        verify(adminUserService).listUsers(anyInt(), anyInt(), anyString(), eq("ACTIVE"), any(), any());
    }

    @Test
    void listShouldFilterByAdmin() throws Exception {
        var resp = new HashMap<String, Object>();
        resp.put("users", List.of());
        resp.put("total", 0L);
        resp.put("page", 1);
        resp.put("size", 20);
        when(adminUserService.listUsers(anyInt(), anyInt(), anyString(), any(), eq("ADMIN"), any())).thenReturn(resp);

        mockMvc.perform(get("/api/admin/users?admin=ADMIN").with(user(buildAdmin()))).andExpect(status().isOk());
        verify(adminUserService).listUsers(anyInt(), anyInt(), anyString(), any(), eq("ADMIN"), any());
    }

    @Test
    void listShouldNotReturnPasswordHash() throws Exception {
        var resp = new HashMap<String, Object>();
        resp.put("users", List.of(userMap(1L, "testuser", "Test", false, 1)));
        resp.put("total", 1L);
        resp.put("page", 1);
        resp.put("size", 20);
        when(adminUserService.listUsers(anyInt(), anyInt(), anyString(), any(), any(), any())).thenReturn(resp);

        mockMvc.perform(get("/api/admin/users").with(user(buildAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[0].passwordHash").doesNotExist());
    }

    // ── Deactivate tests ──

    @Test
    void deactivateShouldReturn403WhenNoCsrf() throws Exception {
        mockMvc.perform(put("/api/admin/users/1/deactivate")
                        .with(user(buildAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"违规\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivateShouldReturn403WhenNormalUser() throws Exception {
        mockMvc.perform(put("/api/admin/users/1/deactivate")
                        .with(user(buildNormalUser()))
                        .with(csrf())  // CSRF ok but normal user lacks ADMIN role
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"违规\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivateShouldReturn400WhenReasonEmpty() throws Exception {
        mockMvc.perform(put("/api/admin/users/1/deactivate")
                        .with(user(buildAdmin()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deactivateShouldReturn204WhenAdmin() throws Exception {
        mockMvc.perform(put("/api/admin/users/2/deactivate")
                        .with(user(buildAdmin()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"违规行为\"}"))
                .andExpect(status().isNoContent());
    }

    // ── Restore tests ──

    @Test
    void restoreShouldReturn403WhenNoCsrf() throws Exception {
        mockMvc.perform(put("/api/admin/users/1/restore")
                        .with(user(buildAdmin()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void restoreShouldReturn403WhenNormalUser() throws Exception {
        mockMvc.perform(put("/api/admin/users/1/restore")
                        .with(user(buildNormalUser()))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void restoreShouldReturn204WhenAdmin() throws Exception {
        mockMvc.perform(put("/api/admin/users/2/restore")
                        .with(user(buildAdmin()))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    // ── Grant admin tests ──

    @Test
    void grantAdminShouldReturn204() throws Exception {
        mockMvc.perform(put("/api/admin/users/2/grant-admin")
                        .with(user(buildAdmin()))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void grantAdminShouldReturn403WhenNormalUser() throws Exception {
        mockMvc.perform(put("/api/admin/users/2/grant-admin")
                        .with(user(buildNormalUser()))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ── Revoke admin tests ──

    @Test
    void revokeAdminShouldReturn204() throws Exception {
        mockMvc.perform(put("/api/admin/users/2/revoke-admin")
                        .with(user(buildAdmin()))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void revokeAdminShouldReturn403WhenNormalUser() throws Exception {
        mockMvc.perform(put("/api/admin/users/2/revoke-admin")
                        .with(user(buildNormalUser()))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
