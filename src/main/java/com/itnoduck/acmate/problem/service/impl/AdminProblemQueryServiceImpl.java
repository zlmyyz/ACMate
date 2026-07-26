package com.itnoduck.acmate.problem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itnoduck.acmate.common.dto.PageResponse;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.problem.dto.AdminProblemSummaryResponse;
import com.itnoduck.acmate.problem.dto.MineProblemStatusFilter;
import com.itnoduck.acmate.problem.dto.ProblemQueryRequest;
import com.itnoduck.acmate.problem.dto.ProblemStatusView;
import com.itnoduck.acmate.problem.entity.Problem;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import com.itnoduck.acmate.problem.service.AdminProblemQueryService;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理员题目查询服务实现。
 *
 * <p>ALL 不限制 status，允许管理员查看全站正常和停用的全部题目。</p>
 * <p>创建者信息使用 selectBatchIds 批量读取，避免 N+1 查询。</p>
 * <p>创建者数据缺失时保留题目记录，username 和 nickname 返回 null，
 * 避免因用户数据异常导致整个列表 500。</p>
 */
@Service
public class AdminProblemQueryServiceImpl implements AdminProblemQueryService {

    private final ProblemMapper problemMapper;
    private final AppUserMapper appUserMapper;

    public AdminProblemQueryServiceImpl(ProblemMapper problemMapper, AppUserMapper appUserMapper) {
        this.problemMapper = problemMapper;
        this.appUserMapper = appUserMapper;
    }

    @Override
    public PageResponse<AdminProblemSummaryResponse> listProblems(ProblemQueryRequest request,
                                                                   MineProblemStatusFilter statusFilter) {
        long page = Math.max(request.getPage(), 1);
        long size = Math.max(1, Math.min(request.getSize(), 100));

        // 管理员查询：ALL 不附加 status 条件
        LambdaQueryWrapper<Problem> wrapper = new LambdaQueryWrapper<>();
        if (statusFilter == MineProblemStatusFilter.ACTIVE) {
            wrapper.eq(Problem::getStatus, 1);
        } else if (statusFilter == MineProblemStatusFilter.INACTIVE) {
            wrapper.eq(Problem::getStatus, 0);
        }

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
        if (request.getKeyword() != null) {
            wrapper.and(w -> w
                    .like(Problem::getTitle, request.getKeyword())
                    .or()
                    .like(Problem::getExternalProblemKey, request.getKeyword()));
        }

        wrapper.orderByDesc(Problem::getCreateTime, Problem::getId);

        Page<Problem> mpPage = new Page<>(page, size);
        Page<Problem> result = problemMapper.selectPage(mpPage, wrapper);

        // 批量加载创建者信息，避免 N+1
        Set<Long> userIds = result.getRecords().stream()
                .map(Problem::getCreatorUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        final Map<Long, AppUser> userMap;
        if (!userIds.isEmpty()) {
            List<AppUser> users = appUserMapper.selectBatchIds(userIds);
            userMap = users.stream()
                    .collect(Collectors.toMap(AppUser::getId, Function.identity()));
        } else {
            userMap = Collections.emptyMap();
        }

        List<AdminProblemSummaryResponse> records = result.getRecords().stream()
                .map(p -> toAdminSummaryResponse(p, userMap))
                .toList();

        return new PageResponse<>(
                result.getCurrent(),
                result.getSize(),
                result.getTotal(),
                result.getPages(),
                records);
    }

    private AdminProblemSummaryResponse toAdminSummaryResponse(Problem p, Map<Long, AppUser> userMap) {
        AppUser creator = userMap.get(p.getCreatorUserId());
        return new AdminProblemSummaryResponse(
                p.getId(), p.getPlatform(), p.getExternalProblemKey(), p.getTitle(),
                p.getSourceUrl(), p.getDifficulty(), p.getTags(),
                ProblemStatusView.fromStatus(p.getStatus()),
                p.getCreatorUserId(),
                // 创建者数据缺失时保留题目记录，username/nickname 返回 null
                creator != null ? creator.getUsername() : null,
                creator != null ? creator.getNickname() : null,
                p.getCreateTime(), p.getUpdateTime());
    }
}
