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

import java.time.LocalDateTime;
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
        problem.setCreatorUserId(creatorUserId);
        problem.setStatus(1);

        int rows;
        try {
            rows = problemMapper.insert(problem);
        } catch (DuplicateKeyException e) {
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

        Problem existing = problemMapper.selectOne(
                new LambdaQueryWrapper<Problem>()
                        .eq(Problem::getId, problemId));
        if (existing == null) {
            throw new BusinessException(404, "题目不存在");
        }

        // 资源所有权校验：创建者或管理员可以管理题目
        // 非创建者且非管理员：停用题目返回 404（不暴露存在性），正常题目返回 403
        if (!operatorAdmin && !Objects.equals(existing.getCreatorUserId(), operatorUserId)) {
            if (existing.getStatus() != null && existing.getStatus() == 0) {
                throw new BusinessException(404, "题目不存在");
            }
            throw new BusinessException(403, "无权修改该题目");
        }

        if (!"CUSTOM".equals(request.getPlatform()) && request.getExternalProblemKey() == null) {
            throw new BusinessException(400, "非自定义平台必须提供外部题目标识");
        }

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

        LambdaUpdateWrapper<Problem> updateWrapper = Wrappers.lambdaUpdate(Problem.class)
                .eq(Problem::getId, problemId)
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

        Problem updated = problemMapper.selectOne(
                new LambdaQueryWrapper<Problem>()
                        .eq(Problem::getId, problemId));
        if (updated == null) {
            throw new BusinessException(404, "题目不存在");
        }
        return toDetailResponse(updated);
    }

    /**
     * 停用题目（ACTIVE → INACTIVE）。
     *
     * <p>必须先查询资源再判断权限——URL 层只能确认调用者已登录，
     * 无法区分谁是创建者。</p>
     * <p>停用后题目仍占用 platform+externalProblemKey，
     * 其他用户不能重新创建相同题目标识。</p>
     * <p>WHERE 条件包含原 status=1，防止并发重复修改。</p>
     */
    @Override
    @Transactional
    public void deactivateProblem(long problemId, long operatorUserId, boolean operatorAdmin) {
        if (problemId <= 0) {
            throw new BusinessException(400, "题目 ID 无效");
        }
        if (operatorUserId <= 0) {
            throw new BusinessException(400, "操作者 ID 无效");
        }

        // 先查询资源：不限制 status，因为停用题目也属于创建者的私有管理内容
        Problem existing = problemMapper.selectOne(
                new LambdaQueryWrapper<Problem>()
                        .eq(Problem::getId, problemId));
        if (existing == null) {
            throw new BusinessException(404, "题目不存在");
        }

        // 权限判断：创建者或管理员
        // 停用题目对其他用户返回 404，不暴露私有停用题目存在性
        if (!operatorAdmin && !Objects.equals(existing.getCreatorUserId(), operatorUserId)) {
            if (existing.getStatus() != null && existing.getStatus() == 0) {
                throw new BusinessException(404, "题目不存在");
            }
            throw new BusinessException(403, "无权管理该题目");
        }

        // 已经停用：幂等返回，不报错
        if (existing.getStatus() != null && existing.getStatus() == 0) {
            return;
        }

        // WHERE 带原状态=1，防止并发重复更新
        LambdaUpdateWrapper<Problem> updateWrapper = Wrappers.lambdaUpdate(Problem.class)
                .eq(Problem::getId, problemId)
                .eq(Problem::getStatus, 1)
                .set(Problem::getStatus, 0)
                .set(Problem::getDeactivationSource, "CREATOR")
                .set(Problem::getDeactivatedBy, operatorUserId)
                .set(Problem::getDeactivationTime, LocalDateTime.now());

        int rows = problemMapper.update(null, updateWrapper);
        if (rows == 0) {
            // 并发下可能已被其他请求修改：重新读取检查目标状态
            Problem recheck = problemMapper.selectOne(
                    new LambdaQueryWrapper<Problem>().eq(Problem::getId, problemId));
            if (recheck == null) {
                throw new BusinessException(404, "题目不存在");
            }
            if (recheck.getStatus() != null && recheck.getStatus() == 0) {
                return; // 已处于目标状态，幂等成功
            }
            throw new RuntimeException("停用题目异常：状态不匹配");
        }
    }

    /**
     * 恢复题目（INACTIVE → ACTIVE）。
     *
     * <p>WHERE 条件包含原 status=0，防止并发重复修改。</p>
     */
    @Override
    @Transactional
    public void restoreProblem(long problemId, long operatorUserId, boolean operatorAdmin) {
        if (problemId <= 0) {
            throw new BusinessException(400, "题目 ID 无效");
        }
        if (operatorUserId <= 0) {
            throw new BusinessException(400, "操作者 ID 无效");
        }

        Problem existing = problemMapper.selectOne(
                new LambdaQueryWrapper<Problem>()
                        .eq(Problem::getId, problemId));
        if (existing == null) {
            throw new BusinessException(404, "题目不存在");
        }

        if (!operatorAdmin && !Objects.equals(existing.getCreatorUserId(), operatorUserId)) {
            if (existing.getStatus() != null && existing.getStatus() == 0) {
                throw new BusinessException(404, "题目不存在");
            }
            throw new BusinessException(403, "无权管理该题目");
        }

        // 管理员强制停用的题目，创建者不能自行恢复
        if (!operatorAdmin && "ADMIN".equals(existing.getDeactivationSource())) {
            throw new BusinessException(403, "该题目由管理员停用，请联系管理员处理");
        }

        // 已经正常：幂等返回
        if (existing.getStatus() != null && existing.getStatus() == 1) {
            return;
        }

        // WHERE 带原状态=0，防止并发重复更新
        LambdaUpdateWrapper<Problem> updateWrapper = Wrappers.lambdaUpdate(Problem.class)
                .eq(Problem::getId, problemId)
                .eq(Problem::getStatus, 0)
                .set(Problem::getStatus, 1)
                .set(Problem::getDeactivationSource, null)
                .set(Problem::getDeactivationReason, null)
                .set(Problem::getDeactivatedBy, null)
                .set(Problem::getDeactivationTime, null);

        int rows = problemMapper.update(null, updateWrapper);
        if (rows == 0) {
            Problem recheck = problemMapper.selectOne(
                    new LambdaQueryWrapper<Problem>().eq(Problem::getId, problemId));
            if (recheck == null) {
                throw new BusinessException(404, "题目不存在");
            }
            if (recheck.getStatus() != null && recheck.getStatus() == 1) {
                return;
            }
            throw new RuntimeException("恢复题目异常：状态不匹配");
        }
    }

    @Override
    @Transactional
    public void adminForceDeactivateProblem(long problemId, String reason, long operatorUserId) {
        if (problemId <= 0) {
            throw new BusinessException(400, "题目 ID 无效");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(400, "停用原因不能为空");
        }
        if (reason.length() > 500) {
            throw new BusinessException(400, "停用原因不能超过 500 字");
        }
        if (operatorUserId <= 0) {
            throw new BusinessException(400, "操作者 ID 无效");
        }

        Problem existing = problemMapper.selectOne(
                new LambdaQueryWrapper<Problem>()
                        .eq(Problem::getId, problemId));
        if (existing == null) {
            throw new BusinessException(404, "题目不存在");
        }

        // 已经停用：幂等返回（不覆盖原有停用来源和原因）
        if (existing.getStatus() != null && existing.getStatus() == 0) {
            return;
        }

        LambdaUpdateWrapper<Problem> updateWrapper = Wrappers.lambdaUpdate(Problem.class)
                .eq(Problem::getId, problemId)
                .eq(Problem::getStatus, 1)
                .set(Problem::getStatus, 0)
                .set(Problem::getDeactivationSource, "ADMIN")
                .set(Problem::getDeactivationReason, reason)
                .set(Problem::getDeactivatedBy, operatorUserId)
                .set(Problem::getDeactivationTime, LocalDateTime.now());

        int rows = problemMapper.update(null, updateWrapper);
        if (rows == 0) {
            Problem recheck = problemMapper.selectOne(
                    new LambdaQueryWrapper<Problem>().eq(Problem::getId, problemId));
            if (recheck == null) {
                throw new BusinessException(404, "题目不存在");
            }
            if (recheck.getStatus() != null && recheck.getStatus() == 0) {
                return;
            }
            throw new RuntimeException("停用题目异常：状态不匹配");
        }
    }

    private ProblemDetailResponse toDetailResponse(Problem p) {
        return new ProblemDetailResponse(
                p.getId(), p.getPlatform(), p.getExternalProblemKey(), p.getTitle(),
                p.getSourceUrl(), p.getDifficulty(), p.getTags(), p.getContentMd(),
                p.getCreatorUserId(), p.getCreateTime(), p.getUpdateTime());
    }
}
