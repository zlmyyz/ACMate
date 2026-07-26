package com.itnoduck.acmate.problem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.problem.dto.CreateProblemRequest;
import com.itnoduck.acmate.problem.dto.ProblemDetailResponse;
import com.itnoduck.acmate.problem.dto.UpdateProblemRequest;
import com.itnoduck.acmate.problem.entity.Problem;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProblemCommandServiceImplTest {

    @Mock
    private ProblemMapper problemMapper;

    @InjectMocks
    private ProblemCommandServiceImpl service;

    private CreateProblemRequest buildRequest(String platform, String externalKey) {
        CreateProblemRequest req = new CreateProblemRequest();
        req.setPlatform(platform);
        req.setExternalProblemKey(externalKey);
        req.setTitle("Test Problem");
        req.setSourceUrl("https://example.com");
        req.setDifficulty("800");
        req.setTags(" dp , array, ,hash-map, dp ");
        req.setContentMd("## Content");
        return req;
    }

    @Test
    void shouldCreateProblemSuccessfully() {
        CreateProblemRequest req = buildRequest("CUSTOM", null);
        when(problemMapper.insert(ArgumentMatchers.<Problem>any())).thenAnswer(inv -> {
            Problem p = inv.getArgument(0);
            p.setId(1L);
            return 1;
        });

        ProblemDetailResponse result = service.createProblem(req, 5L);

        assertEquals(1L, result.id());
        assertEquals("CUSTOM", result.platform());
        assertEquals("Test Problem", result.title());
        assertEquals(5L, result.creatorUserId());
    }

    @Test
    void shouldSetCreatorUserIdFromParameter() {
        CreateProblemRequest req = buildRequest("CUSTOM", null);
        ArgumentCaptor<Problem> captor = ArgumentCaptor.forClass(Problem.class);
        when(problemMapper.insert(captor.capture())).thenAnswer(inv -> {
            Problem p = inv.getArgument(0);
            p.setId(1L);
            return 1;
        });

        service.createProblem(req, 7L);

        assertEquals(7L, captor.getValue().getCreatorUserId());
    }

    @Test
    void shouldSetStatusToOne() {
        CreateProblemRequest req = buildRequest("CUSTOM", null);
        ArgumentCaptor<Problem> captor = ArgumentCaptor.forClass(Problem.class);
        when(problemMapper.insert(captor.capture())).thenAnswer(inv -> {
            Problem p = inv.getArgument(0);
            p.setId(1L);
            return 1;
        });

        service.createProblem(req, 1L);

        assertEquals(1, captor.getValue().getStatus());
    }

    @Test
    void shouldRequireExternalKeyForNonCustomPlatform() {
        CreateProblemRequest req = buildRequest("CODEFORCES", null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createProblem(req, 1L));
        assertEquals(400, ex.getCode());
        assertEquals("非自定义平台必须提供外部题目标识", ex.getMessage());
    }

    @Test
    void shouldAllowNullExternalKeyForCustomPlatform() {
        CreateProblemRequest req = buildRequest("CUSTOM", null);
        when(problemMapper.insert(ArgumentMatchers.<Problem>any())).thenAnswer(inv -> {
            Problem p = inv.getArgument(0);
            p.setId(1L);
            return 1;
        });

        ProblemDetailResponse result = service.createProblem(req, 1L);
        assertNotNull(result);
    }

    @Test
    void shouldReturn409WhenPreCheckFindsDuplicate() {
        CreateProblemRequest req = buildRequest("CODEFORCES", "123A");
        when(problemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createProblem(req, 1L));
        assertEquals(409, ex.getCode());
        assertEquals("该平台题目标识已存在", ex.getMessage());
        // 前置查重发现重复后不应执行 insert
        verify(problemMapper, never()).insert(ArgumentMatchers.<Problem>any());
    }

    @Test
    void shouldReturn409OnDuplicateKeyException() {
        CreateProblemRequest req = buildRequest("CODEFORCES", "123A");
        when(problemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(problemMapper.insert(ArgumentMatchers.<Problem>any())).thenThrow(new DuplicateKeyException("dup"));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createProblem(req, 1L));
        assertEquals(409, ex.getCode());
        assertEquals("该平台题目标识已存在", ex.getMessage());
    }

    @Test
    void shouldReturn500WhenInsertReturnsZero() {
        CreateProblemRequest req = buildRequest("CUSTOM", null);
        when(problemMapper.insert(ArgumentMatchers.<Problem>any())).thenReturn(0);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.createProblem(req, 1L));
        assertTrue(ex.getMessage().contains("插入行数异常"));
    }

    @Test
    void shouldReturn500WhenIdNotBackfilled() {
        CreateProblemRequest req = buildRequest("CUSTOM", null);
        when(problemMapper.insert(ArgumentMatchers.<Problem>any())).thenReturn(1);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.createProblem(req, 1L));
        assertTrue(ex.getMessage().contains("ID 未回填"));
    }

    @Test
    void shouldNormalizeTags() {
        CreateProblemRequest req = new CreateProblemRequest();
        req.setPlatform("CUSTOM");
        req.setTitle("Test");
        req.setTags(" array , ,hash-map, array,dp , ");

        ArgumentCaptor<Problem> captor = ArgumentCaptor.forClass(Problem.class);
        when(problemMapper.insert(captor.capture())).thenAnswer(inv -> {
            Problem p = inv.getArgument(0);
            p.setId(1L);
            return 1;
        });

        service.createProblem(req, 1L);

        assertEquals("array,hash-map,dp", captor.getValue().getTags());
    }

    @Test
    void shouldConvertBlankFieldsToNull() {
        CreateProblemRequest req = buildRequest("CUSTOM", null);
        req.setSourceUrl("   ");
        req.setDifficulty("");

        ArgumentCaptor<Problem> captor = ArgumentCaptor.forClass(Problem.class);
        when(problemMapper.insert(captor.capture())).thenAnswer(inv -> {
            Problem p = inv.getArgument(0);
            p.setId(1L);
            return 1;
        });

        service.createProblem(req, 1L);

        assertNull(captor.getValue().getSourceUrl());
        assertNull(captor.getValue().getDifficulty());
    }

    @Test
    void shouldConvertEntityToDetailResponse() {
        CreateProblemRequest req = buildRequest("CUSTOM", null);
        when(problemMapper.insert(ArgumentMatchers.<Problem>any())).thenAnswer(inv -> {
            Problem p = inv.getArgument(0);
            p.setId(10L);
            return 1;
        });

        ProblemDetailResponse result = service.createProblem(req, 3L);

        assertEquals(10L, result.id());
        assertEquals("CUSTOM", result.platform());
        assertEquals("dp,array,hash-map", result.tags());
        assertEquals("## Content", result.contentMd());
        assertEquals(3L, result.creatorUserId());
    }

    // --- updateProblem ---

    private UpdateProblemRequest buildUpdateRequest(String platform, String externalKey) {
        UpdateProblemRequest req = new UpdateProblemRequest();
        req.setPlatform(platform);
        req.setExternalProblemKey(externalKey);
        req.setTitle("Updated Problem");
        req.setSourceUrl("https://updated.com");
        req.setDifficulty("900");
        req.setTags("dp,greedy");
        req.setContentMd("## Updated");
        return req;
    }

    private Problem buildExistingProblem(long id, long creatorUserId, int status) {
        Problem p = new Problem();
        p.setId(id);
        p.setPlatform("CUSTOM");
        p.setExternalProblemKey("EXT-1");
        p.setTitle("Original");
        p.setSourceUrl("https://orig.com");
        p.setDifficulty("800");
        p.setTags("old");
        p.setContentMd("## Original");
        p.setCreatorUserId(creatorUserId);
        p.setStatus(status);
        p.setCreateTime(LocalDateTime.of(2026, 7, 20, 12, 0));
        p.setUpdateTime(LocalDateTime.of(2026, 7, 21, 12, 0));
        return p;
    }

    // 辅助：设置 selectOne 链式返回（初始查询 → 回读）
    private void mockSelectOneChain(Problem initial, Problem reRead) {
        when(problemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(initial, reRead);
    }

    @Test
    void shouldAllowOwnerToUpdate() {
        Problem existing = buildExistingProblem(1L, 5L, 1);
        UpdateProblemRequest req = buildUpdateRequest("CUSTOM", "EXT-1");
        Problem updated = buildExistingProblem(1L, 5L, 1);
        updated.setTitle("Updated Problem");
        updated.setDifficulty("900");
        mockSelectOneChain(existing, updated);
        when(problemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(problemMapper.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(1);

        ProblemDetailResponse result = service.updateProblem(1L, req, 5L, false);

        assertEquals("Updated Problem", result.title());
    }

    @Test
    void shouldAllowAdminToUpdateOtherUserProblem() {
        Problem existing = buildExistingProblem(1L, 999L, 1);
        UpdateProblemRequest req = buildUpdateRequest("CUSTOM", "EXT-1");
        Problem updated = buildExistingProblem(1L, 999L, 1);
        updated.setTitle("Updated Problem");
        mockSelectOneChain(existing, updated);
        when(problemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(problemMapper.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(1);

        ProblemDetailResponse result = service.updateProblem(1L, req, 1L, true);

        assertNotNull(result);
        assertEquals("Updated Problem", result.title());
    }

    @Test
    void shouldReturn403WhenNonOwnerNonAdmin() {
        Problem existing = buildExistingProblem(1L, 999L, 1);
        UpdateProblemRequest req = buildUpdateRequest("CUSTOM", "EXT-1");
        when(problemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateProblem(1L, req, 5L, false));
        assertEquals(403, ex.getCode());
        assertEquals("无权修改该题目", ex.getMessage());
        verify(problemMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void shouldReturn400WhenProblemIdInvalid() {
        UpdateProblemRequest req = buildUpdateRequest("CUSTOM", null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateProblem(0L, req, 1L, false));
        assertEquals(400, ex.getCode());
        assertEquals("题目 ID 无效", ex.getMessage());
    }

    @Test
    void shouldReturn400WhenOperatorIdInvalid() {
        UpdateProblemRequest req = buildUpdateRequest("CUSTOM", null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateProblem(1L, req, 0L, false));
        assertEquals(400, ex.getCode());
        assertEquals("操作者 ID 无效", ex.getMessage());
    }

    @Test
    void shouldReturn404WhenProblemNotFound() {
        UpdateProblemRequest req = buildUpdateRequest("CUSTOM", null);
        when(problemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateProblem(1L, req, 5L, true));
        assertEquals(404, ex.getCode());
    }

    @Test
    void shouldReturn404ForDisabledProblem() {
        UpdateProblemRequest req = buildUpdateRequest("CUSTOM", null);
        when(problemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateProblem(1L, req, 5L, true));
        assertEquals(404, ex.getCode());
    }

    @Test
    void shouldReturn400ForNonCustomWithoutExternalKey() {
        Problem existing = buildExistingProblem(1L, 5L, 1);
        existing.setPlatform("CODEFORCES");
        UpdateProblemRequest req = buildUpdateRequest("CODEFORCES", null);
        when(problemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateProblem(1L, req, 5L, true));
        assertEquals(400, ex.getCode());
        assertEquals("非自定义平台必须提供外部题目标识", ex.getMessage());
    }

    @Test
    void shouldAllowCustomPlatformWithNullExternalKey() {
        Problem existing = buildExistingProblem(1L, 5L, 1);
        UpdateProblemRequest req = buildUpdateRequest("CUSTOM", null);
        Problem updated = buildExistingProblem(1L, 5L, 1);
        updated.setExternalProblemKey(null);
        mockSelectOneChain(existing, updated);
        when(problemMapper.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(1);

        ProblemDetailResponse result = service.updateProblem(1L, req, 5L, false);
        assertNotNull(result);
    }

    @Test
    void shouldNotBlockSelfOnDuplicateCheck() {
        Problem existing = buildExistingProblem(1L, 5L, 1);
        UpdateProblemRequest req = buildUpdateRequest("CUSTOM", "EXT-1");
        Problem updated = buildExistingProblem(1L, 5L, 1);
        updated.setTitle("Updated Problem");
        mockSelectOneChain(existing, updated);
        when(problemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(problemMapper.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(1);

        ProblemDetailResponse result = service.updateProblem(1L, req, 5L, false);
        assertNotNull(result);
    }

    @Test
    void shouldReturn409WhenOtherProblemHasSameKey() {
        Problem existing = buildExistingProblem(1L, 5L, 1);
        UpdateProblemRequest req = buildUpdateRequest("CODEFORCES", "CONFLICT");
        when(problemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        when(problemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateProblem(1L, req, 5L, false));
        assertEquals(409, ex.getCode());
        assertEquals("该平台题目标识已存在", ex.getMessage());
        verify(problemMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void shouldReturn409OnUpdateDuplicateKeyException() {
        Problem existing = buildExistingProblem(1L, 5L, 1);
        UpdateProblemRequest req = buildUpdateRequest("CODEFORCES", "123A");
        when(problemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        when(problemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(problemMapper.update(eq(null), any(LambdaUpdateWrapper.class)))
                .thenThrow(new DuplicateKeyException("dup"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateProblem(1L, req, 5L, false));
        assertEquals(409, ex.getCode());
        assertEquals("该平台题目标识已存在", ex.getMessage());
    }

    @Test
    void shouldReturn404WhenUpdateReturnsZero() {
        Problem existing = buildExistingProblem(1L, 5L, 1);
        UpdateProblemRequest req = buildUpdateRequest("CUSTOM", null);
        when(problemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        when(problemMapper.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateProblem(1L, req, 5L, false));
        assertEquals(404, ex.getCode());
        assertEquals("题目不存在", ex.getMessage());
    }

    @Test
    void shouldThrowWhenUpdateReturnsGreaterThanOne() {
        Problem existing = buildExistingProblem(1L, 5L, 1);
        UpdateProblemRequest req = buildUpdateRequest("CUSTOM", null);
        when(problemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        when(problemMapper.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(2);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.updateProblem(1L, req, 5L, false));
        assertTrue(ex.getMessage().contains("影响行数超过预期"));
    }

    @Test
    void shouldPreserveCreatorUserIdAfterUpdate() {
        Problem existing = buildExistingProblem(1L, 5L, 1);
        UpdateProblemRequest req = buildUpdateRequest("CUSTOM", "EXT-1");
        Problem updated = buildExistingProblem(1L, 5L, 1);
        updated.setTitle("Updated Problem");
        updated.setCreatorUserId(5L);
        mockSelectOneChain(existing, updated);
        when(problemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(problemMapper.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(1);

        ProblemDetailResponse result = service.updateProblem(1L, req, 5L, false);
        assertEquals(5L, result.creatorUserId());
    }

    @Test
    void shouldPreserveStatusAfterUpdate() {
        Problem existing = buildExistingProblem(1L, 5L, 1);
        UpdateProblemRequest req = buildUpdateRequest("CUSTOM", "EXT-1");
        Problem updated = buildExistingProblem(1L, 5L, 1);
        updated.setTitle("Updated Problem");
        updated.setStatus(1);
        mockSelectOneChain(existing, updated);
        when(problemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(problemMapper.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(1);

        ProblemDetailResponse result = service.updateProblem(1L, req, 5L, false);
        assertEquals("Updated Problem", result.title());
    }

    @Test
    void shouldPreserveCreateTimeAfterUpdate() {
        LocalDateTime createTime = LocalDateTime.of(2026, 7, 20, 12, 0);
        Problem existing = buildExistingProblem(1L, 5L, 1);
        UpdateProblemRequest req = buildUpdateRequest("CUSTOM", "EXT-1");
        Problem updated = buildExistingProblem(1L, 5L, 1);
        updated.setTitle("Updated Problem");
        updated.setCreateTime(createTime);
        mockSelectOneChain(existing, updated);
        when(problemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(problemMapper.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(1);

        ProblemDetailResponse result = service.updateProblem(1L, req, 5L, false);
        assertEquals(createTime, result.createTime());
    }

    @Test
    void shouldUpdateTitle() {
        Problem existing = buildExistingProblem(1L, 5L, 1);
        UpdateProblemRequest req = buildUpdateRequest("CUSTOM", "EXT-1");
        req.setTitle("New Title");
        Problem updated = buildExistingProblem(1L, 5L, 1);
        updated.setTitle("New Title");
        mockSelectOneChain(existing, updated);
        when(problemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(problemMapper.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(1);

        ProblemDetailResponse result = service.updateProblem(1L, req, 5L, false);
        assertEquals("New Title", result.title());
    }

    @Test
    void shouldUpdateSourceUrlToNull() {
        Problem existing = buildExistingProblem(1L, 5L, 1);
        UpdateProblemRequest req = buildUpdateRequest("CUSTOM", "EXT-1");
        req.setSourceUrl("   ");
        Problem updated = buildExistingProblem(1L, 5L, 1);
        updated.setSourceUrl(null);
        mockSelectOneChain(existing, updated);
        when(problemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        ArgumentCaptor<LambdaUpdateWrapper<Problem>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        when(problemMapper.update(eq(null), captor.capture())).thenReturn(1);

        ProblemDetailResponse result = service.updateProblem(1L, req, 5L, false);
        assertNull(result.sourceUrl());

        // 验证 LambdaUpdateWrapper 的 SET 部分不包含 creatorUserId 和 status
        String sqlSet = captor.getValue().getSqlSet();
        assertNotNull(sqlSet);
        assertFalse(sqlSet.contains("creator_user_id"), "SET should not contain creator_user_id");
        assertFalse(sqlSet.contains("status"), "SET should not contain status");
    }

    @Test
    void shouldUpdateDifficultyToNull() {
        Problem existing = buildExistingProblem(1L, 5L, 1);
        UpdateProblemRequest req = buildUpdateRequest("CUSTOM", "EXT-1");
        req.setDifficulty("");
        Problem updated = buildExistingProblem(1L, 5L, 1);
        updated.setDifficulty(null);
        mockSelectOneChain(existing, updated);
        when(problemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(problemMapper.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(1);

        ProblemDetailResponse result = service.updateProblem(1L, req, 5L, false);
        assertNull(result.difficulty());
    }

    @Test
    void shouldUpdateTagsToNull() {
        Problem existing = buildExistingProblem(1L, 5L, 1);
        UpdateProblemRequest req = buildUpdateRequest("CUSTOM", "EXT-1");
        req.setTags("   ");
        Problem updated = buildExistingProblem(1L, 5L, 1);
        updated.setTags(null);
        mockSelectOneChain(existing, updated);
        when(problemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(problemMapper.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(1);

        ProblemDetailResponse result = service.updateProblem(1L, req, 5L, false);
        assertNull(result.tags());
    }

    @Test
    void shouldUpdateContentMdToNull() {
        Problem existing = buildExistingProblem(1L, 5L, 1);
        UpdateProblemRequest req = buildUpdateRequest("CUSTOM", "EXT-1");
        req.setContentMd(null);
        Problem updated = buildExistingProblem(1L, 5L, 1);
        updated.setContentMd(null);
        mockSelectOneChain(existing, updated);
        when(problemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(problemMapper.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(1);

        ProblemDetailResponse result = service.updateProblem(1L, req, 5L, false);
        assertNull(result.contentMd());
    }

    @Test
    void shouldNormalizeTagsOnUpdate() {
        Problem existing = buildExistingProblem(1L, 5L, 1);
        UpdateProblemRequest req = buildUpdateRequest("CUSTOM", "EXT-1");
        req.setTags(" dp , ,array, dp ,greedy ");
        Problem updated = buildExistingProblem(1L, 5L, 1);
        updated.setTags("dp,array,greedy");
        mockSelectOneChain(existing, updated);
        when(problemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(problemMapper.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(1);

        ProblemDetailResponse result = service.updateProblem(1L, req, 5L, false);
        assertEquals("dp,array,greedy", result.tags());
    }

    @Test
    void shouldReReadAndReturnLatestAfterUpdate() {
        Problem existing = buildExistingProblem(1L, 5L, 1);
        UpdateProblemRequest req = buildUpdateRequest("CUSTOM", "EXT-1");
        when(problemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(problemMapper.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(1);

        LocalDateTime dbUpdateTime = LocalDateTime.of(2026, 7, 26, 12, 0);
        Problem reRead = buildExistingProblem(1L, 5L, 1);
        reRead.setTitle("Updated Problem");
        reRead.setUpdateTime(dbUpdateTime);
        mockSelectOneChain(existing, reRead);

        ProblemDetailResponse result = service.updateProblem(1L, req, 5L, false);
        assertEquals("Updated Problem", result.title());
        assertEquals(dbUpdateTime, result.updateTime());
    }

    @Test
    void shouldReturn404WhenReReadFails() {
        Problem existing = buildExistingProblem(1L, 5L, 1);
        UpdateProblemRequest req = buildUpdateRequest("CUSTOM", "EXT-1");
        when(problemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(problemMapper.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(1);
        mockSelectOneChain(existing, null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateProblem(1L, req, 5L, false));
        assertEquals(404, ex.getCode());
    }

    @Test
    void updateProblemRequestShouldNotHaveCreatorUserIdField() throws Exception {
        assertThrows(NoSuchMethodException.class,
                () -> UpdateProblemRequest.class.getDeclaredMethod("setCreatorUserId", Long.class));
    }

    @Test
    void updateProblemRequestShouldNotHaveStatusField() throws Exception {
        assertThrows(NoSuchMethodException.class,
                () -> UpdateProblemRequest.class.getDeclaredMethod("setStatus", Integer.class));
    }

    @Test
    void updateWrapperWhereShouldIncludeStatusEq1() {
        Problem existing = buildExistingProblem(1L, 5L, 1);
        UpdateProblemRequest req = buildUpdateRequest("CUSTOM", "EXT-1");
        Problem updated = buildExistingProblem(1L, 5L, 1);
        updated.setTitle("Updated Problem");
        mockSelectOneChain(existing, updated);
        when(problemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        ArgumentCaptor<LambdaUpdateWrapper<Problem>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        when(problemMapper.update(eq(null), captor.capture())).thenReturn(1);

        service.updateProblem(1L, req, 5L, false);

        String sqlSegment = captor.getValue().getCustomSqlSegment();
        assertTrue(sqlSegment.contains("status"));
    }
}
