package com.itnoduck.acmate.problem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.problem.dto.CreateProblemRequest;
import com.itnoduck.acmate.problem.dto.ProblemDetailResponse;
import com.itnoduck.acmate.problem.entity.Problem;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import com.itnoduck.acmate.problem.service.ProblemCommandService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 题目命令服务实现。
 *
 * <p>creatorUserId 必须来自服务端当前认证用户，不能由请求体指定——
 * 这是信任边界：客户端不应决定题目的创建者。</p>
 *
 * <p>status 由服务端固定为 1（正常），不在请求中暴露。</p>
 */
@Service
public class ProblemCommandServiceImpl implements ProblemCommandService {

    private final ProblemMapper problemMapper;

    public ProblemCommandServiceImpl(ProblemMapper problemMapper) {
        this.problemMapper = problemMapper;
    }

    @Override
    public ProblemDetailResponse createProblem(CreateProblemRequest request, long creatorUserId) {
        if (creatorUserId <= 0) {
            throw new BusinessException(400, "创建者 ID 无效");
        }
        // 非 CUSTOM 平台必须提供外部题目标识
        if (!"CUSTOM".equals(request.getPlatform()) && request.getExternalProblemKey() == null) {
            throw new BusinessException(400, "非自定义平台必须提供外部题目标识");
        }

        // 前置查重：相同平台+题目标识（仅 externalProblemKey 非空时检查）
        if (request.getExternalProblemKey() != null) {
            long count = problemMapper.selectCount(
                    new LambdaQueryWrapper<Problem>()
                            .eq(Problem::getPlatform, request.getPlatform())
                            .eq(Problem::getExternalProblemKey, request.getExternalProblemKey()));
            if (count > 0) {
                throw new BusinessException(409, "该平台题目标识已存在");
            }
        }

        Problem problem = new Problem();
        problem.setPlatform(request.getPlatform());
        problem.setExternalProblemKey(request.getExternalProblemKey());
        problem.setTitle(request.getTitle());
        problem.setSourceUrl(request.getSourceUrl());
        problem.setDifficulty(request.getDifficulty());
        problem.setTags(request.getTags());
        problem.setContentMd(request.getContentMd());
        // creatorUserId 由服务端指定，客户端不可控
        problem.setCreatorUserId(creatorUserId);
        // status 由服务端固定为正常
        problem.setStatus(1);

        int rows;
        try {
            rows = problemMapper.insert(problem);
        } catch (DuplicateKeyException e) {
            // 唯一索引 uk_platform_problem 兜底：并发情况下前置查重可能漏过
            throw new BusinessException(409, "该平台题目标识已存在");
        }

        if (rows != 1) {
            throw new RuntimeException("创建题目失败：插入行数异常");
        }
        if (problem.getId() == null) {
            throw new RuntimeException("创建题目失败：ID 未回填");
        }

        return toDetailResponse(problem);
    }

    private ProblemDetailResponse toDetailResponse(Problem p) {
        return new ProblemDetailResponse(
                p.getId(), p.getPlatform(), p.getExternalProblemKey(), p.getTitle(),
                p.getSourceUrl(), p.getDifficulty(), p.getTags(), p.getContentMd(),
                p.getCreatorUserId(), p.getCreateTime(), p.getUpdateTime());
    }
}
