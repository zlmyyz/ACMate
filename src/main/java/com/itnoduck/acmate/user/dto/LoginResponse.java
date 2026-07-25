package com.itnoduck.acmate.user.dto;

public record LoginResponse(
        Long id,
        String username,
        String nickname,
        String email,
        String avatarUrl,
        boolean admin) {
}
