package com.itnoduck.acmate.user.dto;

import java.time.LocalDateTime;

public record UserProfileResponse(
        Long id,
        String username,
        String nickname,
        String avatarUrl,
        String bio,
        boolean admin,
        long problemCount,
        LocalDateTime createTime) {
}
