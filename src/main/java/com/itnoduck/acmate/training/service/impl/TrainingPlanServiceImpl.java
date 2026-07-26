package com.itnoduck.acmate.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.training.dto.*;
import com.itnoduck.acmate.training.entity.TrainingPlan;
import com.itnoduck.acmate.training.entity.TrainingPlanMember;
import com.itnoduck.acmate.training.entity.TrainingPlanProblem;
import com.itnoduck.acmate.training.mapper.TrainingPlanMapper;
import com.itnoduck.acmate.training.mapper.TrainingPlanMemberMapper;
import com.itnoduck.acmate.training.mapper.TrainingPlanProblemMapper;
import com.itnoduck.acmate.training.service.TrainingPlanService;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import com.itnoduck.acmate.problem.entity.Problem;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TrainingPlanServiceImpl implements TrainingPlanService {

    private final TrainingPlanMapper planMapper;
    private final TrainingPlanProblemMapper planProblemMapper;
    private final TrainingPlanMemberMapper memberMapper;
    private final AppUserMapper userMapper;
    private final ProblemMapper problemMapper;

    public TrainingPlanServiceImpl(TrainingPlanMapper planMapper,
                                    TrainingPlanProblemMapper planProblemMapper,
                                    TrainingPlanMemberMapper memberMapper,
                                    AppUserMapper userMapper,
                                    ProblemMapper problemMapper) {
        this.planMapper = planMapper;
        this.planProblemMapper = planProblemMapper;
        this.memberMapper = memberMapper;
        this.userMapper = userMapper;
        this.problemMapper = problemMapper;
    }

    @Override
    @Transactional
    public PlanDetailResponse createPlan(CreatePlanRequest request, Long creatorUserId) {
        String planType = request.getPlanType();
        if (planType == null || !planType.equals("PUBLIC")) {
            planType = "PERSONAL";
        }

        TrainingPlan plan = new TrainingPlan();
        plan.setTitle(request.getTitle().strip());
        plan.setDescription(request.getDescription());
        plan.setStartTime(request.getStartTime());
        plan.setEndTime(request.getEndTime());
        plan.setCreatorUserId(creatorUserId);
        plan.setPlanType(planType);
        plan.setIsActive(1);

        if (request.getStartTime() != null && request.getEndTime() != null
                && !request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException(400, "结束时间必须晚于开始时间");
        }

        planMapper.insert(plan);
        return toDetailResponse(plan, creatorUserId);
    }

    @Override
    @Transactional
    public PlanDetailResponse updatePlan(Long planId, UpdatePlanRequest request, Long userId) {
        TrainingPlan plan = getPlan(planId);
        checkEditPermission(plan, userId);

        LambdaUpdateWrapper<TrainingPlan> wrapper = Wrappers.lambdaUpdate(TrainingPlan.class)
                .eq(TrainingPlan::getId, planId);

        if (request.getTitle() != null) {
            wrapper.set(TrainingPlan::getTitle, request.getTitle().strip());
        }
        if (request.getDescription() != null) {
            wrapper.set(TrainingPlan::getDescription,
                    request.getDescription().isBlank() ? null : request.getDescription().strip());
        }
        if (request.getStartTime() != null) {
            wrapper.set(TrainingPlan::getStartTime, request.getStartTime());
        }
        if (request.getEndTime() != null) {
            wrapper.set(TrainingPlan::getEndTime, request.getEndTime());
        }

        planMapper.update(null, wrapper);
        return getPlanDetail(planId, userId);
    }

    @Override
    public PlanDetailResponse getPlanDetail(Long planId, Long userId) {
        TrainingPlan plan = getPlan(planId);
        checkViewPermission(plan, userId);
        return toDetailResponse(plan, userId);
    }

    @Override
    public List<PlanSummaryResponse> listPlans(String planType, String timeStatus, String keyword,
                                                int page, int size, Long userId) {
        LambdaQueryWrapper<TrainingPlan> qw = new LambdaQueryWrapper<>();

        if ("PERSONAL".equals(planType)) {
            qw.eq(TrainingPlan::getPlanType, "PERSONAL")
              .eq(TrainingPlan::getCreatorUserId, userId);
        } else {
            qw.eq(TrainingPlan::getPlanType, "PUBLIC")
              .eq(TrainingPlan::getIsActive, 1);
        }

        if (keyword != null && !keyword.isBlank()) {
            qw.like(TrainingPlan::getTitle, keyword.strip());
        }

        if (timeStatus != null && !timeStatus.isBlank()) {
            LocalDateTime now = LocalDateTime.now();
            switch (timeStatus) {
                case "NOT_STARTED" -> qw.and(w -> w.isNull(TrainingPlan::getStartTime)
                        .or().gt(TrainingPlan::getStartTime, now));
                case "ONGOING" -> qw.and(w -> w.le(TrainingPlan::getStartTime, now)
                        .and(w2 -> w2.isNull(TrainingPlan::getEndTime)
                                .or().gt(TrainingPlan::getEndTime, now)));
                case "ENDED" -> qw.and(w -> w.isNotNull(TrainingPlan::getEndTime)
                        .le(TrainingPlan::getEndTime, now));
            }
        }

        qw.orderByDesc(TrainingPlan::getCreateTime);

        Page<TrainingPlan> result = planMapper.selectPage(new Page<>(page, size), qw);
        List<PlanSummaryResponse> list = new ArrayList<>();
        for (TrainingPlan p : result.getRecords()) {
            list.add(toSummaryResponse(p));
        }
        return list;
    }

    @Override
    public int countPlans(String planType, String timeStatus, String keyword, Long userId) {
        LambdaQueryWrapper<TrainingPlan> qw = new LambdaQueryWrapper<>();

        if ("PERSONAL".equals(planType)) {
            qw.eq(TrainingPlan::getPlanType, "PERSONAL")
              .eq(TrainingPlan::getCreatorUserId, userId);
        } else {
            qw.eq(TrainingPlan::getPlanType, "PUBLIC")
              .eq(TrainingPlan::getIsActive, 1);
        }

        if (keyword != null && !keyword.isBlank()) {
            qw.like(TrainingPlan::getTitle, keyword.strip());
        }

        if (timeStatus != null && !timeStatus.isBlank()) {
            LocalDateTime now = LocalDateTime.now();
            switch (timeStatus) {
                case "NOT_STARTED" -> qw.and(w -> w.isNull(TrainingPlan::getStartTime)
                        .or().gt(TrainingPlan::getStartTime, now));
                case "ONGOING" -> qw.and(w -> w.le(TrainingPlan::getStartTime, now)
                        .and(w2 -> w2.isNull(TrainingPlan::getEndTime)
                                .or().gt(TrainingPlan::getEndTime, now)));
                case "ENDED" -> qw.and(w -> w.isNotNull(TrainingPlan::getEndTime)
                        .le(TrainingPlan::getEndTime, now));
            }
        }

        Long cnt = planMapper.selectCount(qw);
        return cnt != null ? cnt.intValue() : 0;
    }

    @Override
    @Transactional
    public void deletePlan(Long planId, Long userId) {
        TrainingPlan plan = getPlan(planId);
        if (!plan.getCreatorUserId().equals(userId)) {
            throw new BusinessException(403, "无权删除该计划");
        }
        planProblemMapper.delete(new LambdaQueryWrapper<TrainingPlanProblem>()
                .eq(TrainingPlanProblem::getPlanId, planId));
        memberMapper.delete(new LambdaQueryWrapper<TrainingPlanMember>()
                .eq(TrainingPlanMember::getPlanId, planId));
        planMapper.deleteById(planId);
    }

    @Override
    @Transactional
    public void addProblem(Long planId, AddProblemRequest request, Long userId) {
        TrainingPlan plan = getPlan(planId);
        checkEditPermission(plan, userId);

        Problem problem = problemMapper.selectById(request.getProblemId());
        if (problem == null || problem.getStatus() == 0) {
            throw new BusinessException(404, "题目不存在");
        }

        long exists = planProblemMapper.selectCount(new LambdaQueryWrapper<TrainingPlanProblem>()
                .eq(TrainingPlanProblem::getPlanId, planId)
                .eq(TrainingPlanProblem::getProblemId, request.getProblemId()));
        if (exists > 0) {
            throw new BusinessException(409, "该题目已在计划中");
        }

        TrainingPlanProblem tpp = new TrainingPlanProblem();
        tpp.setPlanId(planId);
        tpp.setProblemId(request.getProblemId());
        tpp.setSortOrder(request.getSortOrder());
        tpp.setRequiredFlag(request.getRequiredFlag() == 0 ? 0 : 1);
        planProblemMapper.insert(tpp);
    }

    @Override
    @Transactional
    public void removeProblem(Long planId, Long problemId, Long userId) {
        TrainingPlan plan = getPlan(planId);
        checkEditPermission(plan, userId);
        planProblemMapper.delete(new LambdaQueryWrapper<TrainingPlanProblem>()
                .eq(TrainingPlanProblem::getPlanId, planId)
                .eq(TrainingPlanProblem::getProblemId, problemId));
    }

    @Override
    @Transactional
    public void joinPlan(Long planId, Long userId) {
        TrainingPlan plan = getPlan(planId);
        if (!"PUBLIC".equals(plan.getPlanType())) {
            throw new BusinessException(400, "该计划不允许自由加入");
        }
        if (plan.getIsActive() == 0) {
            throw new BusinessException(400, "该计划已停用，不接受新成员");
        }

        long exists = memberMapper.selectCount(new LambdaQueryWrapper<TrainingPlanMember>()
                .eq(TrainingPlanMember::getPlanId, planId)
                .eq(TrainingPlanMember::getUserId, userId));
        if (exists > 0) {
            throw new BusinessException(409, "你已加入该计划");
        }

        TrainingPlanMember member = new TrainingPlanMember();
        member.setPlanId(planId);
        member.setUserId(userId);
        memberMapper.insert(member);
    }

    @Override
    @Transactional
    public void removeMember(Long planId, Long memberUserId, Long operatorUserId) {
        TrainingPlan plan = getPlan(planId);
        checkEditPermission(plan, operatorUserId);

        memberMapper.delete(new LambdaQueryWrapper<TrainingPlanMember>()
                .eq(TrainingPlanMember::getPlanId, planId)
                .eq(TrainingPlanMember::getUserId, memberUserId));
    }

    @Override
    @Transactional
    public void toggleActive(Long planId, Long userId) {
        TrainingPlan plan = getPlan(planId);
        checkEditPermission(plan, userId);

        int newStatus = plan.getIsActive() == 1 ? 0 : 1;
        planMapper.update(null, Wrappers.lambdaUpdate(TrainingPlan.class)
                .eq(TrainingPlan::getId, planId)
                .set(TrainingPlan::getIsActive, newStatus));
    }

    // -- helpers --

    private TrainingPlan getPlan(Long planId) {
        TrainingPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(404, "训练计划不存在");
        }
        return plan;
    }

    private void checkEditPermission(TrainingPlan plan, Long userId) {
        if (plan.getCreatorUserId().equals(userId)) return;
        throw new BusinessException(403, "无权修改该计划");
    }

    private void checkViewPermission(TrainingPlan plan, Long userId) {
        if ("PERSONAL".equals(plan.getPlanType()) && !plan.getCreatorUserId().equals(userId)) {
            throw new BusinessException(403, "无权查看该计划");
        }
    }

    private PlanSummaryResponse toSummaryResponse(TrainingPlan plan) {
        PlanSummaryResponse r = new PlanSummaryResponse();
        r.setId(plan.getId());
        r.setTitle(plan.getTitle());
        r.setPlanType(plan.getPlanType());
        r.setActive(plan.getIsActive() != null && plan.getIsActive() == 1);
        r.setStartTime(plan.getStartTime());
        r.setEndTime(plan.getEndTime());
        r.setTimeStatus(computeTimeStatus(plan));
        r.setCreateTime(plan.getCreateTime());

        AppUser creator = userMapper.selectById(plan.getCreatorUserId());
        r.setCreatorUsername(creator != null ? creator.getUsername() : null);
        r.setCreatorNickname(creator != null ? creator.getNickname() : null);

        long pc = planProblemMapper.selectCount(new LambdaQueryWrapper<TrainingPlanProblem>()
                .eq(TrainingPlanProblem::getPlanId, plan.getId()));
        r.setProblemCount((int) pc);

        long mc = memberMapper.selectCount(new LambdaQueryWrapper<TrainingPlanMember>()
                .eq(TrainingPlanMember::getPlanId, plan.getId()));
        r.setMemberCount((int) mc);

        return r;
    }

    private PlanDetailResponse toDetailResponse(TrainingPlan plan, Long userId) {
        PlanDetailResponse r = new PlanDetailResponse();
        r.setId(plan.getId());
        r.setTitle(plan.getTitle());
        r.setDescription(plan.getDescription());
        r.setPlanType(plan.getPlanType());
        r.setActive(plan.getIsActive() != null && plan.getIsActive() == 1);
        r.setCreatorUserId(plan.getCreatorUserId());
        r.setStartTime(plan.getStartTime());
        r.setEndTime(plan.getEndTime());
        r.setTimeStatus(computeTimeStatus(plan));
        r.setCreateTime(plan.getCreateTime());
        r.setUpdateTime(plan.getUpdateTime());

        AppUser creator = userMapper.selectById(plan.getCreatorUserId());
        r.setCreatorUsername(creator != null ? creator.getUsername() : null);
        r.setCreatorNickname(creator != null ? creator.getNickname() : null);

        long mc = memberMapper.selectCount(new LambdaQueryWrapper<TrainingPlanMember>()
                .eq(TrainingPlanMember::getPlanId, plan.getId()));
        r.setMemberCount((int) mc);

        boolean isMember = memberMapper.selectCount(new LambdaQueryWrapper<TrainingPlanMember>()
                .eq(TrainingPlanMember::getPlanId, plan.getId())
                .eq(TrainingPlanMember::getUserId, userId)) > 0;
        r.setMember(isMember || plan.getCreatorUserId().equals(userId));

        List<TrainingPlanProblem> tpps = planProblemMapper.selectList(
                new LambdaQueryWrapper<TrainingPlanProblem>()
                        .eq(TrainingPlanProblem::getPlanId, plan.getId())
                        .orderByAsc(TrainingPlanProblem::getSortOrder));
        List<PlanProblemResponse> problems = new ArrayList<>();
        for (TrainingPlanProblem tpp : tpps) {
            PlanProblemResponse pp = new PlanProblemResponse();
            pp.setId(tpp.getId());
            pp.setProblemId(tpp.getProblemId());
            pp.setSortOrder(tpp.getSortOrder());
            pp.setRequired(tpp.getRequiredFlag() == 1);

            Problem prob = problemMapper.selectById(tpp.getProblemId());
            if (prob != null) {
                pp.setProblemTitle(prob.getTitle());
                pp.setPlatform(prob.getPlatform());
                pp.setDifficulty(prob.getDifficulty());
                pp.setProblemActive(prob.getStatus() != null && prob.getStatus() == 1);
            }
            problems.add(pp);
        }
        r.setProblems(problems);
        r.setProblemCount(problems.size());

        return r;
    }

    private String computeTimeStatus(TrainingPlan plan) {
        LocalDateTime now = LocalDateTime.now();
        if (plan.getStartTime() != null && now.isBefore(plan.getStartTime())) {
            return "NOT_STARTED";
        }
        if (plan.getEndTime() != null && (now.isEqual(plan.getEndTime()) || now.isAfter(plan.getEndTime()))) {
            return "ENDED";
        }
        return "ONGOING";
    }
}
