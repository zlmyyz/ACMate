package com.itnoduck.acmate.problem.dto;

import java.time.LocalDateTime;

public record ProblemSummaryResponse(
        Long id,
        String platform,
        String externalProblemKey,
        String title,
        String sourceUrl,
        String difficulty,
        String tags,
        Long creatorUserId,
        LocalDateTime createTime
) {}
