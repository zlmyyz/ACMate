package com.itnoduck.acmate.user.dto;

public record CurrentUserResponse(
        Long id,
        String username,
        String nickname,
        String email,
        String avatarUrl,
        String bio,
        boolean admin) {
}
