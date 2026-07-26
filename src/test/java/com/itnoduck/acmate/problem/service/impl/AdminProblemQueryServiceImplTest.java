package com.itnoduck.acmate.problem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itnoduck.acmate.common.dto.PageResponse;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.problem.dto.AdminProblemSummaryResponse;
import com.itnoduck.acmate.problem.dto.MineProblemStatusFilter;
import com.itnoduck.acmate.problem.dto.ProblemQueryRequest;
import com.itnoduck.acmate.problem.entity.Problem;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
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
class AdminProblemQueryServiceImplTest {

    @Mock
    private ProblemMapper problemMapper;

    @Mock
    private AppUserMapper appUserMapper;

    @InjectMocks
    private AdminProblemQueryServiceImpl service;

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

    private AppUser buildAppUser(Long id, String username, String nickname) {
        AppUser u = new AppUser();
        u.setId(id);
        u.setUsername(username);
        u.setNickname(nickname);
        return u;
    }

    @Test
    void shouldReturnAllStatusesWithFilterAll() {
        Problem active = buildProblem(1L, "Active", 1, 10L);
        Problem inactive = buildProblem(2L, "Inactive", 0, 20L);
        Page<Problem> mpPage = new Page<>(1, 20);
        mpPage.setRecords(List.of(active, inactive));
        mpPage.setTotal(2);
        when(problemMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(mpPage);
        when(appUserMapper.selectBatchIds(any())).thenReturn(List.of(
                buildAppUser(10L, "user1", "User One"),
                buildAppUser(20L, "user2", "User Two")));

        PageResponse<AdminProblemSummaryResponse> result = service.listProblems(
                new ProblemQueryRequest(), MineProblemStatusFilter.ALL);

        assertEquals(2, result.total());
        assertTrue(result.records().stream().anyMatch(r -> r.status().name().equals("ACTIVE")));
        assertTrue(result.records().stream().anyMatch(r -> r.status().name().equals("INACTIVE")));
    }

    @Test
    void shouldFilterOnlyActive() {
        Problem problem = buildProblem(1L, "Active", 1, 10L);
        Page<Problem> mpPage = new Page<>(1, 20);
        mpPage.setRecords(List.of(problem));
        mpPage.setTotal(1);
        when(problemMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(mpPage);
        when(appUserMapper.selectBatchIds(any())).thenReturn(List.of(buildAppUser(10L, "user1", "User One")));

        PageResponse<AdminProblemSummaryResponse> result = service.listProblems(
                new ProblemQueryRequest(), MineProblemStatusFilter.ACTIVE);

        assertEquals(1, result.total());
        assertEquals("ACTIVE", result.records().get(0).status().name());

        ArgumentCaptor<LambdaQueryWrapper<Problem>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(problemMapper).selectPage(any(), captor.capture());
        assertTrue(captor.getValue().getTargetSql().contains("status"));
    }

    @Test
    void shouldFilterOnlyInactive() {
        Problem problem = buildProblem(1L, "Inactive", 0, 10L);
        Page<Problem> mpPage = new Page<>(1, 20);
        mpPage.setRecords(List.of(problem));
        mpPage.setTotal(1);
        when(problemMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(mpPage);
        when(appUserMapper.selectBatchIds(any())).thenReturn(List.of(buildAppUser(10L, "user1", "User One")));

        PageResponse<AdminProblemSummaryResponse> result = service.listProblems(
                new ProblemQueryRequest(), MineProblemStatusFilter.INACTIVE);

        assertEquals("INACTIVE", result.records().get(0).status().name());
    }

    @Test
    void shouldFilterByCreatorUserId() {
        ProblemQueryRequest request = new ProblemQueryRequest();
        request.setCreatorUserId(42L);
        Page<Problem> mpPage = new Page<>(1, 20);
        mpPage.setRecords(List.of());
        mpPage.setTotal(0);
        when(problemMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(mpPage);

        service.listProblems(request, MineProblemStatusFilter.ALL);

        ArgumentCaptor<LambdaQueryWrapper<Problem>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(problemMapper).selectPage(any(), captor.capture());
        assertTrue(captor.getValue().getTargetSql().contains("creator_user_id"));
    }

    @Test
    void shouldReturn400WhenCreatorUserIdInvalid() {
        ProblemQueryRequest request = new ProblemQueryRequest();
        request.setCreatorUserId(0L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.listProblems(request, MineProblemStatusFilter.ALL));
        assertEquals(400, ex.getCode());
    }

    @Test
    void shouldIncludeCreatorInfoInResponse() {
        Problem problem = buildProblem(1L, "Test", 1, 10L);
        Page<Problem> mpPage = new Page<>(1, 20);
        mpPage.setRecords(List.of(problem));
        mpPage.setTotal(1);
        when(problemMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(mpPage);
        when(appUserMapper.selectBatchIds(any())).thenReturn(List.of(
                buildAppUser(10L, "testuser", "Test User")));

        PageResponse<AdminProblemSummaryResponse> result = service.listProblems(
                new ProblemQueryRequest(), MineProblemStatusFilter.ALL);

        AdminProblemSummaryResponse record = result.records().get(0);
        assertEquals(10L, record.creatorUserId());
        assertEquals("testuser", record.creatorUsername());
        assertEquals("Test User", record.creatorNickname());
    }

    @Test
    void shouldReturnNullCreatorInfoWhenUserMissing() {
        Problem problem = buildProblem(1L, "Test", 1, 999L);
        Page<Problem> mpPage = new Page<>(1, 20);
        mpPage.setRecords(List.of(problem));
        mpPage.setTotal(1);
        when(problemMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(mpPage);
        when(appUserMapper.selectBatchIds(any())).thenReturn(List.of());

        // 创建者不存在时题目仍应返回，username/nickname 为 null
        PageResponse<AdminProblemSummaryResponse> result = service.listProblems(
                new ProblemQueryRequest(), MineProblemStatusFilter.ALL);

        AdminProblemSummaryResponse record = result.records().get(0);
        assertEquals(1L, record.id());
        assertEquals(999L, record.creatorUserId());
        assertNull(record.creatorUsername());
        assertNull(record.creatorNickname());
    }

    @Test
    void shouldBatchLoadUsersNotNPlus1() {
        Problem p1 = buildProblem(1L, "P1", 1, 10L);
        Problem p2 = buildProblem(2L, "P2", 1, 20L);
        Problem p3 = buildProblem(3L, "P3", 0, 10L);
        Page<Problem> mpPage = new Page<>(1, 20);
        mpPage.setRecords(List.of(p1, p2, p3));
        mpPage.setTotal(3);
        when(problemMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(mpPage);
        when(appUserMapper.selectBatchIds(any())).thenReturn(List.of(
                buildAppUser(10L, "u1", "U1"), buildAppUser(20L, "u2", "U2")));

        service.listProblems(new ProblemQueryRequest(), MineProblemStatusFilter.ALL);

        // selectBatchIds 应只被调用一次
        verify(appUserMapper).selectBatchIds(any());
    }

    @Test
    void shouldSupportKeywordWithBracketOr() {
        ProblemQueryRequest request = new ProblemQueryRequest();
        request.setKeyword("test");
        Page<Problem> mpPage = new Page<>(1, 20);
        mpPage.setRecords(List.of());
        mpPage.setTotal(0);
        when(problemMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(mpPage);

        service.listProblems(request, MineProblemStatusFilter.ALL);

        ArgumentCaptor<LambdaQueryWrapper<Problem>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(problemMapper).selectPage(any(), captor.capture());
        String sql = captor.getValue().getTargetSql();
        assertTrue(sql.contains("LIKE"));
        assertTrue(sql.contains("title") || sql.contains("external_problem_key"));
    }
}
