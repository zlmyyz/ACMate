package com.itnoduck.acmate.training.service;

import com.itnoduck.acmate.training.dto.*;

import java.util.List;

public interface TrainingPlanService {
    PlanDetailResponse createPlan(CreatePlanRequest request, Long creatorUserId);
    PlanDetailResponse updatePlan(Long planId, UpdatePlanRequest request, Long userId);
    PlanDetailResponse getPlanDetail(Long planId, Long userId);
    List<PlanSummaryResponse> listPlans(String planType, String timeStatus, String keyword, int page, int size, Long userId);
    int countPlans(String planType, String timeStatus, String keyword, Long userId);
    void deletePlan(Long planId, Long userId);
    void addProblem(Long planId, AddProblemRequest request, Long userId);
    void removeProblem(Long planId, Long problemId, Long userId);
    void joinPlan(Long planId, Long userId);
    void removeMember(Long planId, Long memberUserId, Long operatorUserId);
    void toggleActive(Long planId, Long userId);
}
