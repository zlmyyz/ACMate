package com.itnoduck.acmate.training.service.impl;

import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.problem.entity.Problem;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import com.itnoduck.acmate.training.entity.TrainingPlan;
import com.itnoduck.acmate.training.entity.TrainingPlanMember;
import com.itnoduck.acmate.training.entity.TrainingPlanProblem;
import com.itnoduck.acmate.training.entity.UserProblemStatus;
import com.itnoduck.acmate.training.mapper.TrainingPlanMapper;
import com.itnoduck.acmate.training.mapper.TrainingPlanMemberMapper;
import com.itnoduck.acmate.training.mapper.TrainingPlanProblemMapper;
import com.itnoduck.acmate.training.mapper.UserProblemStatusMapper;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import com.itnoduck.acmate.testutil.MybatisPlusTestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingPlanProgressServiceImplTest {

    @Mock private TrainingPlanMapper planMapper;
    @Mock private TrainingPlanProblemMapper planProblemMapper;
    @Mock private TrainingPlanMemberMapper memberMapper;
    @Mock private AppUserMapper userMapper;
    @Mock private ProblemMapper problemMapper;
    @Mock private UserProblemStatusMapper upsMapper;
    @InjectMocks private TrainingPlanProgressServiceImpl service;

    private static final Long PLAN_ID = 100L;
    private static final Long USER_ID = 1L;
    private static final Long PROBLEM_ID = 10L;

    @BeforeAll
    static void initMybatisPlus() {
        MybatisPlusTestHelper.initEntityTables();
    }

    private TrainingPlan activePlan() {
        TrainingPlan p = new TrainingPlan();
        p.setId(PLAN_ID); p.setTitle("Plan"); p.setPlanType("PUBLIC");
        p.setCreatorUserId(2L); p.setIsActive(1);
        return p;
    }

    private TrainingPlanMember activeMember() {
        TrainingPlanMember m = new TrainingPlanMember();
        m.setId(1L); m.setPlanId(PLAN_ID); m.setUserId(USER_ID); m.setStatus(1);
        m.setJoinTime(LocalDateTime.now());
        return m;
    }

    @Test
    void updateStatus_planNotFound_returns404() {
        when(planMapper.selectById(PLAN_ID)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateStatus(PLAN_ID, PROBLEM_ID, "CHALLENGING", USER_ID));
        assertEquals(404, ex.getCode());
    }

    @Test
    void updateStatus_deactivatedPlan_returns400() {
        TrainingPlan p = activePlan(); p.setIsActive(0);
        when(planMapper.selectById(PLAN_ID)).thenReturn(p);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateStatus(PLAN_ID, PROBLEM_ID, "CHALLENGING", USER_ID));
        assertEquals(400, ex.getCode());
    }

    @Test
    void updateStatus_nonMember_returns403() {
        when(planMapper.selectById(PLAN_ID)).thenReturn(activePlan());
        when(memberMapper.selectOne(any())).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateStatus(PLAN_ID, PROBLEM_ID, "CHALLENGING", USER_ID));
        assertEquals(403, ex.getCode());
    }

    @Test
    void updateStatus_problemNotInPlan_returns404() {
        when(planMapper.selectById(PLAN_ID)).thenReturn(activePlan());
        when(memberMapper.selectOne(any())).thenReturn(activeMember());
        when(planProblemMapper.selectCount(any())).thenReturn(0L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateStatus(PLAN_ID, PROBLEM_ID, "CHALLENGING", USER_ID));
        assertEquals(404, ex.getCode());
    }

    @Test
    void updateStatus_atomicUpdateSucceeds() {
        when(planMapper.selectById(PLAN_ID)).thenReturn(activePlan());
        when(memberMapper.selectOne(any())).thenReturn(activeMember());
        when(planProblemMapper.selectCount(any())).thenReturn(1L);
        when(upsMapper.updateStatusAtomic(USER_ID, PROBLEM_ID, 1)).thenReturn(1);

        assertDoesNotThrow(() -> service.updateStatus(PLAN_ID, PROBLEM_ID, "CHALLENGING", USER_ID));
    }

    @Test
    void updateStatus_noRow_doesNotInsertForNotStarted() {
        when(planMapper.selectById(PLAN_ID)).thenReturn(activePlan());
        when(memberMapper.selectOne(any())).thenReturn(activeMember());
        when(planProblemMapper.selectCount(any())).thenReturn(1L);
        when(upsMapper.updateStatusAtomic(USER_ID, PROBLEM_ID, 0)).thenReturn(0);

        assertDoesNotThrow(() -> service.updateStatus(PLAN_ID, PROBLEM_ID, "NOT_STARTED", USER_ID));
        verify(upsMapper, never()).insert(any(UserProblemStatus.class));
    }

    @Test
    void updateStatus_noRow_insertsForChallenging() {
        when(planMapper.selectById(PLAN_ID)).thenReturn(activePlan());
        when(memberMapper.selectOne(any())).thenReturn(activeMember());
        when(planProblemMapper.selectCount(any())).thenReturn(1L);
        when(upsMapper.updateStatusAtomic(USER_ID, PROBLEM_ID, 1)).thenReturn(0);

        assertDoesNotThrow(() -> service.updateStatus(PLAN_ID, PROBLEM_ID, "CHALLENGING", USER_ID));
        verify(upsMapper).insert(any(UserProblemStatus.class));
    }

    @Test
    void updateNote_succeeds() {
        when(planMapper.selectById(PLAN_ID)).thenReturn(activePlan());
        when(memberMapper.selectOne(any())).thenReturn(activeMember());
        when(planProblemMapper.selectCount(any())).thenReturn(1L);

        assertDoesNotThrow(() -> service.updateNote(PLAN_ID, PROBLEM_ID, "need review", USER_ID));
        verify(upsMapper).upsertNote(USER_ID, PROBLEM_ID, "need review");
    }

    @Test
    void updateNote_tooLong_returns400() {
        String longNote = "a".repeat(501);
        when(planMapper.selectById(PLAN_ID)).thenReturn(activePlan());
        when(memberMapper.selectOne(any())).thenReturn(activeMember());
        when(planProblemMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateNote(PLAN_ID, PROBLEM_ID, longNote, USER_ID));
        assertEquals(400, ex.getCode());
    }

    @Test
    void getMemberProgress_planNotFound_returns404() {
        when(planMapper.selectById(PLAN_ID)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getMemberProgress(PLAN_ID, USER_ID, USER_ID));
        assertEquals(404, ex.getCode());
    }

    @Test
    void getMemberProgress_selfInPublicPlan_succeeds() {
        when(planMapper.selectById(PLAN_ID)).thenReturn(activePlan());
        when(memberMapper.selectOne(any())).thenReturn(activeMember());
        when(planProblemMapper.selectList(any())).thenReturn(List.of());
        when(userMapper.selectById(USER_ID)).thenReturn(new AppUser());

        var result = service.getMemberProgress(PLAN_ID, USER_ID, USER_ID);
        assertEquals(0, result.getCompletedCount());
        assertEquals(0, result.getTotalCount());
    }

    @Test
    void getMemberProgress_withProblems_countsCompleted() {
        when(planMapper.selectById(PLAN_ID)).thenReturn(activePlan());
        when(memberMapper.selectOne(any())).thenReturn(activeMember());

        TrainingPlanProblem tpp = new TrainingPlanProblem();
        tpp.setId(1L); tpp.setProblemId(PROBLEM_ID); tpp.setSortOrder(0); tpp.setRequiredFlag(1);
        when(planProblemMapper.selectList(any())).thenReturn(List.of(tpp));

        Problem prob = new Problem();
        prob.setId(PROBLEM_ID); prob.setTitle("Test"); prob.setPlatform("CUSTOM"); prob.setStatus(1);
        when(problemMapper.selectBatchIds(any())).thenReturn(List.of(prob));

        UserProblemStatus ups = new UserProblemStatus();
        ups.setUserId(USER_ID); ups.setProblemId(PROBLEM_ID); ups.setStatus(2);
        ups.setFirstAcTime(LocalDateTime.now());
        when(upsMapper.selectList(any())).thenReturn(List.of(ups));

        when(userMapper.selectById(USER_ID)).thenReturn(new AppUser());

        var result = service.getMemberProgress(PLAN_ID, USER_ID, USER_ID);
        assertEquals(1, result.getCompletedCount());
        assertEquals(1, result.getTotalCount());
        assertNotNull(result.getLastAcceptedTime());
        assertEquals(1, result.getProblems().size());
        assertEquals("ACCEPTED", result.getProblems().get(0).getMyStatus());
    }
}
