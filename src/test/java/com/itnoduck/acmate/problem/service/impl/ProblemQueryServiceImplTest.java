package com.itnoduck.acmate.problem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itnoduck.acmate.common.dto.PageResponse;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.problem.dto.MineProblemStatusFilter;
import com.itnoduck.acmate.problem.dto.MyProblemSummaryResponse;
import com.itnoduck.acmate.problem.dto.ProblemDetailResponse;
import com.itnoduck.acmate.problem.dto.ProblemQueryRequest;
import com.itnoduck.acmate.problem.dto.ProblemSummaryResponse;
import com.itnoduck.acmate.problem.entity.Problem;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProblemQueryServiceImplTest {

    @Mock
    private ProblemMapper problemMapper;

    @InjectMocks
    private ProblemQueryServiceImpl service;

    private Problem buildProblem(Long id, String title, int status, Long creatorUserId) {
        Problem p = new Problem();
        p.setId(id);
        p.setPlatform("CUSTOM");
        p.setExternalProblemKey("EXT-" + id);
        p.setTitle(title);
        p.setSourceUrl("https://example.com/" + id);
        p.setDifficulty("800");
        p.setTags("dp,greedy");
        p.setContentMd("## Content for " + id);
        p.setCreatorUserId(creatorUserId);
        p.setStatus(status);
        p.setCreateTime(LocalDateTime.of(2026, 7, 20, 12, 0, 0).plusDays(id));
        p.setUpdateTime(LocalDateTime.of(2026, 7, 21, 12, 0, 0).plusDays(id));
        return p;
    }

    private Problem buildProblem(Long id, String title, int status) {
        return buildProblem(id, title, status, 1L);
    }

    // --- getProblem ---

    @Test
    void shouldReturnProblemById() {
        Problem problem = buildProblem(1L, "Two Sum", 1);
        when(problemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(problem);

        ProblemDetailResponse result = service.getProblem(1L, 3L, false);

        assertEquals(1L, result.id());
        assertEquals("Two Sum", result.title());
        assertEquals("CUSTOM", result.platform());
        assertEquals("dp,greedy", result.tags());
        assertEquals("## Content for 1", result.contentMd());
    }

    @Test
    void shouldThrow404WhenProblemNotFound() {
        when(problemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getProblem(999L, 3L, false));
        assertEquals(404, ex.getCode());
        assertEquals("题目不存在", ex.getMessage());
    }

    @Test
    void shouldThrow404WhenIdIsZeroOrNegative() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getProblem(0L, 3L, false));
        assertEquals(404, ex.getCode());
    }

    @Test
    void shouldReturn404ForDisabledProblemWhenViewerIsNotOwnerNorAdmin() {
        Problem problem = buildProblem(1L, "Two Sum", 0, 1L);
        when(problemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(problem);

        // viewer is user 3, creator is user 1, not admin → 404
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getProblem(1L, 3L, false));
        assertEquals(404, ex.getCode());
    }

    @Test
    void shouldAllowCreatorToViewOwnDisabledProblem() {
        Problem problem = buildProblem(1L, "Two Sum", 0, 1L);
        when(problemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(problem);

        // viewer == creator → visible
        ProblemDetailResponse result = service.getProblem(1L, 1L, false);
        assertEquals(1L, result.id());
        assertEquals("Two Sum", result.title());
    }

    @Test
    void shouldAllowAdminToViewDisabledProblem() {
        Problem problem = buildProblem(1L, "Two Sum", 0, 3L);
        when(problemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(problem);

        // admin, not creator → visible
        ProblemDetailResponse result = service.getProblem(1L, 1L, true);
        assertEquals(1L, result.id());
    }

    // --- listProblems ---

    @Test
    void shouldFilterByStatus1InListQuery() {
        when(problemMapper.selectPage(any(), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<>());

        ProblemQueryRequest request = new ProblemQueryRequest();
        service.listProblems(request);

        ArgumentCaptor<LambdaQueryWrapper<Problem>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(problemMapper).selectPage(any(), captor.capture());
        String sql = captor.getValue().getTargetSql();
        assertTrue(sql.contains("status"), "Should contain status filter");
    }

    @Test
    void shouldFilterByPlatform() {
        when(problemMapper.selectPage(any(), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<>());

        ProblemQueryRequest request = new ProblemQueryRequest();
        request.setPlatform("CODEFORCES");
        service.listProblems(request);

        ArgumentCaptor<LambdaQueryWrapper<Problem>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(problemMapper).selectPage(any(), captor.capture());
        String sql = captor.getValue().getTargetSql();
        assertTrue(sql.contains("platform"));
    }

    @Test
    void shouldFilterByDifficulty() {
        when(problemMapper.selectPage(any(), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<>());

        ProblemQueryRequest request = new ProblemQueryRequest();
        request.setDifficulty("1200");
        service.listProblems(request);

        ArgumentCaptor<LambdaQueryWrapper<Problem>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(problemMapper).selectPage(any(), captor.capture());
        String sql = captor.getValue().getTargetSql();
        assertTrue(sql.contains("difficulty"));
    }

    @Test
    void shouldUseBracketedOrForKeyword() {
        when(problemMapper.selectPage(any(), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<>());

        ProblemQueryRequest request = new ProblemQueryRequest();
        request.setKeyword("two");
        service.listProblems(request);

        ArgumentCaptor<LambdaQueryWrapper<Problem>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(problemMapper).selectPage(any(), captor.capture());
        String sql = captor.getValue().getTargetSql();
        assertTrue(sql.contains("LIKE"), "Should contain LIKE for keyword");
        assertTrue(sql.contains("title") || sql.contains("external_problem_key"), "Should search title or external key");
    }

    @Test
    void shouldConvertEntityToSummaryResponse() {
        Problem problem = buildProblem(1L, "Two Sum", 1);
        when(problemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(problem);

        ProblemDetailResponse result = service.getProblem(1L, 1L, false);

        assertEquals(1L, result.id());
        assertEquals("CUSTOM", result.platform());
        assertEquals("EXT-1", result.externalProblemKey());
        assertEquals("Two Sum", result.title());
        assertEquals("https://example.com/1", result.sourceUrl());
        assertEquals("800", result.difficulty());
        assertEquals("dp,greedy", result.tags());
        assertEquals("## Content for 1", result.contentMd());
        assertEquals(1L, result.creatorUserId());
        assertNotNull(result.createTime());
        assertNotNull(result.updateTime());
    }

    // --- listMyProblems ---

    @Test
    void shouldReturnAllStatusesWithFilterAll() {
        Problem active = buildProblem(1L, "Active", 1, 5L);
        Problem inactive = buildProblem(2L, "Inactive", 0, 5L);
        Page<Problem> mpPage = new Page<>(1, 20);
        mpPage.setRecords(List.of(active, inactive));
        mpPage.setTotal(2);
        when(problemMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(mpPage);

        PageResponse<MyProblemSummaryResponse> result = service.listMyProblems(
                new ProblemQueryRequest(), MineProblemStatusFilter.ALL, 5L);

        assertEquals(2, result.total());
        assertEquals(2, result.records().size());
        assertTrue(result.records().stream().anyMatch(r -> r.status().name().equals("ACTIVE")));
        assertTrue(result.records().stream().anyMatch(r -> r.status().name().equals("INACTIVE")));
    }

    @Test
    void shouldFilterOnlyActiveWithFilterActive() {
        Problem problem = buildProblem(1L, "Active", 1, 5L);
        Page<Problem> mpPage = new Page<>(1, 20);
        mpPage.setRecords(List.of(problem));
        mpPage.setTotal(1);
        when(problemMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(mpPage);

        PageResponse<MyProblemSummaryResponse> result = service.listMyProblems(
                new ProblemQueryRequest(), MineProblemStatusFilter.ACTIVE, 5L);

        assertEquals(1, result.total());
        assertEquals("ACTIVE", result.records().get(0).status().name());

        ArgumentCaptor<LambdaQueryWrapper<Problem>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(problemMapper).selectPage(any(), captor.capture());
        String sql = captor.getValue().getTargetSql();
        assertTrue(sql.contains("status"));
        // Should have creator_user_id condition
        assertTrue(sql.contains("creator_user_id"));
    }

    @Test
    void shouldFilterOnlyInactiveWithFilterInactive() {
        Problem problem = buildProblem(1L, "Inactive", 0, 5L);
        Page<Problem> mpPage = new Page<>(1, 20);
        mpPage.setRecords(List.of(problem));
        mpPage.setTotal(1);
        when(problemMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(mpPage);

        PageResponse<MyProblemSummaryResponse> result = service.listMyProblems(
                new ProblemQueryRequest(), MineProblemStatusFilter.INACTIVE, 5L);

        assertEquals(1, result.total());
        assertEquals("INACTIVE", result.records().get(0).status().name());
    }

    @Test
    void listMyProblemsShouldAlwaysFilterByCreatorUserId() {
        Page<Problem> mpPage = new Page<>(1, 20);
        mpPage.setRecords(List.of());
        mpPage.setTotal(0);
        when(problemMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(mpPage);

        service.listMyProblems(new ProblemQueryRequest(), MineProblemStatusFilter.ALL, 5L);

        ArgumentCaptor<LambdaQueryWrapper<Problem>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(problemMapper).selectPage(any(), captor.capture());
        String sql = captor.getValue().getTargetSql();
        assertTrue(sql.contains("creator_user_id"), "Should contain creator_user_id filter");
    }

    @Test
    void listMyProblemsShouldNotReturnOtherUsersProblems() {
        // 当前用户 5L 创建了 id=1，用户 3L 创建了 id=2
        // Service 固定过滤 creator_user_id = 5L，因此只会返回 id=1
        Problem myProblem = buildProblem(1L, "My Problem", 1, 5L);
        Page<Problem> mpPage = new Page<>(1, 20);
        mpPage.setRecords(List.of(myProblem));
        mpPage.setTotal(1);
        when(problemMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(mpPage);

        PageResponse<MyProblemSummaryResponse> result = service.listMyProblems(
                new ProblemQueryRequest(), MineProblemStatusFilter.ALL, 5L);

        assertEquals(1, result.total());
        assertEquals("My Problem", result.records().get(0).title());
    }

    @Test
    void listMyProblemsShouldThrow400WhenUserIdInvalid() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.listMyProblems(new ProblemQueryRequest(), MineProblemStatusFilter.ALL, 0L));
        assertEquals(400, ex.getCode());
    }

    @Test
    void publicListShouldStillOnlyReturnStatus1() {
        Page<Problem> mpPage = new Page<>(1, 20);
        mpPage.setRecords(List.of());
        mpPage.setTotal(0);
        when(problemMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(mpPage);

        service.listProblems(new ProblemQueryRequest());

        ArgumentCaptor<LambdaQueryWrapper<Problem>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(problemMapper).selectPage(any(), captor.capture());
        String sql = captor.getValue().getTargetSql();
        assertTrue(sql.contains("status"), "Public list should still filter status=1");
    }
}
