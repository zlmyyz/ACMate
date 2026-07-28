package com.itnoduck.acmate.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itnoduck.acmate.auditlog.service.AuditLogService;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.security.AuthenticatedUser;
import com.itnoduck.acmate.testutil.MybatisPlusTestHelper;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.session.SessionRegistry;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private AppUserMapper userMapper;
    @Mock
    private SessionRegistry sessionRegistry;
    @Mock
    private AuditLogService auditLogService;

    private AdminUserServiceImpl service;

    private final AuthenticatedUser admin = new AuthenticatedUser(1L, "admin", "hash", "Admin",
            null, null, null, true, true,
            List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN")));

    private final AuthenticatedUser normal = new AuthenticatedUser(2L, "user", "hash", "User",
            null, null, null, false, true,
            List.of(new SimpleGrantedAuthority("ROLE_USER")));

    private AppUser buildUser(Long id, String username, int status, int isAdmin) {
        AppUser u = new AppUser();
        u.setId(id);
        u.setUsername(username);
        u.setNickname(username);
        u.setPasswordHash("hash");
        u.setStatus(status);
        u.setIsAdmin(isAdmin);
        return u;
    }

    @BeforeEach
    void setUp() {
        MybatisPlusTestHelper.initEntityTables();
        service = new AdminUserServiceImpl(userMapper, sessionRegistry, auditLogService);
    }

    // ── List tests ──

    @Test
    void listShouldThrow403WhenNotAdmin() {
        assertThrows(BusinessException.class,
                () -> service.listUsers(1, 20, null, null, null, normal));
    }

    @Test
    void listShouldDelegateToMapper() {
        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<AppUser>(1, 20) {{ setTotal(0); setRecords(List.of()); }});

        var result = service.listUsers(1, 20, "test", "ACTIVE", "ADMIN", admin);
        assertEquals(0L, result.get("total"));
    }

    @Test
    void listShouldFilterByKeywordUsername() {
        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<AppUser>(1, 20) {{ setTotal(1); setRecords(List.of(buildUser(1L, "testuser", 1, 0))); }});

        var result = service.listUsers(1, 20, "testuser", null, null, admin);
        assertEquals(1L, result.get("total"));
    }

    @Test
    void listShouldFilterByActive() {
        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<AppUser>(1, 20) {{ setTotal(0); setRecords(List.of()); }});

        service.listUsers(1, 20, null, "ACTIVE", null, admin);
        verify(userMapper).selectPage(any(Page.class), argThat((LambdaQueryWrapper<AppUser> qw) -> true));
    }

    @Test
    void listShouldFilterByInactive() {
        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<AppUser>(1, 20) {{ setTotal(0); setRecords(List.of()); }});

        service.listUsers(1, 20, null, "INACTIVE", null, admin);
    }

    @Test
    void listShouldFilterByAdminRole() {
        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<AppUser>(1, 20) {{ setTotal(0); setRecords(List.of()); }});

        service.listUsers(1, 20, null, null, "ADMIN", admin);
    }

    @Test
    void listShouldFilterByUserRole() {
        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<AppUser>(1, 20) {{ setTotal(0); setRecords(List.of()); }});

        service.listUsers(1, 20, null, null, "USER", admin);
    }

    @Test
    void listShouldNotReturnPasswordHash() {
        var user = buildUser(1L, "test", 1, 0);
        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<AppUser>(1, 20) {{ setTotal(1); setRecords(List.of(user)); }});

        var result = service.listUsers(1, 20, null, null, null, admin);
        var users = (List<?>) result.get("users");
        assertFalse(users.isEmpty());
    }

    // ── Deactivate tests ──

    @Test
    void deactivateShouldThrow403WhenNotAdmin() {
        assertThrows(BusinessException.class, () -> service.deactivate(1L, "reason", normal));
    }

    @Test
    void deactivateShouldThrow400WhenSelf() {
        assertThrows(BusinessException.class, () -> service.deactivate(1L, "reason", admin));
    }

    @Test
    void deactivateShouldThrow404WhenNotFound() {
        when(userMapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.deactivate(99L, "reason", admin));
    }

    @Test
    void deactivateShouldSucceedForNormalUser() {
        var target = buildUser(2L, "alice", 1, 0);
        when(userMapper.selectById(2L)).thenReturn(target);

        service.deactivate(2L, "违规行为", admin);

        verify(userMapper).update(any(), any());
        verify(auditLogService).log(eq(1L), eq("USER_DEACTIVATED"), eq("USER"), eq(2L), eq("违规行为"), eq("ACTIVE"), eq("DEACTIVATED"));
    }

    @Test
    void deactivateShouldThrowWhenLastActiveAdmin() {
        var target = buildUser(2L, "admin2", 1, 1);
        when(userMapper.selectById(2L)).thenReturn(target);
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.deactivate(2L, "reason", admin));
    }

    @Test
    void deactivateShouldBeIdempotent() {
        var target = buildUser(2L, "alice", 0, 0);
        when(userMapper.selectById(2L)).thenReturn(target);

        service.deactivate(2L, "reason", admin);

        verify(userMapper, never()).update(any(), any());
        verify(auditLogService, never()).log(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void deactivateShouldExpireSessions() {
        var target = buildUser(2L, "alice", 1, 0);
        when(userMapper.selectById(2L)).thenReturn(target);

        service.deactivate(2L, "reason", admin);

        verify(userMapper).update(any(), any());
    }

    // ── Restore tests ──

    @Test
    void restoreShouldThrow403WhenNotAdmin() {
        assertThrows(BusinessException.class, () -> service.restore(1L, normal));
    }

    @Test
    void restoreShouldThrow404WhenNotFound() {
        when(userMapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.restore(99L, admin));
    }

    @Test
    void restoreShouldSucceed() {
        var target = buildUser(2L, "alice", 0, 0);
        when(userMapper.selectById(2L)).thenReturn(target);

        service.restore(2L, admin);

        verify(userMapper).update(any(), any());
        verify(auditLogService).log(eq(1L), eq("USER_RESTORED"), eq("USER"), eq(2L), isNull(), eq("DEACTIVATED"), eq("ACTIVE"));
    }

    @Test
    void restoreShouldBeIdempotent() {
        var target = buildUser(2L, "alice", 1, 0);
        when(userMapper.selectById(2L)).thenReturn(target);

        service.restore(2L, admin);

        verify(userMapper, never()).update(any(), any());
        verify(auditLogService, never()).log(any(), any(), any(), any(), any(), any(), any());
    }

    // ── Grant admin tests ──

    @Test
    void grantAdminShouldThrow403WhenNotAdmin() {
        assertThrows(BusinessException.class, () -> service.grantAdmin(1L, normal));
    }

    @Test
    void grantAdminShouldThrow404WhenNotFound() {
        when(userMapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.grantAdmin(99L, admin));
    }

    @Test
    void grantAdminShouldSucceed() {
        var target = buildUser(2L, "alice", 1, 0);
        when(userMapper.selectById(2L)).thenReturn(target);

        service.grantAdmin(2L, admin);

        verify(userMapper).update(any(), any());
        verify(auditLogService).log(eq(1L), eq("ADMIN_GRANTED"), eq("USER"), eq(2L), isNull(), eq("USER"), eq("ADMIN"));
    }

    @Test
    void grantAdminShouldBeIdempotent() {
        var target = buildUser(2L, "alice", 1, 1);
        when(userMapper.selectById(2L)).thenReturn(target);

        service.grantAdmin(2L, admin);

        verify(userMapper, never()).update(any(), any());
        verify(auditLogService, never()).log(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void grantAdminShouldExpireSessions() {
        var target = buildUser(2L, "alice", 1, 0);
        when(userMapper.selectById(2L)).thenReturn(target);

        service.grantAdmin(2L, admin);

        verify(userMapper).update(any(), any());
    }

    // ── Revoke admin tests ──

    @Test
    void revokeAdminShouldThrow403WhenNotAdmin() {
        assertThrows(BusinessException.class, () -> service.revokeAdmin(1L, normal));
    }

    @Test
    void revokeAdminShouldThrow400WhenSelf() {
        assertThrows(BusinessException.class, () -> service.revokeAdmin(1L, admin));
    }

    @Test
    void revokeAdminShouldThrow404WhenNotFound() {
        when(userMapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.revokeAdmin(99L, admin));
    }

    @Test
    void revokeAdminShouldThrowWhenLastActiveAdmin() {
        var target = buildUser(2L, "admin2", 1, 1);
        when(userMapper.selectById(2L)).thenReturn(target);
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.revokeAdmin(2L, admin));
    }

    @Test
    void revokeAdminShouldSucceed() {
        var target = buildUser(2L, "admin2", 1, 1);
        when(userMapper.selectById(2L)).thenReturn(target);
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);

        service.revokeAdmin(2L, admin);

        verify(userMapper).update(any(), any());
        verify(auditLogService).log(eq(1L), eq("ADMIN_REVOKED"), eq("USER"), eq(2L), isNull(), eq("ADMIN"), eq("USER"));
    }

    @Test
    void revokeAdminShouldBeIdempotent() {
        var target = buildUser(2L, "alice", 1, 0);
        when(userMapper.selectById(2L)).thenReturn(target);

        service.revokeAdmin(2L, admin);

        verify(userMapper, never()).update(any(), any());
        verify(auditLogService, never()).log(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void revokeAdminShouldExpireSessions() {
        var target = buildUser(2L, "admin2", 1, 1);
        when(userMapper.selectById(2L)).thenReturn(target);
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);

        service.revokeAdmin(2L, admin);

        verify(userMapper).update(any(), any());
    }
}
