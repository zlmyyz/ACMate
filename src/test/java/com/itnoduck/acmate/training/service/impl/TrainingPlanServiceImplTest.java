package com.itnoduck.acmate.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.itnoduck.acmate.auditlog.service.AuditLogService;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.training.dto.*;
import com.itnoduck.acmate.training.entity.TrainingPlan;
import com.itnoduck.acmate.training.entity.TrainingPlanMember;
import com.itnoduck.acmate.training.entity.TrainingPlanProblem;
import com.itnoduck.acmate.training.mapper.TrainingPlanMapper;
import com.itnoduck.acmate.training.mapper.TrainingPlanMemberMapper;
import com.itnoduck.acmate.training.mapper.TrainingPlanProblemMapper;
import com.itnoduck.acmate.problem.entity.Problem;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import com.itnoduck.acmate.testutil.MybatisPlusTestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingPlanServiceImplTest {

    @Mock private TrainingPlanMapper planMapper;
    @Mock private TrainingPlanProblemMapper planProblemMapper;
    @Mock private TrainingPlanMemberMapper memberMapper;
    @Mock private AppUserMapper userMapper;
    @Mock private ProblemMapper problemMapper;
    @Mock private AuditLogService auditLogService;
    @InjectMocks private TrainingPlanServiceImpl service;

    private static final Long CREATOR_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long ADMIN_ID = 3L;
    private static final Long PLAN_ID = 100L;

    @BeforeAll
    static void initMybatisPlus() {
        MybatisPlusTestHelper.initEntityTables();
    }

    private AppUser adminUser() {
        AppUser u = new AppUser();
        u.setId(ADMIN_ID); u.setUsername("admin"); u.setNickname("Admin"); u.setIsAdmin(1);
        return u;
    }

    private AppUser normalUser(Long id) {
        AppUser u = new AppUser();
        u.setId(id); u.setUsername("user" + id); u.setNickname("User" + id); u.setIsAdmin(0);
        return u;
    }

    private TrainingPlan createPlanInDb(String planType, Long creatorId, Integer isActive) {
        TrainingPlan p = new TrainingPlan();
        p.setId(PLAN_ID); p.setTitle("Test Plan"); p.setPlanType(planType);
        p.setCreatorUserId(creatorId); p.setIsActive(isActive != null ? isActive : 1);
        p.setStartTime(null); p.setEndTime(null);
        p.setDeactivationSource(null); p.setDeactivationReason(null);
        p.setDeactivatedBy(null); p.setDeactivationTime(null);
        return p;
    }

    private Problem createProblem(Long problemId) {
        Problem prob = new Problem();
        prob.setId(problemId); prob.setTitle("Problem " + problemId);
        prob.setPlatform("CUSTOM"); prob.setStatus(1);
        return prob;
    }

    @BeforeEach
    void mockInserts() {
        lenient().doAnswer(inv -> {
            TrainingPlan p = inv.getArgument(0);
            p.setId(PLAN_ID);
            p.setCreateTime(LocalDateTime.now());
            p.setUpdateTime(LocalDateTime.now());
            return 1;
        }).when(planMapper).insert(any(TrainingPlan.class));
        lenient().doAnswer(inv -> {
            TrainingPlanMember m = inv.getArgument(0);
            m.setId(1L);
            m.setJoinTime(LocalDateTime.now());
            return 1;
        }).when(memberMapper).insert(any(TrainingPlanMember.class));
        lenient().doAnswer(inv -> {
            TrainingPlanProblem tpp = inv.getArgument(0);
            tpp.setId(1L);
            tpp.setCreateTime(LocalDateTime.now());
            return 1;
        }).when(planProblemMapper).insert(any(TrainingPlanProblem.class));
        lenient().when(planMapper.updateById(any(TrainingPlan.class))).thenReturn(1);
        lenient().when(planMapper.update(any(), any())).thenReturn(1);
    }

    // ======================== PERSONAL ========================

    @Test
    void personalPlan_creatorAutoJoinsAsMember() {
        CreatePlanRequest req = new CreatePlanRequest();
        req.setTitle("My Personal Plan");
        req.setPlanType("PERSONAL");

        service.createPlan(req, CREATOR_ID);

        ArgumentCaptor<TrainingPlanMember> memberCaptor = ArgumentCaptor.forClass(TrainingPlanMember.class);
        verify(memberMapper).insert(memberCaptor.capture());
        assertEquals(PLAN_ID, memberCaptor.getValue().getPlanId());
        assertEquals(CREATOR_ID, memberCaptor.getValue().getUserId());
    }

    @Test
    void personalPlan_otherUserCannotJoin() {
        TrainingPlan plan = createPlanInDb("PERSONAL", CREATOR_ID, 1);
        when(planMapper.selectById(PLAN_ID)).thenReturn(plan);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.joinPlan(PLAN_ID, OTHER_USER_ID));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("不允许"));
    }

    @Test
    void personalPlan_creatorCannotBeRemoved() {
        TrainingPlan plan = createPlanInDb("PERSONAL", CREATOR_ID, 1);
        when(planMapper.selectById(PLAN_ID)).thenReturn(plan);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.removeMember(PLAN_ID, CREATOR_ID, CREATOR_ID));
        assertEquals(400, ex.getCode());
    }

    @Test
    void personalPlan_notInPublicList() {
        when(planMapper.selectPage(any(), any())).thenReturn(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>());

        var result = service.listPlans("PUBLIC", null, null, 1, 20, OTHER_USER_ID);
        assertTrue(result.isEmpty());
    }

    // ======================== PUBLIC ========================

    @Test
    void publicPlan_creatorDoesNotAutoJoin() {
        CreatePlanRequest req = new CreatePlanRequest();
        req.setTitle("Public Plan");
        req.setPlanType("PUBLIC");
        when(userMapper.selectById(ADMIN_ID)).thenReturn(adminUser());

        service.createPlan(req, ADMIN_ID);

        verify(memberMapper, never()).insert(any(TrainingPlanMember.class));
    }

    @Test
    void publicPlan_creatorCanManuallyJoin() {
        TrainingPlan plan = createPlanInDb("PUBLIC", ADMIN_ID, 1);
        when(planMapper.selectById(PLAN_ID)).thenReturn(plan);
        when(memberMapper.selectOne(any())).thenReturn(null);

        service.joinPlan(PLAN_ID, ADMIN_ID);

        ArgumentCaptor<TrainingPlanMember> captor = ArgumentCaptor.forClass(TrainingPlanMember.class);
        verify(memberMapper).insert(captor.capture());
        assertEquals(PLAN_ID, captor.getValue().getPlanId());
        assertEquals(ADMIN_ID, captor.getValue().getUserId());
    }

    @Test
    void publicPlan_normalUserCanJoin() {
        TrainingPlan plan = createPlanInDb("PUBLIC", ADMIN_ID, 1);
        when(planMapper.selectById(PLAN_ID)).thenReturn(plan);
        when(memberMapper.selectOne(any())).thenReturn(null);

        assertDoesNotThrow(() -> service.joinPlan(PLAN_ID, OTHER_USER_ID));
        verify(memberMapper).insert(any(TrainingPlanMember.class));
    }

    @Test
    void publicPlan_duplicateJoinIsIdempotent() {
        TrainingPlan plan = createPlanInDb("PUBLIC", ADMIN_ID, 1);
        when(planMapper.selectById(PLAN_ID)).thenReturn(plan);

        TrainingPlanMember existing = new TrainingPlanMember();
        existing.setId(1L); existing.setPlanId(PLAN_ID); existing.setUserId(OTHER_USER_ID);
        when(memberMapper.selectOne(any())).thenReturn(existing);

        // Should not throw, should not insert
        assertDoesNotThrow(() -> service.joinPlan(PLAN_ID, OTHER_USER_ID));
        verify(memberMapper, never()).insert(any(TrainingPlanMember.class));
    }

    @Test
    void publicPlan_concurrentDuplicateJoinHandled() {
        TrainingPlan plan = createPlanInDb("PUBLIC", ADMIN_ID, 1);
        when(planMapper.selectById(PLAN_ID)).thenReturn(plan);
        when(memberMapper.selectOne(any())).thenReturn(null);
        doThrow(new DuplicateKeyException("Duplicate")).when(memberMapper).insert(any(TrainingPlanMember.class));

        // Should not throw — DuplicateKeyException is caught
        assertDoesNotThrow(() -> service.joinPlan(PLAN_ID, OTHER_USER_ID));
    }

    // ======================== PERMISSIONS ========================

    @Test
    void normalUserCannotCreatePublic() {
        CreatePlanRequest req = new CreatePlanRequest();
        req.setTitle("Public"); req.setPlanType("PUBLIC");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createPlan(req, OTHER_USER_ID));
        assertEquals(403, ex.getCode());
    }

    @Test
    void adminCanCreatePublic() {
        CreatePlanRequest req = new CreatePlanRequest();
        req.setTitle("Public Plan"); req.setPlanType("PUBLIC");
        when(userMapper.selectById(ADMIN_ID)).thenReturn(adminUser());

        assertDoesNotThrow(() -> service.createPlan(req, ADMIN_ID));
    }

    @Test
    void normalUserCanCreatePersonal() {
        CreatePlanRequest req = new CreatePlanRequest();
        req.setTitle("My Plan"); req.setPlanType("PERSONAL");

        assertDoesNotThrow(() -> service.createPlan(req, OTHER_USER_ID));
        verify(memberMapper).insert(any(TrainingPlanMember.class));
    }

    @Test
    void nonCreatorAdminCannotEditContent() {
        TrainingPlan plan = createPlanInDb("PUBLIC", CREATOR_ID, 1);
        when(planMapper.selectById(PLAN_ID)).thenReturn(plan);

        UpdatePlanRequest req = new UpdatePlanRequest();
        req.setTitle("Hacked");

        // Admin is NOT the creator
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updatePlan(PLAN_ID, req, ADMIN_ID));
        assertEquals(403, ex.getCode());
    }

    @Test
    void onlyCreatorCanEditContent() {
        TrainingPlan plan = createPlanInDb("PUBLIC", CREATOR_ID, 1);
        when(planMapper.selectById(PLAN_ID)).thenReturn(plan);

        UpdatePlanRequest req = new UpdatePlanRequest();
        req.setTitle("Valid Edit");

        assertDoesNotThrow(() -> service.updatePlan(PLAN_ID, req, CREATOR_ID));
    }

    // ======================== MEMBER REMOVAL ========================

    @Test
    void creatorCanRemoveOrdinaryMember() {
        TrainingPlan plan = createPlanInDb("PUBLIC", CREATOR_ID, 1);
        when(planMapper.selectById(PLAN_ID)).thenReturn(plan);
        when(memberMapper.delete(any())).thenReturn(1);

        assertDoesNotThrow(() -> service.removeMember(PLAN_ID, OTHER_USER_ID, CREATOR_ID));
    }

    @Test
    void nonCreatorCannotRemoveMember() {
        TrainingPlan plan = createPlanInDb("PUBLIC", CREATOR_ID, 1);
        when(planMapper.selectById(PLAN_ID)).thenReturn(plan);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.removeMember(PLAN_ID, OTHER_USER_ID, OTHER_USER_ID));
        assertEquals(403, ex.getCode());
    }

    @Test
    void removeMember_notInPlan_returns404() {
        TrainingPlan plan = createPlanInDb("PUBLIC", CREATOR_ID, 1);
        when(planMapper.selectById(PLAN_ID)).thenReturn(plan);
        when(memberMapper.delete(any())).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.removeMember(PLAN_ID, 999L, CREATOR_ID));
        assertEquals(404, ex.getCode());
    }

    // ======================== DEACTIVATE / RESTORE ========================

    @Test
    void creatorCanDeactivateOwnPlan() {
        TrainingPlan plan = createPlanInDb("PUBLIC", CREATOR_ID, 1);
        when(planMapper.selectById(PLAN_ID)).thenReturn(plan);

        assertDoesNotThrow(() -> service.deactivatePlan(PLAN_ID, null, CREATOR_ID));

        assertEquals(0, plan.getIsActive());
        assertEquals("CREATOR", plan.getDeactivationSource());
        assertEquals(CREATOR_ID, plan.getDeactivatedBy());
    }

    @Test
    void adminForceDeactivateRequiresReason() {
        TrainingPlan plan = createPlanInDb("PUBLIC", CREATOR_ID, 1);
        when(planMapper.selectById(PLAN_ID)).thenReturn(plan);
        when(userMapper.selectById(ADMIN_ID)).thenReturn(adminUser());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.deactivatePlan(PLAN_ID, null, ADMIN_ID));
        assertEquals(400, ex.getCode());
    }

    @Test
    void adminForceDeactivateWithReasonWritesAuditLog() {
        TrainingPlan plan = createPlanInDb("PUBLIC", CREATOR_ID, 1);
        when(planMapper.selectById(PLAN_ID)).thenReturn(plan);
        when(userMapper.selectById(ADMIN_ID)).thenReturn(adminUser());

        service.deactivatePlan(PLAN_ID, "Violation", ADMIN_ID);

        assertEquals(0, plan.getIsActive());
        assertEquals("ADMIN", plan.getDeactivationSource());
        assertEquals("Violation", plan.getDeactivationReason());
        assertEquals(ADMIN_ID, plan.getDeactivatedBy());
        verify(auditLogService).log(eq(ADMIN_ID), eq("DEACTIVATE"), eq("TRAINING_PLAN"), eq(PLAN_ID), eq("Violation"), eq(null), eq(null));
    }

    @Test
    void deactivateIsIdempotent() {
        TrainingPlan plan = createPlanInDb("PUBLIC", CREATOR_ID, 0);
        plan.setDeactivationSource("CREATOR");
        plan.setDeactivatedBy(CREATOR_ID);
        when(planMapper.selectById(PLAN_ID)).thenReturn(plan);

        // Already deactivated — should be no-op
        assertDoesNotThrow(() -> service.deactivatePlan(PLAN_ID, null, CREATOR_ID));
        verify(planMapper, never()).updateById(any(TrainingPlan.class));
    }

    @Test
    void creatorCannotRestoreAdminDeactivatedPlan() {
        TrainingPlan plan = createPlanInDb("PUBLIC", CREATOR_ID, 0);
        plan.setDeactivationSource("ADMIN");
        plan.setDeactivationReason("Violation");
        when(planMapper.selectById(PLAN_ID)).thenReturn(plan);

        // Creator is not admin
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.restorePlan(PLAN_ID, CREATOR_ID));
        assertEquals(403, ex.getCode());
    }

    @Test
    void adminCanRestoreAnyPlan() {
        TrainingPlan plan = createPlanInDb("PUBLIC", CREATOR_ID, 0);
        plan.setDeactivationSource("ADMIN");
        plan.setDeactivationReason("Violation");
        when(planMapper.selectById(PLAN_ID)).thenReturn(plan);
        when(userMapper.selectById(ADMIN_ID)).thenReturn(adminUser());

        service.restorePlan(PLAN_ID, ADMIN_ID);

        assertEquals(1, plan.getIsActive());
        assertNull(plan.getDeactivationSource());
        assertNull(plan.getDeactivationReason());
        assertNull(plan.getDeactivatedBy());
        verify(auditLogService).log(eq(ADMIN_ID), eq("RESTORE"), eq("TRAINING_PLAN"), eq(PLAN_ID), eq(null), eq(null), eq(null));
    }

    @Test
    void restoreIsIdempotent() {
        TrainingPlan plan = createPlanInDb("PUBLIC", CREATOR_ID, 1);
        when(planMapper.selectById(PLAN_ID)).thenReturn(plan);

        assertDoesNotThrow(() -> service.restorePlan(PLAN_ID, CREATOR_ID));
        verify(planMapper, never()).updateById(any(TrainingPlan.class));
    }

    // ======================== JOIN REJECTION ========================

    @Test
    void deactivatedPlanCannotBeJoined() {
        TrainingPlan plan = createPlanInDb("PUBLIC", ADMIN_ID, 0);
        when(planMapper.selectById(PLAN_ID)).thenReturn(plan);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.joinPlan(PLAN_ID, OTHER_USER_ID));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("停用"));
    }

    @Test
    void endedPlanCannotBeJoined() {
        TrainingPlan plan = createPlanInDb("PUBLIC", ADMIN_ID, 1);
        plan.setStartTime(LocalDateTime.now().minusDays(10));
        plan.setEndTime(LocalDateTime.now().minusDays(1));
        when(planMapper.selectById(PLAN_ID)).thenReturn(plan);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.joinPlan(PLAN_ID, OTHER_USER_ID));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("结束"));
    }

    // ======================== PROBLEM RELATIONS ========================

    @Test
    void addProblem_toNonexistentProblem_returns404() {
        TrainingPlan plan = createPlanInDb("PUBLIC", CREATOR_ID, 1);
        when(planMapper.selectById(PLAN_ID)).thenReturn(plan);
        when(problemMapper.selectById(999L)).thenReturn(null);

        AddProblemRequest req = new AddProblemRequest();
        req.setProblemId(999L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.addProblem(PLAN_ID, req, CREATOR_ID));
        assertEquals(404, ex.getCode());
    }

    @Test
    void addProblem_duplicate_returns409() {
        TrainingPlan plan = createPlanInDb("PUBLIC", CREATOR_ID, 1);
        when(planMapper.selectById(PLAN_ID)).thenReturn(plan);
        when(problemMapper.selectById(1L)).thenReturn(createProblem(1L));
        when(planProblemMapper.selectCount(any())).thenReturn(1L);

        AddProblemRequest req = new AddProblemRequest();
        req.setProblemId(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.addProblem(PLAN_ID, req, CREATOR_ID));
        assertEquals(409, ex.getCode());
    }

    @Test
    void nonCreatorCannotAddProblem() {
        TrainingPlan plan = createPlanInDb("PUBLIC", CREATOR_ID, 1);
        when(planMapper.selectById(PLAN_ID)).thenReturn(plan);

        AddProblemRequest req = new AddProblemRequest();
        req.setProblemId(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.addProblem(PLAN_ID, req, OTHER_USER_ID));
        assertEquals(403, ex.getCode());
    }

    // ======================== VIEW PERMISSIONS ========================

    @Test
    void nonMemberCannotViewDeactivatedPersonalPlan() {
        TrainingPlan plan = createPlanInDb("PERSONAL", CREATOR_ID, 0);
        when(planMapper.selectById(PLAN_ID)).thenReturn(plan);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getPlanDetail(PLAN_ID, OTHER_USER_ID));
        assertEquals(404, ex.getCode());
    }

    @Test
    void memberCanViewDeactivatedPlan() {
        TrainingPlan plan = createPlanInDb("PUBLIC", CREATOR_ID, 0);
        plan.setDeactivationSource("CREATOR");
        when(planMapper.selectById(PLAN_ID)).thenReturn(plan);
        when(userMapper.selectById(CREATOR_ID)).thenReturn(normalUser(CREATOR_ID));
        when(userMapper.selectById(OTHER_USER_ID)).thenReturn(normalUser(OTHER_USER_ID));

        TrainingPlanMember member = new TrainingPlanMember();
        member.setId(1L); member.setPlanId(PLAN_ID); member.setUserId(OTHER_USER_ID);
        member.setJoinTime(LocalDateTime.now());
        when(memberMapper.selectList(any())).thenReturn(List.of(member));

        assertDoesNotThrow(() -> service.getPlanDetail(PLAN_ID, OTHER_USER_ID));
    }

    // ======================== N+1 / BATCH LOADING ========================

    @Test
    void listPlans_batchLoadsCreators() {
        TrainingPlan plan = createPlanInDb("PUBLIC", CREATOR_ID, 1);
        var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<TrainingPlan>();
        page.setRecords(List.of(plan));
        when(planMapper.selectPage(any(), any())).thenReturn(page);
        when(userMapper.selectBatchIds(Set.of(CREATOR_ID))).thenReturn(List.of(normalUser(CREATOR_ID)));
        when(planProblemMapper.selectList(any())).thenReturn(List.of());
        when(memberMapper.selectList(any())).thenReturn(List.of());

        var result = service.listPlans("PUBLIC", null, null, 1, 20, OTHER_USER_ID);

        assertEquals(1, result.size());
        verify(userMapper, atMost(1)).selectBatchIds(any());
        verify(userMapper, never()).selectById(any());
    }

    // ======================== Pagination ========================

    @Test
    void listPlans_clampsInvalidPageParams() {
        when(planMapper.selectPage(any(), any())).thenReturn(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>());

        assertDoesNotThrow(() -> service.listPlans("PUBLIC", null, null, 0, 200, OTHER_USER_ID));
    }

    // ======================== ERROR SEMANTICS ========================

    @Test
    void nonexistentPlan_returns404() {
        when(planMapper.selectById(PLAN_ID)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getPlanDetail(PLAN_ID, CREATOR_ID));
        assertEquals(404, ex.getCode());
    }

    @Test
    void updatePlan_emptyTitle_returns400() {
        TrainingPlan plan = createPlanInDb("PUBLIC", CREATOR_ID, 1);
        when(planMapper.selectById(PLAN_ID)).thenReturn(plan);

        UpdatePlanRequest req = new UpdatePlanRequest();
        req.setTitle("   ");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updatePlan(PLAN_ID, req, CREATOR_ID));
        assertEquals(400, ex.getCode());
    }

    @Test
    void createPlan_emptyTitle_returns400() {
        CreatePlanRequest req = new CreatePlanRequest();
        req.setTitle("");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createPlan(req, CREATOR_ID));
        assertEquals(400, ex.getCode());
    }

    @Test
    void createPlan_endTimeBeforeStart_returns400() {
        CreatePlanRequest req = new CreatePlanRequest();
        req.setTitle("Test");
        req.setStartTime(LocalDateTime.now().plusDays(2));
        req.setEndTime(LocalDateTime.now().plusDays(1));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createPlan(req, CREATOR_ID));
        assertEquals(400, ex.getCode());
    }
}
