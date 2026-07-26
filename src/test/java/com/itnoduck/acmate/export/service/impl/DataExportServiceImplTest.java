package com.itnoduck.acmate.export.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itnoduck.acmate.auditlog.service.AuditLogService;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.oj.mapper.OjSubmissionMapper;
import com.itnoduck.acmate.problem.entity.Problem;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import com.itnoduck.acmate.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataExportServiceImplTest {

    @Mock
    private ProblemMapper problemMapper;

    @Mock
    private OjSubmissionMapper submissionMapper;

    @Mock
    private AuditLogService auditLogService;

    private DataExportServiceImpl service;

    private AuthenticatedUser adminUser;

    @BeforeEach
    void setUp() {
        service = new DataExportServiceImpl(problemMapper, submissionMapper, auditLogService);
        adminUser = new AuthenticatedUser(1L, "admin", "hash", "Admin",
                null, null, null, true, true,
                List.of());
    }

    // --- Permission ---

    @Test
    void shouldRejectNonAdminForExportProblems() {
        var normalUser = new AuthenticatedUser(2L, "user", "hash", "User",
                null, null, null, false, true, List.of());
        var ex = assertThrows(BusinessException.class, () -> service.exportProblems(normalUser));
        assertEquals(403, ex.getCode());
    }

    // --- CSV formula injection: exportProblems ---

    @Test
    void shouldEscapeFormulaPrefixEquals() {
        var problem = buildProblem("=SUM(A1:A2)");
        when(problemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(problem));
        String csv = service.exportProblems(adminUser);
        assertTrue(csv.contains("'=SUM(A1:A2)"));
    }

    @Test
    void shouldEscapeFormulaPrefixPlus() {
        var problem = buildProblem("+1+1");
        when(problemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(problem));
        String csv = service.exportProblems(adminUser);
        assertTrue(csv.contains("'+1+1"));
    }

    @Test
    void shouldEscapeFormulaPrefixMinus() {
        var problem = buildProblem("-CMD");
        when(problemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(problem));
        String csv = service.exportProblems(adminUser);
        assertTrue(csv.contains("'-CMD"));
    }

    @Test
    void shouldEscapeFormulaPrefixAt() {
        var problem = buildProblem("@IMPORT");
        when(problemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(problem));
        String csv = service.exportProblems(adminUser);
        assertTrue(csv.contains("'@IMPORT"));
    }

    @Test
    void shouldNotEscapeNormalChinese() {
        var problem = buildProblem("两数之和");
        when(problemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(problem));
        String csv = service.exportProblems(adminUser);
        assertTrue(csv.contains("两数之和"));
        assertFalse(csv.contains("'两数之和"));
    }

    @Test
    void shouldEscapeCommaInField() {
        var problem = buildProblem("a,b");
        when(problemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(problem));
        String csv = service.exportProblems(adminUser);
        assertTrue(csv.contains("\"a,b\""));
    }

    @Test
    void shouldEscapeDoubleQuoteInField() {
        var problem = buildProblem("a\"b");
        when(problemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(problem));
        String csv = service.exportProblems(adminUser);
        assertTrue(csv.contains("\"a\"\"b\""));
    }

    @Test
    void shouldEscapeNewlineInField() {
        var problem = buildProblem("a\nb");
        when(problemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(problem));
        String csv = service.exportProblems(adminUser);
        assertTrue(csv.contains("\"a\nb\""));
    }

    @Test
    void shouldReturnEmptyForNullTitle() {
        var problem = buildProblem(null);
        when(problemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(problem));
        String csv = service.exportProblems(adminUser);
        assertTrue(csv.contains(",,"));
    }

    // --- Leaderboard export ---

    @Test
    void shouldExportLeaderboardWithTotalPeriod() {
        when(submissionMapper.aggregateLeaderboardAll(eq(0L), eq(1000)))
                .thenReturn(List.of(Map.of("user_id", 1L, "solved_count", 5L)));
        String csv = service.exportLeaderboard("total", adminUser);
        assertTrue(csv.contains("Rank,UserId,SolvedCount"));
        assertTrue(csv.contains("1,1,5"));
    }

    @Test
    void shouldExportLeaderboardWith7dPeriod() {
        when(submissionMapper.aggregateLeaderboard(any(LocalDateTime.class), eq(0L), eq(1000)))
                .thenReturn(List.of(Map.of("user_id", 2L, "solved_count", 3L)));
        String csv = service.exportLeaderboard("7d", adminUser);
        assertTrue(csv.contains("1,2,3"));
    }

    @Test
    void shouldRejectNonAdminForExportLeaderboard() {
        var normalUser = new AuthenticatedUser(2L, "user", "hash", "User",
                null, null, null, false, true, List.of());
        var ex = assertThrows(BusinessException.class, () -> service.exportLeaderboard("total", normalUser));
        assertEquals(403, ex.getCode());
    }

    private Problem buildProblem(String title) {
        Problem p = new Problem();
        p.setId(1L);
        p.setPlatform("CUSTOM");
        p.setExternalProblemKey("KEY-1");
        p.setTitle(title);
        p.setSourceUrl("https://example.com");
        p.setDifficulty("800");
        p.setTags("dp");
        p.setCreatorUserId(1L);
        p.setStatus(1);
        p.setCreateTime(LocalDateTime.of(2026, 7, 27, 12, 0));
        return p;
    }
}
