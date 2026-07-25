package com.itnoduck.acmate.problem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itnoduck.acmate.common.dto.PageResponse;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.problem.dto.ProblemDetailResponse;
import com.itnoduck.acmate.problem.dto.ProblemQueryRequest;
import com.itnoduck.acmate.problem.dto.ProblemSummaryResponse;
import com.itnoduck.acmate.problem.entity.Problem;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import com.itnoduck.acmate.problem.service.ProblemQueryService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 题目查询服务实现。
 *
 * <p>所有查询均强制过滤 status=1，避免普通用户看到已禁用的题目。
 * 关键词条件使用括号组合，防止 OR 绕过 status 过滤。</p>
 */
@Service
public class ProblemQueryServiceImpl implements ProblemQueryService {

    private final ProblemMapper problemMapper;

    public ProblemQueryServiceImpl(ProblemMapper problemMapper) {
        this.problemMapper = problemMapper;
    }

    @Override
    public ProblemDetailResponse getProblem(long id) {
        if (id <= 0) {
            throw new BusinessException(404, "题目不存在");
        }
        Problem problem = problemMapper.selectOne(
                new LambdaQueryWrapper<Problem>()
                        .eq(Problem::getId, id)
                        .eq(Problem::getStatus, 1));
        if (problem == null) {
            throw new BusinessException(404, "题目不存在");
        }
        return toDetailResponse(problem);
    }

    @Override
    public PageResponse<ProblemSummaryResponse> listProblems(ProblemQueryRequest request) {
        long page = Math.max(request.getPage(), 1);
        long size = Math.max(1, Math.min(request.getSize(), 100));

        // 基础条件：只返回正常状态的题目
        LambdaQueryWrapper<Problem> wrapper = new LambdaQueryWrapper<Problem>()
                .eq(Problem::getStatus, 1);

        if (request.getPlatform() != null) {
            wrapper.eq(Problem::getPlatform, request.getPlatform());
        }
        if (request.getDifficulty() != null) {
            wrapper.eq(Problem::getDifficulty, request.getDifficulty());
        }
        // 关键词匹配 title 或 externalProblemKey，括号组合避免 OR 绕过 status=1
        if (request.getKeyword() != null) {
            wrapper.and(w -> w
                    .like(Problem::getTitle, request.getKeyword())
                    .or()
                    .like(Problem::getExternalProblemKey, request.getKeyword()));
        }

        wrapper.orderByDesc(Problem::getCreateTime, Problem::getId);

        Page<Problem> mpPage = new Page<>(page, size);
        Page<Problem> result = problemMapper.selectPage(mpPage, wrapper);

        List<ProblemSummaryResponse> records = result.getRecords().stream()
                .map(this::toSummaryResponse)
                .toList();

        return new PageResponse<>(
                result.getCurrent(),
                result.getSize(),
                result.getTotal(),
                result.getPages(),
                records);
    }

    private ProblemDetailResponse toDetailResponse(Problem p) {
        return new ProblemDetailResponse(
                p.getId(), p.getPlatform(), p.getExternalProblemKey(), p.getTitle(),
                p.getSourceUrl(), p.getDifficulty(), p.getTags(), p.getContentMd(),
                p.getCreatorUserId(), p.getCreateTime(), p.getUpdateTime());
    }

    private ProblemSummaryResponse toSummaryResponse(Problem p) {
        return new ProblemSummaryResponse(
                p.getId(), p.getPlatform(), p.getExternalProblemKey(), p.getTitle(),
                p.getSourceUrl(), p.getDifficulty(), p.getTags(),
                p.getCreatorUserId(), p.getCreateTime());
    }
}
