package com.itnoduck.acmate.user.controller;

import com.itnoduck.acmate.common.dto.PageResponse;
import com.itnoduck.acmate.problem.dto.ProblemQueryRequest;
import com.itnoduck.acmate.problem.dto.ProblemSummaryResponse;
import com.itnoduck.acmate.problem.service.ProblemQueryService;
import com.itnoduck.acmate.security.AuthenticatedUser;
import com.itnoduck.acmate.training.service.TrainingPlanService;
import com.itnoduck.acmate.user.dto.CurrentUserResponse;
import com.itnoduck.acmate.user.dto.PublicPlanSummaryResponse;
import com.itnoduck.acmate.user.dto.PublicUserProfileResponse;
import com.itnoduck.acmate.user.dto.UpdateProfileRequest;
import com.itnoduck.acmate.user.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserProfileService userProfileService;
    private final ProblemQueryService problemQueryService;
    private final TrainingPlanService trainingPlanService;

    public UserController(UserProfileService userProfileService,
                          ProblemQueryService problemQueryService,
                          TrainingPlanService trainingPlanService) {
        this.userProfileService = userProfileService;
        this.problemQueryService = problemQueryService;
        this.trainingPlanService = trainingPlanService;
    }

    @GetMapping("/me")
    public CurrentUserResponse me(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return new CurrentUserResponse(
                authenticatedUser.getId(),
                authenticatedUser.getUsername(),
                authenticatedUser.getNickname(),
                authenticatedUser.getEmail(),
                authenticatedUser.getAvatarUrl(),
                authenticatedUser.getBio(),
                authenticatedUser.isAdmin()
        );
    }

    @GetMapping("/{id}")
    public PublicUserProfileResponse profile(@PathVariable long id) {
        return userProfileService.getProfile(id);
    }

    /** Public: problems created by this user (ACTIVE only) */
    @GetMapping("/{id}/problems")
    public PageResponse<ProblemSummaryResponse> userProblems(@PathVariable long id,
                                                              @RequestParam(defaultValue = "1") long page,
                                                              @RequestParam(defaultValue = "20") long size) {
        ProblemQueryRequest req = new ProblemQueryRequest();
        req.setPage(Math.max(1, page));
        req.setSize(Math.max(1, Math.min(size, 100)));
        req.setCreatorUserId(id);
        return problemQueryService.listProblems(req);
    }

    /** Public: PUBLIC training plans where user is an ACTIVE member */
    @GetMapping("/{id}/training-plans")
    public Map<String, Object> userTrainingPlans(@PathVariable long id,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        List<PublicPlanSummaryResponse> plans = trainingPlanService.listPublicPlansByUserId(id, page, size);
        int total = trainingPlanService.countPublicPlansByUserId(id);
        return Map.of("plans", plans, "total", total, "page", page, "size", size);
    }

    @PutMapping("/me/profile")
    public ResponseEntity<Void> updateProfile(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                               @Valid @RequestBody UpdateProfileRequest request) {
        userProfileService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<PublicUserProfileResponse> uploadAvatar(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                                    @RequestParam("file") MultipartFile file) throws IOException {
        userProfileService.updateAvatar(currentUser.getId(),
                file.getOriginalFilename(), file.getBytes());
        PublicUserProfileResponse profile = userProfileService.getProfile(currentUser.getId());
        return ResponseEntity.ok(profile);
    }
}
