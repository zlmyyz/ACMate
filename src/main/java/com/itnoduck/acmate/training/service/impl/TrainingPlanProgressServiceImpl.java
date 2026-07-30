package com.itnoduck.acmate.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.problem.entity.Problem;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import com.itnoduck.acmate.training.dto.MemberProgressResponse;
import com.itnoduck.acmate.training.entity.TrainingPlan;
import com.itnoduck.acmate.training.entity.TrainingPlanMember;
import com.itnoduck.acmate.training.entity.TrainingPlanProblem;
import com.itnoduck.acmate.training.entity.UserProblemStatus;
import com.itnoduck.acmate.training.enums.ProblemStatus;
import com.itnoduck.acmate.training.mapper.TrainingPlanMapper;
import com.itnoduck.acmate.training.mapper.TrainingPlanMemberMapper;
import com.itnoduck.acmate.training.mapper.TrainingPlanProblemMapper;
import com.itnoduck.acmate.training.mapper.UserProblemStatusMapper;
import com.itnoduck.acmate.training.service.TrainingPlanProgressService;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TrainingPlanProgressServiceImpl implements TrainingPlanProgressService {

    private final TrainingPlanMapper planMapper;
    private final TrainingPlanProblemMapper planProblemMapper;
    private final TrainingPlanMemberMapper memberMapper;
    private final AppUserMapper userMapper;
    private final ProblemMapper problemMapper;
    private final UserProblemStatusMapper upsMapper;

    public TrainingPlanProgressServiceImpl(TrainingPlanMapper planMapper,
                                           TrainingPlanProblemMapper planProblemMapper,
                                           TrainingPlanMemberMapper memberMapper,
                                           AppUserMapper userMapper,
                                           ProblemMapper problemMapper,
                                           UserProblemStatusMapper upsMapper) {
        this.planMapper = planMapper;
        this.planProblemMapper = planProblemMapper;
        this.memberMapper = memberMapper;
        this.userMapper = userMapper;
        this.problemMapper = problemMapper;
        this.upsMapper = upsMapper;
    }

    @Override
    @Transactional
    public void updateStatus(Long planId, Long problemId, String status, Long userId) {
        TrainingPlan plan = planMapper.selectById(planId);
        if (plan == null) throw new BusinessException(404, "训练计划不存在");
        if (plan.getIsActive() == null || plan.getIsActive() == 0)
            throw new BusinessException(400, "该计划已停用");

        TrainingPlanMember member = memberMapper.selectOne(new LambdaQueryWrapper<TrainingPlanMember>()
                .eq(TrainingPlanMember::getPlanId, planId)
                .eq(TrainingPlanMember::getUserId, userId));
        if (member == null || (member.getStatus() != null && member.getStatus() == 0))
            throw new BusinessException(403, "你不是该计划成员");

        long problemExists = planProblemMapper.selectCount(new LambdaQueryWrapper<TrainingPlanProblem>()
                .eq(TrainingPlanProblem::getPlanId, planId)
                .eq(TrainingPlanProblem::getProblemId, problemId));
        if (problemExists == 0)
            throw new BusinessException(404, "该题目不在计划中");

        ProblemStatus ps = ProblemStatus.fromString(status);
        int rows = upsMapper.updateStatusAtomic(userId, problemId, ps.getCode());
        if (rows == 0 && ps.getCode() != 0) {
            // no row yet and status is not NOT_STARTED — insert
            var ups = new UserProblemStatus();
            ups.setUserId(userId);
            ups.setProblemId(problemId);
            ups.setStatus(ps.getCode());
            try {
                upsMapper.insert(ups);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    @Transactional
    public void updateNote(Long planId, Long problemId, String note, Long userId) {
        TrainingPlan plan = planMapper.selectById(planId);
        if (plan == null) throw new BusinessException(404, "训练计划不存在");
        if (plan.getIsActive() == null || plan.getIsActive() == 0)
            throw new BusinessException(400, "该计划已停用");

        TrainingPlanMember member = memberMapper.selectOne(new LambdaQueryWrapper<TrainingPlanMember>()
                .eq(TrainingPlanMember::getPlanId, planId)
                .eq(TrainingPlanMember::getUserId, userId));
        if (member == null || (member.getStatus() != null && member.getStatus() == 0))
            throw new BusinessException(403, "你不是该计划成员");

        long problemExists = planProblemMapper.selectCount(new LambdaQueryWrapper<TrainingPlanProblem>()
                .eq(TrainingPlanProblem::getPlanId, planId)
                .eq(TrainingPlanProblem::getProblemId, problemId));
        if (problemExists == 0)
            throw new BusinessException(404, "该题目不在计划中");

        if (note != null && note.length() > 500)
            throw new BusinessException(400, "备注不能超过500字");

        upsMapper.upsertNote(userId, problemId, note);
    }

    @Override
    public MemberProgressResponse getMemberProgress(Long planId, Long targetUserId, Long currentUserId) {
        TrainingPlan plan = planMapper.selectById(planId);
        if (plan == null) throw new BusinessException(404, "训练计划不存在");

        // permission: PUBLIC members can view each other, PERSONAL only creator
        boolean isPublic = "PUBLIC".equals(plan.getPlanType());
        if (!isPublic) {
            if (!plan.getCreatorUserId().equals(currentUserId) && !plan.getCreatorUserId().equals(targetUserId))
                throw new BusinessException(404, "训练计划不存在");
        } else {
            TrainingPlanMember viewerMember = memberMapper.selectOne(new LambdaQueryWrapper<TrainingPlanMember>()
                    .eq(TrainingPlanMember::getPlanId, planId)
                    .eq(TrainingPlanMember::getUserId, currentUserId));
            TrainingPlanMember targetMember = memberMapper.selectOne(new LambdaQueryWrapper<TrainingPlanMember>()
                    .eq(TrainingPlanMember::getPlanId, planId)
                    .eq(TrainingPlanMember::getUserId, targetUserId));
            boolean viewerIsCreator = plan.getCreatorUserId().equals(currentUserId);
            // viewer must be a member or creator to see others' progress
            boolean viewerCanSee = viewerIsCreator || (viewerMember != null && (viewerMember.getStatus() == null || viewerMember.getStatus() == 1));
            if (!viewerCanSee) throw new BusinessException(404, "训练计划不存在");
            if (targetMember == null || (targetMember.getStatus() != null && targetMember.getStatus() == 0))
                throw new BusinessException(404, "该成员不在计划中");
        }

        // load problems
        List<TrainingPlanProblem> tpps = planProblemMapper.selectList(
                new LambdaQueryWrapper<TrainingPlanProblem>()
                        .eq(TrainingPlanProblem::getPlanId, planId)
                        .orderByAsc(TrainingPlanProblem::getSortOrder));

        Set<Long> problemIds = tpps.stream().map(TrainingPlanProblem::getProblemId).collect(Collectors.toSet());
        Map<Long, Problem> probMap = problemIds.isEmpty() ? Map.of() : problemMapper.selectBatchIds(problemIds).stream()
                .collect(Collectors.toMap(Problem::getId, p -> p));

        // load user status for target user
        List<UserProblemStatus> statuses = problemIds.isEmpty() ? List.of() : upsMapper.selectList(
                new LambdaQueryWrapper<UserProblemStatus>()
                        .eq(UserProblemStatus::getUserId, targetUserId)
                        .in(UserProblemStatus::getProblemId, problemIds));
        Map<Long, UserProblemStatus> statusMap = statuses.stream()
                .collect(Collectors.toMap(UserProblemStatus::getProblemId, s -> s));

        // build problem list
        int completed = 0;
        int total = tpps.size();
        LocalDateTime lastAccepted = null;
        List<MemberProgressResponse.ProblemProgressItem> items = new ArrayList<>();
        for (TrainingPlanProblem tpp : tpps) {
            UserProblemStatus s = statusMap.get(tpp.getProblemId());
            String myStatus = s != null ? ProblemStatus.fromCode(s.getStatus()).name() : "NOT_STARTED";
            if ("ACCEPTED".equals(myStatus)) {
                completed++;
                if (s.getFirstAcTime() != null && (lastAccepted == null || s.getFirstAcTime().isAfter(lastAccepted)))
                    lastAccepted = s.getFirstAcTime();
            }

            MemberProgressResponse.ProblemProgressItem item = new MemberProgressResponse.ProblemProgressItem();
            item.setProblemId(tpp.getProblemId());
            Problem prob = probMap.get(tpp.getProblemId());
            if (prob != null) {
                item.setProblemTitle(prob.getTitle());
                item.setPlatform(prob.getPlatform());
                item.setDifficulty(prob.getDifficulty());
                item.setProblemActive(prob.getStatus() != null && prob.getStatus() == 1);
            }
            item.setSortOrder(tpp.getSortOrder());
            item.setRequired(tpp.getRequiredFlag() == 1);
            item.setMyStatus(myStatus);
            item.setPerformanceNote(s != null ? s.getPerformanceNote() : null);
            items.add(item);
        }

        // compute rank based on completed count + last accepted time
        AppUser targetUser = userMapper.selectById(targetUserId);
        MemberProgressResponse resp = new MemberProgressResponse();
        resp.setUserId(targetUserId);
        resp.setUsername(targetUser != null ? targetUser.getUsername() : null);
        resp.setNickname(targetUser != null ? targetUser.getNickname() : null);
        resp.setAvatarUrl(targetUser != null ? targetUser.getAvatarUrl() : null);
        resp.setCompletedCount(completed);
        resp.setTotalCount(total);
        resp.setLastAcceptedTime(lastAccepted);
        resp.setProblems(items);
        return resp;
    }
}
