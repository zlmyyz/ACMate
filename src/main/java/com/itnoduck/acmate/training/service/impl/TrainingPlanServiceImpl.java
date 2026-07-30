package com.itnoduck.acmate.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itnoduck.acmate.auditlog.service.AuditLogService;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.notification.event.NotificationEvent;
import com.itnoduck.acmate.training.dto.*;
import com.itnoduck.acmate.training.entity.TrainingPlan;
import com.itnoduck.acmate.user.dto.PublicPlanSummaryResponse;
import com.itnoduck.acmate.training.entity.TrainingPlanMember;
import com.itnoduck.acmate.training.entity.TrainingPlanProblem;
import com.itnoduck.acmate.training.entity.UserProblemStatus;
import com.itnoduck.acmate.training.enums.ProblemStatus;
import com.itnoduck.acmate.training.mapper.TrainingPlanMapper;
import com.itnoduck.acmate.training.mapper.TrainingPlanMemberMapper;
import com.itnoduck.acmate.training.mapper.TrainingPlanProblemMapper;
import com.itnoduck.acmate.training.mapper.UserProblemStatusMapper;
import com.itnoduck.acmate.training.service.TrainingPlanService;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import com.itnoduck.acmate.problem.entity.Problem;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TrainingPlanServiceImpl implements TrainingPlanService {

    private static final Set<String> VALID_PLAN_TYPES = Set.of("PERSONAL", "PUBLIC");

    private final TrainingPlanMapper planMapper;
    private final TrainingPlanProblemMapper planProblemMapper;
    private final TrainingPlanMemberMapper memberMapper;
    private final AppUserMapper userMapper;
    private final ProblemMapper problemMapper;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserProblemStatusMapper upsMapper;

    public TrainingPlanServiceImpl(TrainingPlanMapper planMapper,
                                    TrainingPlanProblemMapper planProblemMapper,
                                    TrainingPlanMemberMapper memberMapper,
                                    AppUserMapper userMapper,
                                    ProblemMapper problemMapper,
                                    AuditLogService auditLogService,
                                    ApplicationEventPublisher eventPublisher,
                                    UserProblemStatusMapper upsMapper) {
        this.planMapper = planMapper;
        this.planProblemMapper = planProblemMapper;
        this.memberMapper = memberMapper;
        this.userMapper = userMapper;
        this.problemMapper = problemMapper;
        this.auditLogService = auditLogService;
        this.eventPublisher = eventPublisher;
        this.upsMapper = upsMapper;
    }

    @Override
    @Transactional
    public PlanDetailResponse createPlan(CreatePlanRequest request, Long creatorUserId) {
        String title = request.getTitle() != null ? request.getTitle().strip() : "";
        if (title.isEmpty()) throw new BusinessException(400, "标题不能为空");

        String planType = request.getPlanType();
        if (planType == null || !VALID_PLAN_TYPES.contains(planType)) {
            planType = "PERSONAL";
        }

        if ("PUBLIC".equals(planType) && !isAdminUser(creatorUserId)) {
            throw new BusinessException(403, "只有管理员才能创建公开计划");
        }

        if (request.getStartTime() != null && request.getEndTime() != null
                && !request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException(400, "结束时间必须晚于开始时间");
        }

        TrainingPlan plan = new TrainingPlan();
        plan.setTitle(title);
        plan.setDescription(request.getDescription());
        plan.setStartTime(request.getStartTime());
        plan.setEndTime(request.getEndTime());
        plan.setCreatorUserId(creatorUserId);
        plan.setPlanType(planType);
        plan.setIsActive(1);
        planMapper.insert(plan);

        if ("PERSONAL".equals(planType)) {
            TrainingPlanMember member = new TrainingPlanMember();
            member.setPlanId(plan.getId());
            member.setUserId(creatorUserId);
            memberMapper.insert(member);
        }

        List<Long> problemIds = request.getProblemIds();
        if (problemIds != null && !problemIds.isEmpty()) {
            validateAndInsertProblems(plan.getId(), problemIds);
        }

        return toDetailResponse(plan, creatorUserId);
    }

    @Override
    @Transactional
    public PlanDetailResponse updatePlan(Long planId, UpdatePlanRequest request, Long userId) {
        TrainingPlan plan = getPlan(planId);
        checkEditPermission(plan, userId);

        boolean scheduleChanged = false;
        if (request.getStartTime() != null && !request.getStartTime().equals(plan.getStartTime())) {
            if ("ENDED".equals(computeTimeStatus(plan)))
                throw new BusinessException(400, "已结束的计划不能修改时间");
            scheduleChanged = true;
        }
        if (request.getEndTime() != null && !request.getEndTime().equals(plan.getEndTime())) {
            if ("ENDED".equals(computeTimeStatus(plan)))
                throw new BusinessException(400, "已结束的计划不能修改时间");
            scheduleChanged = true;
        }

        LambdaUpdateWrapper<TrainingPlan> wrapper = Wrappers.lambdaUpdate(TrainingPlan.class)
                .eq(TrainingPlan::getId, planId);

        if (request.getTitle() != null) {
            String t = request.getTitle().strip();
            if (t.isEmpty()) throw new BusinessException(400, "标题不能为空");
            wrapper.set(TrainingPlan::getTitle, t);
        }
        if (request.getDescription() != null) {
            String d = request.getDescription().strip();
            wrapper.set(TrainingPlan::getDescription, d.isBlank() ? null : d);
        }
        if (request.getStartTime() != null) {
            wrapper.set(TrainingPlan::getStartTime, request.getStartTime());
        }
        if (request.getEndTime() != null) {
            wrapper.set(TrainingPlan::getEndTime, request.getEndTime());
        }

        planMapper.update(null, wrapper);

        if (scheduleChanged && "PUBLIC".equals(plan.getPlanType())) {
            Set<Long> memberIds = getCurrentMemberUserIds(planId);
            var payload = new LinkedHashMap<String, Object>();
            payload.put("planTitle", plan.getTitle());
            eventPublisher.publishEvent(new NotificationEvent(
                    memberIds, userId, "TRAINING_SCHEDULE_CHANGED",
                    "TRAINING_PLAN", planId, payload));
        }

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
        page = Math.max(1, page);
        size = Math.max(1, Math.min(size, 100));

        LambdaQueryWrapper<TrainingPlan> qw = new LambdaQueryWrapper<>();

        if ("MY_CREATED".equals(planType)) {
            qw.eq(TrainingPlan::getCreatorUserId, userId);
        } else if ("MY_JOINED".equals(planType)) {
            List<TrainingPlanMember> memberships = memberMapper.selectList(
                    new LambdaQueryWrapper<TrainingPlanMember>().eq(TrainingPlanMember::getUserId, userId));
            Set<Long> joinedIds = memberships.stream().map(TrainingPlanMember::getPlanId).collect(Collectors.toSet());
            if (joinedIds.isEmpty()) return List.of();
            qw.in(TrainingPlan::getId, joinedIds);
        } else {
            qw.eq(TrainingPlan::getPlanType, "PUBLIC")
              .eq(TrainingPlan::getIsActive, 1);
        }

        if (keyword != null && !keyword.isBlank()) {
            qw.like(TrainingPlan::getTitle, keyword.strip());
        }

        if (timeStatus != null && !timeStatus.isBlank()) {
            applyTimeStatusFilter(qw, timeStatus);
        }

        qw.orderByDesc(TrainingPlan::getCreateTime);

        Page<TrainingPlan> result = planMapper.selectPage(new Page<>(page, size), qw);
        List<TrainingPlan> plans = result.getRecords();
        if (plans.isEmpty()) return List.of();

        Set<Long> creatorIds = plans.stream().map(TrainingPlan::getCreatorUserId).collect(Collectors.toSet());
        Map<Long, AppUser> userMap = batchLoadUsers(creatorIds);

        Set<Long> planIds = plans.stream().map(TrainingPlan::getId).collect(Collectors.toSet());
        Map<Long, Integer> problemCounts = batchCountProblems(planIds);
        Map<Long, Integer> memberCounts = batchCountMembers(planIds);

        return plans.stream().map(p -> {
            PlanSummaryResponse r = new PlanSummaryResponse();
            r.setId(p.getId());
            r.setTitle(p.getTitle());
            r.setPlanType(p.getPlanType());
            r.setActive(p.getIsActive() != null && p.getIsActive() == 1);
            r.setStartTime(p.getStartTime());
            r.setEndTime(p.getEndTime());
            r.setTimeStatus(computeTimeStatus(p));
            r.setCreateTime(p.getCreateTime());
            r.setProblemCount(problemCounts.getOrDefault(p.getId(), 0));
            r.setMemberCount(memberCounts.getOrDefault(p.getId(), 0));

            AppUser creator = userMap.get(p.getCreatorUserId());
            if (creator != null) {
                r.setCreatorUsername(creator.getUsername());
                r.setCreatorNickname(creator.getNickname());
            }
            return r;
        }).collect(Collectors.toList());
    }

    @Override
    public int countPlans(String planType, String timeStatus, String keyword, Long userId) {
        LambdaQueryWrapper<TrainingPlan> qw = new LambdaQueryWrapper<>();

        if ("MY_CREATED".equals(planType)) {
            qw.eq(TrainingPlan::getCreatorUserId, userId);
        } else if ("MY_JOINED".equals(planType)) {
            List<TrainingPlanMember> memberships = memberMapper.selectList(
                    new LambdaQueryWrapper<TrainingPlanMember>().eq(TrainingPlanMember::getUserId, userId));
            Set<Long> joinedIds = memberships.stream().map(TrainingPlanMember::getPlanId).collect(Collectors.toSet());
            if (joinedIds.isEmpty()) return 0;
            qw.in(TrainingPlan::getId, joinedIds);
        } else {
            qw.eq(TrainingPlan::getPlanType, "PUBLIC")
              .eq(TrainingPlan::getIsActive, 1);
        }

        if (keyword != null && !keyword.isBlank()) {
            qw.like(TrainingPlan::getTitle, keyword.strip());
        }
        if (timeStatus != null && !timeStatus.isBlank()) {
            applyTimeStatusFilter(qw, timeStatus);
        }

        Long cnt = planMapper.selectCount(qw);
        return cnt != null ? cnt.intValue() : 0;
    }

    @Override
    @Transactional
    public void deactivatePlan(Long planId, String reason, Long userId) {
        TrainingPlan plan = getPlan(planId);
        boolean isCreator = plan.getCreatorUserId().equals(userId);
        boolean isAdmin = isAdminUser(userId);

        if (!isCreator && !isAdmin) {
            throw new BusinessException(403, "无权停用该计划");
        }

        if (plan.getIsActive() != null && plan.getIsActive() == 0) return;

        plan.setIsActive(0);
        plan.setDeactivationTime(LocalDateTime.now());
        plan.setDeactivatedBy(userId);

        if (isAdmin && !isCreator) {
            if (reason == null || reason.isBlank()) {
                throw new BusinessException(400, "管理员强制停用必须填写原因");
            }
            plan.setDeactivationSource("ADMIN");
            plan.setDeactivationReason(reason.strip());
            auditLogService.log(userId, "TRAINING_ADMIN_DEACTIVATED", "TRAINING_PLAN", planId, reason.strip(), "ACTIVE", "DEACTIVATED");
            Set<Long> memberIds = getCurrentMemberUserIds(planId);
            memberIds.add(plan.getCreatorUserId());
            var payload = new LinkedHashMap<String, Object>();
            payload.put("planTitle", plan.getTitle());
            payload.put("reason", reason.strip());
            eventPublisher.publishEvent(new NotificationEvent(
                    memberIds, userId, "TRAINING_ADMIN_DEACTIVATED",
                    "TRAINING_PLAN", planId, payload));
        } else {
            plan.setDeactivationSource("CREATOR");
            plan.setDeactivationReason(reason != null && !reason.isBlank() ? reason.strip() : null);
        }
        planMapper.updateById(plan);
    }

    @Override
    @Transactional
    public void restorePlan(Long planId, Long userId) {
        TrainingPlan plan = getPlan(planId);
        boolean isCreator = plan.getCreatorUserId().equals(userId);
        boolean isAdmin = isAdminUser(userId);

        if (!isCreator && !isAdmin) {
            throw new BusinessException(403, "无权恢复该计划");
        }

        if (plan.getIsActive() != null && plan.getIsActive() == 1) return;

        if (isCreator && "ADMIN".equals(plan.getDeactivationSource())) {
            throw new BusinessException(403, "该计划由管理员停用，无法自行恢复");
        }

        plan.setIsActive(1);
        plan.setDeactivationSource(null);
        plan.setDeactivationReason(null);
        plan.setDeactivatedBy(null);
        plan.setDeactivationTime(null);
        planMapper.updateById(plan);

        if (isAdmin && !isCreator) {
            auditLogService.log(userId, "TRAINING_RESTORED", "TRAINING_PLAN", planId, null, "DEACTIVATED", "ACTIVE");
            Set<Long> memberIdsR = getCurrentMemberUserIds(planId);
            memberIdsR.add(plan.getCreatorUserId());
            var payload = new LinkedHashMap<String, Object>();
            payload.put("planTitle", plan.getTitle());
            eventPublisher.publishEvent(new NotificationEvent(
                    memberIdsR, userId, "TRAINING_RESTORED",
                    "TRAINING_PLAN", planId, payload));
        }
    }

    @Override
    @Transactional
    public void addProblem(Long planId, AddProblemRequest request, Long userId) {
        TrainingPlan plan = getPlan(planId);
        checkEditPermission(plan, userId);

        if ("ENDED".equals(computeTimeStatus(plan)))
            throw new BusinessException(400, "已结束的计划不能修改题目");

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
        notifyProblemsChanged(plan, userId);
    }

    @Override
    @Transactional
    public void removeProblem(Long planId, Long problemId, Long userId) {
        TrainingPlan plan = getPlan(planId);
        checkEditPermission(plan, userId);
        if ("ENDED".equals(computeTimeStatus(plan)))
            throw new BusinessException(400, "已结束的计划不能修改题目");
        planProblemMapper.delete(new LambdaQueryWrapper<TrainingPlanProblem>()
                .eq(TrainingPlanProblem::getPlanId, planId)
                .eq(TrainingPlanProblem::getProblemId, problemId));
        notifyProblemsChanged(plan, userId);
    }

    @Override
    @Transactional
    public void updateProblems(Long planId, UpdateProblemsRequest request, Long userId) {
        TrainingPlan plan = getPlan(planId);
        checkEditPermission(plan, userId);
        if ("ENDED".equals(computeTimeStatus(plan)))
            throw new BusinessException(400, "已结束的计划不能修改题目");

        List<PlanProblemRequest> reqProblems = request.getProblems();
        if (reqProblems == null) reqProblems = List.of();

        // deduplicate and validate
        Set<Long> reqProblemIds = new LinkedHashSet<>();
        for (PlanProblemRequest ppr : reqProblems) {
            if (ppr.getProblemId() == null || ppr.getProblemId() <= 0) {
                throw new BusinessException(400, "题目ID无效");
            }
            if (!reqProblemIds.add(ppr.getProblemId())) {
                throw new BusinessException(400, "题目ID重复: " + ppr.getProblemId());
            }
        }

        // validate all problems exist and are active
        if (!reqProblemIds.isEmpty()) {
            List<Problem> probs = problemMapper.selectBatchIds(reqProblemIds);
            Map<Long, Problem> probMap = new HashMap<>();
            for (Problem p : probs) probMap.put(p.getId(), p);
            for (Long pid : reqProblemIds) {
                Problem p = probMap.get(pid);
                if (p == null) throw new BusinessException(404, "题目不存在: " + pid);
                if (p.getStatus() == null || p.getStatus() == 0) {
                    throw new BusinessException(400, "停用题目不能加入计划: " + pid);
                }
            }
        }

        // get current problems
        List<TrainingPlanProblem> current = planProblemMapper.selectList(
                new LambdaQueryWrapper<TrainingPlanProblem>()
                        .eq(TrainingPlanProblem::getPlanId, planId)
                        .orderByAsc(TrainingPlanProblem::getSortOrder));
        Map<Long, Integer> currentMap = new LinkedHashMap<>();
        for (TrainingPlanProblem tpp : current) {
            currentMap.put(tpp.getProblemId(), tpp.getSortOrder());
        }

        // compare
        boolean changed = currentMap.size() != reqProblems.size();
        if (!changed) {
            int i = 0;
            for (Long pid : currentMap.keySet()) {
                if (!pid.equals(reqProblems.get(i).getProblemId())) { changed = true; break; }
                i++;
            }
        }

        if (!changed) {
            // check sort orders match
            for (int i = 0; i < reqProblems.size(); i++) {
                PlanProblemRequest ppr = reqProblems.get(i);
                Integer curSort = currentMap.get(ppr.getProblemId());
                int newSort = ppr.getSortOrder() != null ? ppr.getSortOrder() : i;
                if (curSort == null || curSort != newSort) { changed = true; break; }
            }
        }

        if (!changed) return;

        // delete old and insert new
        planProblemMapper.delete(new LambdaQueryWrapper<TrainingPlanProblem>()
                .eq(TrainingPlanProblem::getPlanId, planId));

        for (int i = 0; i < reqProblems.size(); i++) {
            PlanProblemRequest ppr = reqProblems.get(i);
            TrainingPlanProblem tpp = new TrainingPlanProblem();
            tpp.setPlanId(planId);
            tpp.setProblemId(ppr.getProblemId());
            tpp.setSortOrder(ppr.getSortOrder() != null ? ppr.getSortOrder() : i);
            tpp.setRequiredFlag(1);
            try {
                planProblemMapper.insert(tpp);
            } catch (DuplicateKeyException e) {
                throw new BusinessException(409, "题目已在计划中: " + ppr.getProblemId());
            }
        }

        notifyProblemsChanged(plan, userId);
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
        if ("ENDED".equals(computeTimeStatus(plan))) {
            throw new BusinessException(400, "该计划已结束，不接受新成员");
        }

        TrainingPlanMember existing = memberMapper.selectOne(new LambdaQueryWrapper<TrainingPlanMember>()
                .eq(TrainingPlanMember::getPlanId, planId)
                .eq(TrainingPlanMember::getUserId, userId));
        if (existing != null) {
            if (existing.getStatus() != null && existing.getStatus() == 0) {
                // restore removed member - use UpdateWrapper to clear null fields
                memberMapper.update(null, new LambdaUpdateWrapper<TrainingPlanMember>()
                        .eq(TrainingPlanMember::getId, existing.getId())
                        .set(TrainingPlanMember::getStatus, 1)
                        .set(TrainingPlanMember::getRemoveTime, null)
                        .set(TrainingPlanMember::getRemovedBy, null));
            }
            return;
        }

        TrainingPlanMember member = new TrainingPlanMember();
        member.setPlanId(planId);
        member.setUserId(userId);
        try {
            memberMapper.insert(member);
        } catch (DuplicateKeyException e) {
            // concurrent race
        }
    }

    @Override
    @Transactional
    public void removeMember(Long planId, Long memberUserId, Long operatorUserId) {
        TrainingPlan plan = getPlan(planId);
        if (!plan.getCreatorUserId().equals(operatorUserId)) {
            throw new BusinessException(403, "只有创建者才能移除成员");
        }
        if ("PERSONAL".equals(plan.getPlanType())) {
            throw new BusinessException(400, "个人计划不支持移除成员");
        }
        if (plan.getCreatorUserId().equals(memberUserId)) {
            throw new BusinessException(400, "不能移除计划创建者");
        }

        int updated = memberMapper.update(null, new LambdaUpdateWrapper<TrainingPlanMember>()
                .eq(TrainingPlanMember::getPlanId, planId)
                .eq(TrainingPlanMember::getUserId, memberUserId)
                .set(TrainingPlanMember::getStatus, 0)
                .set(TrainingPlanMember::getRemoveTime, LocalDateTime.now())
                .set(TrainingPlanMember::getRemovedBy, operatorUserId));
        if (updated == 0) {
            throw new BusinessException(404, "该成员不在计划中");
        }
        var memberPayload = new LinkedHashMap<String, Object>();
        memberPayload.put("planTitle", plan.getTitle());
        eventPublisher.publishEvent(new NotificationEvent(
                Set.of(memberUserId), operatorUserId, "TRAINING_MEMBER_REMOVED",
                "TRAINING_PLAN", planId, memberPayload));
    }

    @Override
    public List<PublicPlanSummaryResponse> listPublicPlansByUserId(Long userId, int page, int size) {
        page = Math.max(1, page);
        size = Math.max(1, Math.min(size, 100));

        List<TrainingPlanMember> memberships = memberMapper.selectList(
                new LambdaQueryWrapper<TrainingPlanMember>()
                        .eq(TrainingPlanMember::getUserId, userId)
                        .eq(TrainingPlanMember::getStatus, 1));
        if (memberships.isEmpty()) return List.of();
        Set<Long> planIds = memberships.stream().map(TrainingPlanMember::getPlanId).collect(Collectors.toSet());

        LambdaQueryWrapper<TrainingPlan> qw = new LambdaQueryWrapper<TrainingPlan>()
                .in(TrainingPlan::getId, planIds)
                .eq(TrainingPlan::getPlanType, "PUBLIC")
                .eq(TrainingPlan::getIsActive, 1)
                .orderByDesc(TrainingPlan::getCreateTime)
                .orderByDesc(TrainingPlan::getId);

        List<TrainingPlan> plans = planMapper.selectList(qw);
        int offset = (page - 1) * size;
        List<TrainingPlan> pagePlans = plans.stream().skip(offset).limit(size).toList();

        if (pagePlans.isEmpty()) return List.of();

        Set<Long> resultPlanIds = pagePlans.stream().map(TrainingPlan::getId).collect(Collectors.toSet());
        Map<Long, Integer> problemCounts = batchCountProblems(resultPlanIds);
        Map<Long, Integer> memberCounts = batchCountMembers(resultPlanIds);

        return pagePlans.stream().map(p -> {
            PublicPlanSummaryResponse r = new PublicPlanSummaryResponse();
            r.setId(p.getId());
            r.setTitle(p.getTitle());
            r.setStartTime(p.getStartTime());
            r.setEndTime(p.getEndTime());
            r.setTimeStatus(computeTimeStatus(p));
            r.setProblemCount(problemCounts.getOrDefault(p.getId(), 0));
            r.setMemberCount(memberCounts.getOrDefault(p.getId(), 0));
            r.setCreateTime(p.getCreateTime());
            return r;
        }).collect(Collectors.toList());
    }

    @Override
    public int countPublicPlansByUserId(Long userId) {
        List<TrainingPlanMember> memberships = memberMapper.selectList(
                new LambdaQueryWrapper<TrainingPlanMember>()
                        .eq(TrainingPlanMember::getUserId, userId)
                        .eq(TrainingPlanMember::getStatus, 1));
        if (memberships.isEmpty()) return 0;
        Set<Long> planIds = memberships.stream().map(TrainingPlanMember::getPlanId).collect(Collectors.toSet());

        Long cnt = planMapper.selectCount(new LambdaQueryWrapper<TrainingPlan>()
                .in(TrainingPlan::getId, planIds)
                .eq(TrainingPlan::getPlanType, "PUBLIC")
                .eq(TrainingPlan::getIsActive, 1));
        return cnt != null ? cnt.intValue() : 0;
    }

    // ---------- helpers ----------

    private TrainingPlan getPlan(Long planId) {
        TrainingPlan plan = planMapper.selectById(planId);
        if (plan == null) throw new BusinessException(404, "训练计划不存在");
        return plan;
    }

    private boolean isAdminUser(Long userId) {
        AppUser u = userMapper.selectById(userId);
        return u != null && u.getIsAdmin() != null && u.getIsAdmin() == 1;
    }

    private void checkEditPermission(TrainingPlan plan, Long userId) {
        if (plan.getCreatorUserId().equals(userId)) return;
        throw new BusinessException(403, "无权修改该计划");
    }

    private void checkViewPermission(TrainingPlan plan, Long userId) {
        if ("PUBLIC".equals(plan.getPlanType())) return;
        if (plan.getCreatorUserId().equals(userId)) return;
        if (isAdminUser(userId)) return;
        // active member of a deactivated plan can still view
        TrainingPlanMember membership = memberMapper.selectOne(new LambdaQueryWrapper<TrainingPlanMember>()
                .eq(TrainingPlanMember::getPlanId, plan.getId())
                .eq(TrainingPlanMember::getUserId, userId));
        if (membership != null && (membership.getStatus() == null || membership.getStatus() == 1)) return;
        throw new BusinessException(404, "训练计划不存在");
    }

    private Set<Long> getCurrentMemberUserIds(Long planId) {
        return memberMapper.selectList(new LambdaQueryWrapper<TrainingPlanMember>()
                .eq(TrainingPlanMember::getPlanId, planId)
                .eq(TrainingPlanMember::getStatus, 1))
                .stream().map(TrainingPlanMember::getUserId).collect(Collectors.toSet());
    }

    private Map<Long, AppUser> batchLoadUsers(Set<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(AppUser::getId, u -> u, (a, b) -> a));
    }

    private Map<Long, Problem> batchLoadProblems(Set<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return problemMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Problem::getId, p -> p, (a, b) -> a));
    }

    private Map<Long, Integer> batchCountProblems(Set<Long> planIds) {
        if (planIds.isEmpty()) return Map.of();
        Map<Long, Integer> result = new HashMap<>();
        List<TrainingPlanProblem> all = planProblemMapper.selectList(
                new LambdaQueryWrapper<TrainingPlanProblem>().in(TrainingPlanProblem::getPlanId, planIds));
        for (TrainingPlanProblem tpp : all) {
            result.merge(tpp.getPlanId(), 1, Integer::sum);
        }
        return result;
    }

    private Map<Long, Integer> batchCountMembers(Set<Long> planIds) {
        if (planIds.isEmpty()) return Map.of();
        Map<Long, Integer> result = new HashMap<>();
        List<TrainingPlanMember> all = memberMapper.selectList(
                new LambdaQueryWrapper<TrainingPlanMember>().in(TrainingPlanMember::getPlanId, planIds)
                        .eq(TrainingPlanMember::getStatus, 1));
        for (TrainingPlanMember m : all) {
            result.merge(m.getPlanId(), 1, Integer::sum);
        }
        return result;
    }

    private void applyTimeStatusFilter(LambdaQueryWrapper<TrainingPlan> qw, String timeStatus) {
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

    private PlanDetailResponse toDetailResponse(TrainingPlan plan, Long userId) {
        PlanDetailResponse r = new PlanDetailResponse();
        r.setId(plan.getId());
        r.setTitle(plan.getTitle());
        r.setDescription(plan.getDescription());
        r.setPlanType(plan.getPlanType());
        r.setActive(plan.getIsActive() != null && plan.getIsActive() == 1);
        r.setDeactivationSource(plan.getDeactivationSource());
        r.setDeactivationReason(plan.getDeactivationReason());
        r.setCreatorUserId(plan.getCreatorUserId());
        r.setStartTime(plan.getStartTime());
        r.setEndTime(plan.getEndTime());
        r.setTimeStatus(computeTimeStatus(plan));
        r.setCreateTime(plan.getCreateTime());
        r.setUpdateTime(plan.getUpdateTime());

        boolean isAdmin = isAdminUser(userId);
        boolean isCreator = plan.getCreatorUserId().equals(userId);

        AppUser creator = userMapper.selectById(plan.getCreatorUserId());
        if (creator != null) {
            r.setCreatorUsername(creator.getUsername());
            r.setCreatorNickname(creator.getNickname());
        }

        // Members (active only)
        List<TrainingPlanMember> members = memberMapper.selectList(
                new LambdaQueryWrapper<TrainingPlanMember>()
                        .eq(TrainingPlanMember::getPlanId, plan.getId())
                        .eq(TrainingPlanMember::getStatus, 1)
                        .orderByAsc(TrainingPlanMember::getJoinTime));

        boolean joined = members.stream().anyMatch(m -> m.getUserId().equals(userId));
        r.setMemberCount(members.size());
        r.setJoined(joined);
        r.setCreator(isCreator);

        // Permissions
        boolean isActive = plan.getIsActive() != null && plan.getIsActive() == 1;
        boolean isEnded = "ENDED".equals(computeTimeStatus(plan));
        r.setCanEdit(isCreator);
        r.setCanJoin("PUBLIC".equals(plan.getPlanType()) && !joined && isActive && !isEnded);
        r.setCanRemoveMembers(isCreator && "PUBLIC".equals(plan.getPlanType()) && members.size() > 1);
        r.setCanDeactivate((isCreator || isAdmin) && isActive);
        r.setCanRestore((isCreator || isAdmin) && !isActive
                && !(isCreator && !isAdmin && "ADMIN".equals(plan.getDeactivationSource())));

        // Batch load member user info
        Set<Long> memberUserIds = members.stream().map(TrainingPlanMember::getUserId).collect(Collectors.toSet());
        Map<Long, AppUser> memberUserMap = batchLoadUsers(memberUserIds);

        // Load progress stats for all members
        List<TrainingPlanProblem> tpps = planProblemMapper.selectList(
                new LambdaQueryWrapper<TrainingPlanProblem>()
                        .eq(TrainingPlanProblem::getPlanId, plan.getId())
                        .orderByAsc(TrainingPlanProblem::getSortOrder));
        Set<Long> problemIds = tpps.stream().map(TrainingPlanProblem::getProblemId).collect(Collectors.toSet());
        Map<Long, Problem> probMap = batchLoadProblems(problemIds);

        // Per-user progress: batch load all statuses for all members
        Map<Long, List<UserProblemStatus>> memberStatusMap = new HashMap<>();
        if (!memberUserIds.isEmpty() && !problemIds.isEmpty()) {
            List<UserProblemStatus> allStatuses = upsMapper.selectList(
                    new LambdaQueryWrapper<UserProblemStatus>()
                            .in(UserProblemStatus::getUserId, memberUserIds)
                            .in(UserProblemStatus::getProblemId, problemIds));
            for (UserProblemStatus s : allStatuses) {
                memberStatusMap.computeIfAbsent(s.getUserId(), k -> new ArrayList<>()).add(s);
            }
        }

        // Compute progress per member with deadline/current field separation
        boolean hasRequired = tpps.stream().anyMatch(tpp -> tpp.getRequiredFlag() == 1);
        LocalDateTime deadline = plan.getEndTime();

        List<PlanMemberResponse> memberList = new ArrayList<>();
        for (TrainingPlanMember m : members) {
            AppUser mu = memberUserMap.get(m.getUserId());
            List<UserProblemStatus> sts = memberStatusMap.getOrDefault(m.getUserId(), List.of());
            Map<Long, UserProblemStatus> stsMap = sts.stream()
                .collect(Collectors.toMap(UserProblemStatus::getProblemId, s -> s));

            int completedAll = 0, reqCompleted = 0, reqTotal = 0, deadlineReqCompleted = 0;
            LocalDateTime currentLastAc = null, deadlineLastAc = null, latestReqAc = null;
            boolean allReqCompleted = true;

            for (TrainingPlanProblem tpp : tpps) {
                boolean isReq = hasRequired ? tpp.getRequiredFlag() == 1 : true;
                if (isReq) reqTotal++;

                UserProblemStatus s = stsMap.get(tpp.getProblemId());
                boolean isAccepted = s != null && s.getStatus() != null && s.getStatus() == 2;
                if (isAccepted) completedAll++;

                if (isReq) {
                    if (isAccepted) {
                        reqCompleted++;
                        if (s.getFirstAcTime() != null) {
                            if (currentLastAc == null || s.getFirstAcTime().isAfter(currentLastAc))
                                currentLastAc = s.getFirstAcTime();
                            if (latestReqAc == null || s.getFirstAcTime().isAfter(latestReqAc))
                                latestReqAc = s.getFirstAcTime();
                            if (deadline == null || !s.getFirstAcTime().isAfter(deadline)) {
                                deadlineReqCompleted++;
                                if (deadlineLastAc == null || s.getFirstAcTime().isAfter(deadlineLastAc))
                                    deadlineLastAc = s.getFirstAcTime();
                            }
                        } else {
                            deadlineReqCompleted++;
                        }
                    } else {
                        allReqCompleted = false;
                    }
                }
            }

            LocalDateTime currentCompletedAt = allReqCompleted && reqCompleted == reqTotal ? latestReqAc : null;
            boolean allBeforeDeadline = deadline == null ? allReqCompleted && reqCompleted == reqTotal : false;
            if (!allBeforeDeadline && deadline != null && allReqCompleted && reqCompleted == reqTotal) {
                allBeforeDeadline = true;
                for (TrainingPlanProblem tpp : tpps) {
                    boolean isReq = hasRequired ? tpp.getRequiredFlag() == 1 : true;
                    if (!isReq) continue;
                    UserProblemStatus s = stsMap.get(tpp.getProblemId());
                    if (s != null && s.getFirstAcTime() != null && s.getFirstAcTime().isAfter(deadline)) {
                        allBeforeDeadline = false;
                        break;
                    }
                }
            }

            PlanMemberResponse mi = new PlanMemberResponse();
            mi.setUserId(m.getUserId());
            mi.setUsername(mu != null ? mu.getUsername() : null);
            mi.setNickname(mu != null ? mu.getNickname() : null);
            mi.setAvatarUrl(mu != null ? mu.getAvatarUrl() : null);
            mi.setJoinTime(m.getJoinTime());
            mi.setCreator(m.getUserId().equals(plan.getCreatorUserId()));
            mi.setCompletedCount(completedAll);
            mi.setTotalCount(tpps.size());
            mi.setRequiredCompletedCount(reqCompleted);
            mi.setRequiredTotal(reqTotal);
            mi.setCurrentLastAcceptedTime(currentLastAc);
            mi.setDeadlineLastAcceptedTime(deadlineLastAc);
            mi.setCurrentCompletedAt(currentCompletedAt);
            mi.setDeadlineCompletedAt(allBeforeDeadline ? currentCompletedAt : null);
            mi.setDeadlineCompletedCount(deadlineReqCompleted);
            memberList.add(mi);
        }

        // Rank by deadlineCompletedCount DESC → deadlineLastAcceptedTime NULLS LAST ASC → userId ASC
        List<PlanMemberResponse> sorted = new ArrayList<>(memberList);
        sorted.sort((a, b) -> {
            int cmp = Integer.compare(b.getDeadlineCompletedCount(), a.getDeadlineCompletedCount());
            if (cmp != 0) return cmp;
            LocalDateTime ta = a.getDeadlineLastAcceptedTime();
            LocalDateTime tb = b.getDeadlineLastAcceptedTime();
            if (ta != null && tb != null) return ta.compareTo(tb);
            if (ta == null && tb == null) return Long.compare(a.getUserId(), b.getUserId());
            return ta == null ? 1 : -1;
        });
        Map<Long, Integer> rankMap = new HashMap<>();
        for (int i = 0; i < sorted.size(); i++) {
            rankMap.put(sorted.get(i).getUserId(), i + 1);
        }
        for (PlanMemberResponse mi : memberList) {
            mi.setRank((long) rankMap.get(mi.getUserId()));
        }

        // completionOrder: only for pre-deadline completers
        List<PlanMemberResponse> completedOrdered = memberList.stream()
            .filter(mi -> mi.getDeadlineCompletedAt() != null)
            .sorted(Comparator.comparing(PlanMemberResponse::getDeadlineCompletedAt)
                .thenComparingLong(PlanMemberResponse::getUserId))
            .collect(Collectors.toList());
        Map<Long, Integer> completionOrderMap = new HashMap<>();
        for (int i = 0; i < completedOrdered.size(); i++) {
            completionOrderMap.put(completedOrdered.get(i).getUserId(), i + 1);
        }
        for (PlanMemberResponse mi : memberList) {
            mi.setCompletionOrder(completionOrderMap.get(mi.getUserId()));
        }

        r.setMembers(memberList);

        // Problems with my status
        List<PlanProblemResponse> problems = new ArrayList<>();
        Map<Long, UserProblemStatus> myStatusMap = joined ? memberStatusMap.getOrDefault(userId, List.of()).stream()
                .collect(Collectors.toMap(UserProblemStatus::getProblemId, s -> s)) : Map.of();

        for (TrainingPlanProblem tpp : tpps) {
            PlanProblemResponse pp = new PlanProblemResponse();
            pp.setId(tpp.getId());
            pp.setProblemId(tpp.getProblemId());
            pp.setSortOrder(tpp.getSortOrder());
            pp.setRequired(tpp.getRequiredFlag() == 1);
            Problem prob = probMap.get(tpp.getProblemId());
            if (prob != null) {
                pp.setProblemTitle(prob.getTitle());
                pp.setPlatform(prob.getPlatform());
                pp.setDifficulty(prob.getDifficulty());
                pp.setProblemActive(prob.getStatus() != null && prob.getStatus() == 1);
            }
            UserProblemStatus myS = myStatusMap.get(tpp.getProblemId());
            if (myS != null) {
                pp.setMyStatus(ProblemStatus.fromCode(myS.getStatus()).name());
                pp.setPerformanceNote(myS.getPerformanceNote());
            }
            problems.add(pp);
        }
        r.setProblems(problems);
        r.setProblemCount(problems.size());

        // My progress summary
        int myCompleted = 0, reqCompleted = 0, reqTotal = 0;
        Map<Long, UserProblemStatus> myStsMap = joined ? memberStatusMap.getOrDefault(userId, List.of()).stream()
                .collect(Collectors.toMap(UserProblemStatus::getProblemId, s -> s)) : Map.of();
        for (TrainingPlanProblem tpp : tpps) {
            boolean isReq = hasRequired ? tpp.getRequiredFlag() == 1 : true;
            if (isReq) reqTotal++;
            UserProblemStatus s = myStsMap.get(tpp.getProblemId());
            if (s != null && s.getStatus() != null && s.getStatus() == 2) {
                myCompleted++;
                if (isReq) reqCompleted++;
            }
        }
        ProgressSummaryResponse myProgress = new ProgressSummaryResponse();
        myProgress.setRequiredCompletedCount(reqCompleted);
        myProgress.setRequiredTotal(reqTotal);
        myProgress.setOptionalCompletedCount(myCompleted - reqCompleted);
        myProgress.setOptionalTotal(tpps.size() - reqTotal);
        r.setMyProgress(myProgress);

        return r;
    }

    private void validateAndInsertProblems(Long planId, List<Long> problemIds) {
        Set<Long> seen = new LinkedHashSet<>();
        for (Long pid : problemIds) {
            if (pid == null || pid <= 0) throw new BusinessException(400, "题目ID无效");
            if (!seen.add(pid)) throw new BusinessException(400, "题目ID重复: " + pid);
        }
        List<Problem> probs = problemMapper.selectBatchIds(seen);
        Map<Long, Problem> probMap = new HashMap<>();
        for (Problem p : probs) probMap.put(p.getId(), p);
        int sort = 0;
        for (Long pid : problemIds) {
            Problem p = probMap.get(pid);
            if (p == null) throw new BusinessException(404, "题目不存在: " + pid);
            if (p.getStatus() == null || p.getStatus() == 0) {
                throw new BusinessException(400, "停用题目不能加入计划: " + pid);
            }
            TrainingPlanProblem tpp = new TrainingPlanProblem();
            tpp.setPlanId(planId);
            tpp.setProblemId(pid);
            tpp.setSortOrder(sort++);
            tpp.setRequiredFlag(1);
            planProblemMapper.insert(tpp);
        }
    }

    private void notifyProblemsChanged(TrainingPlan plan, Long userId) {
        if (!"PUBLIC".equals(plan.getPlanType())) return;
        Set<Long> memberIds = getCurrentMemberUserIds(plan.getId());
        var payload = new LinkedHashMap<String, Object>();
        payload.put("planTitle", plan.getTitle());
        eventPublisher.publishEvent(new NotificationEvent(
                memberIds, userId, "TRAINING_PROBLEMS_CHANGED",
                "TRAINING_PLAN", plan.getId(), payload));
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
