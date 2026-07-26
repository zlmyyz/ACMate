package com.itnoduck.acmate.problem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.problem.dto.CreateProblemRequest;
import com.itnoduck.acmate.problem.dto.ProblemDetailResponse;
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
}
