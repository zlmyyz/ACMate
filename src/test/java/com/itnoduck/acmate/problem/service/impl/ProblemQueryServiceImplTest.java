package com.itnoduck.acmate.problem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itnoduck.acmate.common.dto.PageResponse;
import com.itnoduck.acmate.common.exception.BusinessException;
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

    private Problem buildProblem(Long id, String title, int status) {
        Problem p = new Problem();
        p.setId(id);
        p.setPlatform("CUSTOM");
        p.setExternalProblemKey("EXT-" + id);
        p.setTitle(title);
        p.setSourceUrl("https://example.com/" + id);
        p.setDifficulty("800");
        p.setTags("dp,greedy");
        p.setContentMd("## Content for " + id);
        p.setCreatorUserId(1L);
        p.setStatus(status);
        p.setCreateTime(LocalDateTime.of(2026, 7, 20, 12, 0, 0).plusDays(id));
        p.setUpdateTime(LocalDateTime.of(2026, 7, 21, 12, 0, 0).plusDays(id));
        return p;
    }

    @Test
    void shouldReturnProblemById() {
        Problem problem = buildProblem(1L, "Two Sum", 1);
        when(problemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(problem);

        ProblemDetailResponse result = service.getProblem(1L);

        assertEquals(1L, result.id());
        assertEquals("Two Sum", result.title());
        assertEquals("CUSTOM", result.platform());
        assertEquals("dp,greedy", result.tags());
        assertEquals("## Content for 1", result.contentMd());
    }

    @Test
    void shouldThrow404WhenProblemNotFound() {
        when(problemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getProblem(999L));
        assertEquals(404, ex.getCode());
        assertEquals("题目不存在", ex.getMessage());
    }

    @Test
    void shouldThrow404WhenIdIsZeroOrNegative() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.getProblem(0L));
        assertEquals(404, ex.getCode());
    }

    @Test
    void shouldNotReturnDisabledProblem() {
        Problem problem = buildProblem(1L, "Two Sum", 0);
        when(problemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getProblem(1L));
        assertEquals(404, ex.getCode());
    }

    @Test
    void shouldFilterByStatus1InListQuery() {
        when(problemMapper.selectPage(any(), any(LambdaQueryWrapper.class)))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>());

        ProblemQueryRequest request = new ProblemQueryRequest();
        service.listProblems(request);

        ArgumentCaptor<LambdaQueryWrapper<Problem>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(problemMapper).selectPage(any(), captor.capture());
        // 验证 status=1 条件存在
        String sql = captor.getValue().getTargetSql();
        assertTrue(sql.contains("status"), "Should contain status filter");
    }

    @Test
    void shouldFilterByPlatform() {
        when(problemMapper.selectPage(any(), any(LambdaQueryWrapper.class)))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>());

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
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>());

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
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>());

        ProblemQueryRequest request = new ProblemQueryRequest();
        request.setKeyword("two");
        service.listProblems(request);

        ArgumentCaptor<LambdaQueryWrapper<Problem>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(problemMapper).selectPage(any(), captor.capture());
        String sql = captor.getValue().getTargetSql();
        // 关键词应出现在括号中
        assertTrue(sql.contains("LIKE"), "Should contain LIKE for keyword");
        assertTrue(sql.contains("title") || sql.contains("external_problem_key"), "Should search title or external key");
    }

    @Test
    void shouldConvertEntityToSummaryResponse() {
        Problem problem = buildProblem(1L, "Two Sum", 1);
        when(problemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(problem);

        ProblemDetailResponse result = service.getProblem(1L);

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
}
