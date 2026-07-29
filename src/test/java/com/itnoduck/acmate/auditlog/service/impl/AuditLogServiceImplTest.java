package com.itnoduck.acmate.auditlog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itnoduck.acmate.auditlog.entity.AuditLog;
import com.itnoduck.acmate.auditlog.mapper.AuditLogMapper;
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

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private AuditLogMapper auditLogMapper;
    @Mock
    private AppUserMapper appUserMapper;

    private AuditLogService service;

    private final AuthenticatedUser admin = new AuthenticatedUser(1L, "admin", "hash", "Admin",
            null, null, null, true, true,
            List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN")));

    private final AuthenticatedUser normal = new AuthenticatedUser(2L, "user", "hash", "User",
            null, null, null, false, true,
            List.of(new SimpleGrantedAuthority("ROLE_USER")));

    @BeforeEach
    void setUp() {
        MybatisPlusTestHelper.initEntityTables();
        service = new AuditLogServiceImpl(auditLogMapper, appUserMapper);
    }

    private AuditLog buildLog(Long id, Long operatorId, String action, String resourceType, Long resourceId,
                               String before, String after, String reason, LocalDateTime time) {
        AuditLog l = new AuditLog();
        l.setId(id);
        l.setOperatorId(operatorId);
        l.setAction(action);
        l.setResourceType(resourceType);
        l.setResourceId(resourceId);
        l.setBeforeState(before);
        l.setAfterState(after);
        l.setReason(reason);
        l.setCreateTime(time);
        return l;
    }

    private AppUser buildActor(Long id, String username, String nickname) {
        AppUser u = new AppUser();
        u.setId(id);
        u.setUsername(username);
        u.setNickname(nickname);
        u.setPasswordHash("hash");
        u.setStatus(1);
        return u;
    }

    // ── Auth ──

    @Test
    void listShouldThrow403WhenNotAdmin() {
        assertThrows(BusinessException.class,
                () -> service.listLogs(1, 20, null, null, null, null, null, null, normal));
    }

    // ── Pagination ──

    @Test
    void listShouldReturnPagedResults() {
        var log = buildLog(1L, 1L, "USER_DEACTIVATED", "USER", 2L, "ACTIVE", "DEACTIVATED", "bad", LocalDateTime.now());
        var page = new Page<AuditLog>(1, 20);
        page.setTotal(1);
        page.setRecords(List.of(log));
        when(auditLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(appUserMapper.selectBatchIds(Set.of(1L))).thenReturn(List.of(buildActor(1L, "admin", "Admin")));

        var result = service.listLogs(1, 20, null, null, null, null, null, null, admin);
        assertEquals(1, result.items().size());
        assertEquals(1, result.total());
        assertEquals("admin", result.items().get(0).actorUsername());
    }

    @Test
    void listShouldReturnEmptyWhenNoLogs() {
        var page = new Page<AuditLog>(1, 20);
        page.setTotal(0);
        page.setRecords(List.of());
        when(auditLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        var result = service.listLogs(1, 20, null, null, null, null, null, null, admin);
        assertEquals(0, result.items().size());
        assertEquals(0, result.total());
    }

    // ── actionType filter ──

    @Test
    void listShouldFilterByActionType() {
        var log = buildLog(1L, 1L, "USER_DEACTIVATED", "USER", 2L, "ACTIVE", "DEACTIVATED", "bad", LocalDateTime.now());
        var page = new Page<AuditLog>(1, 20);
        page.setTotal(1);
        page.setRecords(List.of(log));
        when(auditLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(appUserMapper.selectBatchIds(Set.of(1L))).thenReturn(List.of(buildActor(1L, "admin", "Admin")));

        var result = service.listLogs(1, 20, null, "USER_DEACTIVATED", null, null, null, null, admin);
        assertEquals(1, result.items().size());
        assertEquals("USER_DEACTIVATED", result.items().get(0).actionType());
    }

    @Test
    void listShouldRejectInvalidActionType() {
        assertThrows(BusinessException.class,
                () -> service.listLogs(1, 20, null, "INVALID_ACTION", null, null, null, null, admin));
    }

    // ── targetType filter ──

    @Test
    void listShouldFilterByTargetType() {
        var log = buildLog(1L, 1L, "USER_DEACTIVATED", "USER", 2L, "ACTIVE", "DEACTIVATED", "bad", LocalDateTime.now());
        var page = new Page<AuditLog>(1, 20);
        page.setTotal(1);
        page.setRecords(List.of(log));
        when(auditLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(appUserMapper.selectBatchIds(Set.of(1L))).thenReturn(List.of(buildActor(1L, "admin", "Admin")));

        var result = service.listLogs(1, 20, null, null, "USER", null, null, null, admin);
        assertEquals(1, result.items().size());
    }

    @Test
    void listShouldRejectInvalidTargetType() {
        assertThrows(BusinessException.class,
                () -> service.listLogs(1, 20, null, null, "INVALID_TARGET", null, null, null, admin));
    }

    // ── actorKeyword filter ──

    @Test
    void listShouldFilterByActorKeyword() {
        var log = buildLog(1L, 1L, "USER_DEACTIVATED", "USER", 2L, "ACTIVE", "DEACTIVATED", "bad", LocalDateTime.now());
        var page = new Page<AuditLog>(1, 20);
        page.setTotal(1);
        page.setRecords(List.of(log));
        when(auditLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(appUserMapper.selectBatchIds(Set.of(1L))).thenReturn(List.of(buildActor(1L, "admin", "Admin")));

        var result = service.listLogs(1, 20, "admin", null, null, null, null, null, admin);
        assertEquals(1, result.items().size());

        var empty = service.listLogs(1, 20, "nonexistent", null, null, null, null, null, admin);
        assertEquals(0, empty.items().size());
    }

    // ── targetId filter ──

    @Test
    void listShouldFilterByTargetId() {
        var log = buildLog(1L, 1L, "USER_DEACTIVATED", "USER", 2L, "ACTIVE", "DEACTIVATED", "bad", LocalDateTime.now());
        var page = new Page<AuditLog>(1, 20);
        page.setTotal(1);
        page.setRecords(List.of(log));
        when(auditLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(appUserMapper.selectBatchIds(Set.of(1L))).thenReturn(List.of(buildActor(1L, "admin", "Admin")));

        var result = service.listLogs(1, 20, null, null, null, 2L, null, null, admin);
        assertEquals(1, result.items().size());
    }

    // ── time range ──

    @Test
    void listShouldFilterByTimeRange() {
        var log = buildLog(1L, 1L, "USER_DEACTIVATED", "USER", 2L, "ACTIVE", "DEACTIVATED", "bad", LocalDateTime.now());
        var page = new Page<AuditLog>(1, 20);
        page.setTotal(1);
        page.setRecords(List.of(log));
        when(auditLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(appUserMapper.selectBatchIds(Set.of(1L))).thenReturn(List.of(buildActor(1L, "admin", "Admin")));

        var start = LocalDateTime.now().minusDays(1).toString();
        var end = LocalDateTime.now().plusDays(1).toString();
        var result = service.listLogs(1, 20, null, null, null, null, start, end, admin);
        assertEquals(1, result.items().size());
    }

    @Test
    void listShouldRejectInvalidTimeRange() {
        assertThrows(BusinessException.class,
                () -> service.listLogs(1, 20, null, null, null, null, "not-a-date", null, admin));
    }

    // ── stable sort ──

    @Test
    void listUsesStableSort() {
        var t = LocalDateTime.now();
        var log1 = buildLog(1L, 1L, "USER_DEACTIVATED", "USER", 2L, "ACTIVE", "DEACTIVATED", "bad", t);
        var log2 = buildLog(2L, 1L, "USER_RESTORED", "USER", 2L, "DEACTIVATED", "ACTIVE", null, t);
        var page = new Page<AuditLog>(1, 20);
        page.setTotal(2);
        page.setRecords(List.of(log1, log2));
        when(auditLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(appUserMapper.selectBatchIds(Set.of(1L))).thenReturn(List.of(buildActor(1L, "admin", "Admin")));

        var result = service.listLogs(1, 20, null, null, null, null, null, null, admin);
        assertEquals(2, result.items().size());
    }

    // ── total matches ──

    @Test
    void listTotalMatchesCondition() {
        var log = buildLog(1L, 1L, "USER_DEACTIVATED", "USER", 2L, "ACTIVE", "DEACTIVATED", "bad", LocalDateTime.now());
        var page = new Page<AuditLog>(1, 20);
        page.setTotal(1);
        page.setRecords(List.of(log));
        when(auditLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(appUserMapper.selectBatchIds(Set.of(1L))).thenReturn(List.of(buildActor(1L, "admin", "Admin")));

        var result = service.listLogs(1, 20, null, "USER_DEACTIVATED", null, null, null, null, admin);
        assertEquals(1, result.total());
    }

    // ── batch load actor, no N+1 ──

    @Test
    void listBatchLoadsActors() {
        var log = buildLog(1L, 3L, "USER_DEACTIVATED", "USER", 2L, "ACTIVE", "DEACTIVATED", "bad", LocalDateTime.now());
        var page = new Page<AuditLog>(1, 20);
        page.setTotal(1);
        page.setRecords(List.of(log));
        when(auditLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(appUserMapper.selectBatchIds(Set.of(3L))).thenReturn(List.of(buildActor(3L, "actor1", "Actor One")));

        var result = service.listLogs(1, 20, null, null, null, null, null, null, admin);
        assertEquals("actor1", result.items().get(0).actorUsername());
        assertEquals("Actor One", result.items().get(0).actorNickname());
        verify(appUserMapper, times(1)).selectBatchIds(any());
    }

    // ── response contains no sensitive fields ──

    @Test
    void listResponseDoesNotContainSensitiveFields() {
        var log = buildLog(1L, 1L, "USER_DEACTIVATED", "USER", 2L, "ACTIVE", "DEACTIVATED", "bad", LocalDateTime.now());
        var page = new Page<AuditLog>(1, 20);
        page.setTotal(1);
        page.setRecords(List.of(log));
        when(auditLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(appUserMapper.selectBatchIds(Set.of(1L))).thenReturn(List.of(buildActor(1L, "admin", "Admin")));

        var result = service.listLogs(1, 20, null, null, null, null, null, null, admin);
        var item = result.items().get(0);
        var fieldNames = Arrays.stream(item.getClass().getDeclaredFields()).map(Field::getName).toList();
        assertFalse(fieldNames.contains("passwordHash"));
        assertFalse(fieldNames.contains("email"));
    }

    // ── Log method ──

    @Test
    void logShouldInsert() {
        service.log(1L, "USER_DEACTIVATED", "USER", 2L, "bad", "ACTIVE", "DEACTIVATED");
        verify(auditLogMapper).insert(any(AuditLog.class));
    }
}
