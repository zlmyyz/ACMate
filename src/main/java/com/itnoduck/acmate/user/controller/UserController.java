package com.itnoduck.acmate.user.controller;

import com.itnoduck.acmate.security.AuthenticatedUser;
import com.itnoduck.acmate.user.dto.CurrentUserResponse;
import com.itnoduck.acmate.user.dto.UpdateProfileRequest;
import com.itnoduck.acmate.user.dto.UserProfileResponse;
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

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserProfileService userProfileService;

    public UserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
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
    public UserProfileResponse profile(@PathVariable long id) {
        return userProfileService.getProfile(id);
    }

    @PutMapping("/me/profile")
    public ResponseEntity<Void> updateProfile(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                               @Valid @RequestBody UpdateProfileRequest request) {
        userProfileService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<UserProfileResponse> uploadAvatar(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                              @RequestParam("file") MultipartFile file) throws IOException {
        String avatarUrl = userProfileService.updateAvatar(currentUser.getId(),
                file.getOriginalFilename(), file.getBytes());
        UserProfileResponse profile = userProfileService.getProfile(currentUser.getId());
        return ResponseEntity.ok(profile);
    }
}
