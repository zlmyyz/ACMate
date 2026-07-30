package com.itnoduck.acmate.training.controller;

import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.security.AuthenticatedUser;
import com.itnoduck.acmate.training.dto.MemberProgressResponse;
import com.itnoduck.acmate.training.dto.UpdateNoteRequest;
import com.itnoduck.acmate.training.dto.UpdateStatusRequest;
import com.itnoduck.acmate.training.service.TrainingPlanProgressService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/training-plans")
public class TrainingPlanProgressController {

    private final TrainingPlanProgressService progressService;

    public TrainingPlanProgressController(TrainingPlanProgressService progressService) {
        this.progressService = progressService;
    }

    @PutMapping("/{planId}/problems/{problemId}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long planId,
                                              @PathVariable Long problemId,
                                              @Valid @RequestBody UpdateStatusRequest request,
                                              @AuthenticationPrincipal AuthenticatedUser user) {
        progressService.updateStatus(planId, problemId, request.status(), user.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{planId}/problems/{problemId}/note")
    public ResponseEntity<Void> updateNote(@PathVariable Long planId,
                                            @PathVariable Long problemId,
                                            @RequestBody UpdateNoteRequest request,
                                            @AuthenticationPrincipal AuthenticatedUser user) {
        progressService.updateNote(planId, problemId, request.note(), user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{planId}/members/{userId}/progress")
    public MemberProgressResponse getMemberProgress(@PathVariable Long planId,
                                                     @PathVariable Long userId,
                                                     @AuthenticationPrincipal AuthenticatedUser user) {
        return progressService.getMemberProgress(planId, userId, user.getId());
    }
}
