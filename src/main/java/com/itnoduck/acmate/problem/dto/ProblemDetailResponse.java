package com.itnoduck.acmate.problem.dto;

import java.time.LocalDateTime;

public record ProblemDetailResponse(
        Long id,
        String platform,
        String externalProblemKey,
        String title,
        String sourceUrl,
        String difficulty,
        String tags,
        String contentMd,
        Long creatorUserId,
        String creatorUsername,
        String creatorNickname,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {}
