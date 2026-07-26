package com.itnoduck.acmate.problem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.problem.dto.CreateProblemRequest;
import com.itnoduck.acmate.problem.dto.ProblemDetailResponse;
import com.itnoduck.acmate.problem.dto.UpdateProblemRequest;
import com.itnoduck.acmate.problem.entity.Problem;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import com.itnoduck.acmate.problem.service.ProblemCommandService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 题目命令服务实现。
 *
 * <p>creatorUserId 必须来自服务端当前认证用户，不能由请求体指定——
 * 这是信任边界：客户端不应决定题目的创建者。</p>
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

    /**
     * 事务用于保证当前命令流程中"查询→校验→更新→回读"的读写边界一致。
     */
    @Override
    @Transactional
    public ProblemDetailResponse updateProblem(long problemId, UpdateProblemRequest request,
                                               long operatorUserId, boolean operatorAdmin) {
        if (problemId <= 0) {
            throw new BusinessException(400, "题目 ID 无效");
        }
        if (operatorUserId <= 0) {
            throw new BusinessException(400, "操作者 ID 无效");
        }

        // 查询目标题目：限制 status=1，已停用题目不可通过修改接口编辑
        // 不存在和已停用统一返回 404，避免通过写接口探测隐藏题目
        Problem existing = problemMapper.selectOne(
                new LambdaQueryWrapper<Problem>()
                        .eq(Problem::getId, problemId)
                        .eq(Problem::getStatus, 1));
        if (existing == null) {
            throw new BusinessException(404, "题目不存在");
        }

        // 资源所有权校验：创建者或管理员可以管理题目
        ensureCanManageProblem(existing, operatorUserId, operatorAdmin);

        // 非 CUSTOM 平台必须提供外部题目标识
        if (!"CUSTOM".equals(request.getPlatform()) && request.getExternalProblemKey() == null) {
            throw new BusinessException(400, "非自定义平台必须提供外部题目标识");
        }

        // 题目标识查重：排除当前 problemId，避免保持原值被自己误判为冲突
        // 前置查重用于提供明确的 409 错误；数据库唯一索引仍负责并发一致性
        if (request.getExternalProblemKey() != null) {
            long count = problemMapper.selectCount(
                    new LambdaQueryWrapper<Problem>()
                            .eq(Problem::getPlatform, request.getPlatform())
                            .eq(Problem::getExternalProblemKey, request.getExternalProblemKey())
                            .ne(Problem::getId, problemId));
            if (count > 0) {
                throw new BusinessException(409, "该平台题目标识已存在");
            }
        }

        // 使用 LambdaUpdateWrapper 显式设置允许修改的字段
        // 直接调用 .set() 可确保 null 值被生成到 SQL 中，不受全局字段策略限制
        LambdaUpdateWrapper<Problem> updateWrapper = Wrappers.lambdaUpdate(Problem.class)
                .eq(Problem::getId, problemId)
                .eq(Problem::getStatus, 1)
                .set(Problem::getPlatform, request.getPlatform())
                .set(Problem::getExternalProblemKey, request.getExternalProblemKey())
                .set(Problem::getTitle, request.getTitle())
                .set(Problem::getSourceUrl, request.getSourceUrl())
                .set(Problem::getDifficulty, request.getDifficulty())
                .set(Problem::getTags, request.getTags())
                .set(Problem::getContentMd, request.getContentMd());

        int rows;
        try {
            rows = problemMapper.update(null, updateWrapper);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(409, "该平台题目标识已存在");
        }

        if (rows == 0) {
            throw new BusinessException(404, "题目不存在");
        }
        if (rows > 1) {
            throw new RuntimeException("更新题目异常：影响行数超过预期");
        }

        // 更新后重新查询，确保响应为数据库实际持久化结果（含 updateTime）
        Problem updated = problemMapper.selectOne(
                new LambdaQueryWrapper<Problem>()
                        .eq(Problem::getId, problemId)
                        .eq(Problem::getStatus, 1));
        if (updated == null) {
            throw new BusinessException(404, "题目不存在");
        }
        return toDetailResponse(updated);
    }

    /**
     * 校验当前操作者是否有权管理指定题目。
     *
     * <p>普通用户只能管理自己创建的题目；管理员不受 {@code creatorUserId} 限制。
     * {@code operatorUserId} 和 {@code operatorAdmin} 来自认证主体，
     * 不由请求体中的任何字段推断。</p>
     */
    private void ensureCanManageProblem(Problem problem, long operatorUserId, boolean operatorAdmin) {
        if (operatorAdmin) {
            return;
        }
        if (!Objects.equals(problem.getCreatorUserId(), operatorUserId)) {
            throw new BusinessException(403, "无权修改该题目");
        }
    }

    private ProblemDetailResponse toDetailResponse(Problem p) {
        return new ProblemDetailResponse(
                p.getId(), p.getPlatform(), p.getExternalProblemKey(), p.getTitle(),
                p.getSourceUrl(), p.getDifficulty(), p.getTags(), p.getContentMd(),
                p.getCreatorUserId(), p.getCreateTime(), p.getUpdateTime());
    }
}
