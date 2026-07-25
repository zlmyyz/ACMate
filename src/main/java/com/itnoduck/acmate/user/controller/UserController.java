package com.itnoduck.acmate.user.controller;

import com.itnoduck.acmate.security.AuthenticatedUser;
import com.itnoduck.acmate.user.dto.CurrentUserResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public CurrentUserResponse me(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return new CurrentUserResponse(
                authenticatedUser.getId(),
                authenticatedUser.getUsername(),
                authenticatedUser.getNickname(),
                authenticatedUser.getEmail(),
                authenticatedUser.getAvatarUrl(),
                authenticatedUser.isAdmin()
        );
    }
}
