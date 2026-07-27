package com.itnoduck.acmate.training.controller;

import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.security.AuthenticatedUser;
import com.itnoduck.acmate.training.dto.*;
import com.itnoduck.acmate.training.service.TrainingPlanService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/training-plans")
public class TrainingPlanController {

    private final TrainingPlanService trainingPlanService;

    public TrainingPlanController(TrainingPlanService trainingPlanService) {
        this.trainingPlanService = trainingPlanService;
    }

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "PUBLIC") String type,
            @RequestParam(defaultValue = "") String timeStatus,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUser user) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BusinessException(400, "分页参数非法");
        }
        List<PlanSummaryResponse> plans = trainingPlanService.listPlans(type, timeStatus, keyword, page, size, user.getId());
        int total = trainingPlanService.countPlans(type, timeStatus, keyword, user.getId());
        return Map.of("plans", plans, "total", total, "page", page, "size", size);
    }

    @PostMapping
    public PlanDetailResponse create(@Valid @RequestBody CreatePlanRequest request,
                                      @AuthenticationPrincipal AuthenticatedUser user) {
        if ("PUBLIC".equals(request.getPlanType()) && !user.isAdmin()) {
            throw new BusinessException(403, "只有管理员才能创建公开计划");
        }
        return trainingPlanService.createPlan(request, user.getId());
    }

    @GetMapping("/{id}")
    public PlanDetailResponse detail(@PathVariable Long id,
                                      @AuthenticationPrincipal AuthenticatedUser user) {
        return trainingPlanService.getPlanDetail(id, user.getId());
    }

    @PutMapping("/{id}")
    public PlanDetailResponse update(@PathVariable Long id,
                                      @Valid @RequestBody UpdatePlanRequest request,
                                      @AuthenticationPrincipal AuthenticatedUser user) {
        return trainingPlanService.updatePlan(id, request, user.getId());
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long id,
                                            @RequestBody(required = false) DeactivateRequest request,
                                            @AuthenticationPrincipal AuthenticatedUser user) {
        String reason = request != null ? request.getReason() : null;
        trainingPlanService.deactivatePlan(id, reason, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable Long id,
                                         @AuthenticationPrincipal AuthenticatedUser user) {
        trainingPlanService.restorePlan(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/problems")
    public ResponseEntity<Void> addProblem(@PathVariable Long id,
                                            @Valid @RequestBody AddProblemRequest request,
                                            @AuthenticationPrincipal AuthenticatedUser user) {
        trainingPlanService.addProblem(id, request, user.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/problems/{problemId}")
    public ResponseEntity<Void> removeProblem(@PathVariable Long id,
                                               @PathVariable Long problemId,
                                               @AuthenticationPrincipal AuthenticatedUser user) {
        trainingPlanService.removeProblem(id, problemId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members/me")
    public ResponseEntity<Void> join(@PathVariable Long id,
                                      @AuthenticationPrincipal AuthenticatedUser user) {
        trainingPlanService.joinPlan(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long id,
                                              @PathVariable Long userId,
                                              @AuthenticationPrincipal AuthenticatedUser user) {
        trainingPlanService.removeMember(id, userId, user.getId());
        return ResponseEntity.noContent().build();
    }
}
