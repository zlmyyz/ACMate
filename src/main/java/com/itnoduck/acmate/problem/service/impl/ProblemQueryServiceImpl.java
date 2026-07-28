package com.itnoduck.acmate.problem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itnoduck.acmate.common.dto.PageResponse;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.problem.dto.MineProblemStatusFilter;
import com.itnoduck.acmate.problem.dto.MyProblemSummaryResponse;
import com.itnoduck.acmate.problem.dto.ProblemDetailResponse;
import com.itnoduck.acmate.problem.dto.ProblemQueryRequest;
import com.itnoduck.acmate.problem.dto.ProblemStatusView;
import com.itnoduck.acmate.problem.dto.ProblemSummaryResponse;
import com.itnoduck.acmate.problem.entity.Problem;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import com.itnoduck.acmate.problem.service.ProblemQueryService;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 题目查询服务实现。
 *
 * <p>公共列表固定过滤 status=1，避免普通用户看到已禁用的题目。
 * "我的题目"列表允许创建者查看自己的所有题目（含停用），但仅限当前认证用户。</p>
 */
@Service
public class ProblemQueryServiceImpl implements ProblemQueryService {

    private final ProblemMapper problemMapper;
    private final AppUserMapper appUserMapper;

    public ProblemQueryServiceImpl(ProblemMapper problemMapper, AppUserMapper appUserMapper) {
        this.problemMapper = problemMapper;
        this.appUserMapper = appUserMapper;
    }

    @Override
    public ProblemDetailResponse getProblem(long id, long viewerUserId, boolean viewerAdmin) {
        if (id <= 0) {
            throw new BusinessException(404, "题目不存在");
        }
        Problem problem = problemMapper.selectOne(
                new LambdaQueryWrapper<Problem>()
                        .eq(Problem::getId, id));
        if (problem == null) {
            throw new BusinessException(404, "题目不存在");
        }
        // status=0：仅创建者和管理员可见，其他用户返回 404 避免暴露停用题目存在性
        if (problem.getStatus() != null && problem.getStatus() == 0) {
            if (!viewerAdmin && !Objects.equals(problem.getCreatorUserId(), viewerUserId)) {
                throw new BusinessException(404, "题目不存在");
            }
        }
        return toDetailResponse(problem);
    }

    @Override
    public PageResponse<ProblemSummaryResponse> listProblems(ProblemQueryRequest request) {
        long page = Math.max(request.getPage(), 1);
        long size = Math.max(1, Math.min(request.getSize(), 100));

        // 基础条件：只返回正常状态的题目
        // creatorUserId 用于按创建者筛选用户的公开题目，不影响 status=1 限制
        LambdaQueryWrapper<Problem> wrapper = new LambdaQueryWrapper<Problem>()
                .eq(Problem::getStatus, 1);

        if (request.getCreatorUserId() != null) {
            if (request.getCreatorUserId() <= 0) {
                throw new BusinessException(400, "创建者 ID 必须为正数");
            }
            wrapper.eq(Problem::getCreatorUserId, request.getCreatorUserId());
        }
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

        java.util.Map<Long, AppUser> creatorMap = batchLoadCreators(result.getRecords());
        List<ProblemSummaryResponse> records = result.getRecords().stream()
                .map(p -> toSummaryResponse(p, creatorMap.get(p.getCreatorUserId())))
                .toList();

        return new PageResponse<>(
                result.getCurrent(),
                result.getSize(),
                result.getTotal(),
                result.getPages(),
                records);
    }

    @Override
    public PageResponse<MyProblemSummaryResponse> listMyProblems(ProblemQueryRequest request,
                                                                  MineProblemStatusFilter statusFilter,
                                                                  long currentUserId) {
        if (currentUserId <= 0) {
            throw new BusinessException(400, "用户 ID 无效");
        }
        long page = Math.max(request.getPage(), 1);
        long size = Math.max(1, Math.min(request.getSize(), 100));

        // creatorUserId 必须等于当前认证用户，不接受客户端指定
        LambdaQueryWrapper<Problem> wrapper = new LambdaQueryWrapper<Problem>()
                .eq(Problem::getCreatorUserId, currentUserId);

        // statusFilter 只影响是否附加 status 条件
        if (statusFilter == MineProblemStatusFilter.ACTIVE) {
            wrapper.eq(Problem::getStatus, 1);
        } else if (statusFilter == MineProblemStatusFilter.INACTIVE) {
            wrapper.eq(Problem::getStatus, 0);
        }
        // ALL：不附加 status 条件

        if (request.getPlatform() != null) {
            wrapper.eq(Problem::getPlatform, request.getPlatform());
        }
        if (request.getDifficulty() != null) {
            wrapper.eq(Problem::getDifficulty, request.getDifficulty());
        }
        if (request.getKeyword() != null) {
            wrapper.and(w -> w
                    .like(Problem::getTitle, request.getKeyword())
                    .or()
                    .like(Problem::getExternalProblemKey, request.getKeyword()));
        }

        wrapper.orderByDesc(Problem::getCreateTime, Problem::getId);

        Page<Problem> mpPage = new Page<>(page, size);
        Page<Problem> result = problemMapper.selectPage(mpPage, wrapper);

        List<MyProblemSummaryResponse> records = result.getRecords().stream()
                .map(this::toMySummaryResponse)
                .toList();

        return new PageResponse<>(
                result.getCurrent(),
                result.getSize(),
                result.getTotal(),
                result.getPages(),
                records);
    }

    ProblemDetailResponse toDetailResponse(Problem p) {
        String creatorUsername = null;
        String creatorNickname = null;
        if (p.getCreatorUserId() != null) {
            AppUser creator = appUserMapper.selectById(p.getCreatorUserId());
            if (creator != null) {
                creatorUsername = creator.getUsername();
                creatorNickname = creator.getNickname();
            }
        }
        return new ProblemDetailResponse(
                p.getId(), p.getPlatform(), p.getExternalProblemKey(), p.getTitle(),
                p.getSourceUrl(), p.getDifficulty(), p.getTags(), p.getContentMd(),
                p.getCreatorUserId(), creatorUsername, creatorNickname,
                p.getCreateTime(), p.getUpdateTime());
    }

    private ProblemSummaryResponse toSummaryResponse(Problem p, AppUser creator) {
        return new ProblemSummaryResponse(
                p.getId(), p.getPlatform(), p.getExternalProblemKey(), p.getTitle(),
                p.getSourceUrl(), p.getDifficulty(), p.getTags(),
                p.getCreatorUserId(),
                creator != null ? creator.getUsername() : null,
                creator != null ? creator.getNickname() : null,
                p.getCreateTime());
    }

    private java.util.Map<Long, AppUser> batchLoadCreators(List<Problem> problems) {
        java.util.Set<Long> ids = problems.stream()
                .map(Problem::getCreatorUserId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        if (ids.isEmpty()) return java.util.Map.of();
        return appUserMapper.selectBatchIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(
                        com.itnoduck.acmate.user.entity.AppUser::getId, u -> u));
    }

    private MyProblemSummaryResponse toMySummaryResponse(Problem p) {
        return new MyProblemSummaryResponse(
                p.getId(), p.getPlatform(), p.getExternalProblemKey(), p.getTitle(),
                p.getSourceUrl(), p.getDifficulty(), p.getTags(),
                ProblemStatusView.fromStatus(p.getStatus()),
                p.getCreateTime(), p.getUpdateTime());
    }
}
