package com.itnoduck.acmate.problem.dto;

import java.time.LocalDateTime;

/**
 * 管理员全部题库列表项响应。
 * 包含创建者用户名和昵称，方便管理员辨认题目创建者。
 */
public record AdminProblemSummaryResponse(
        Long id,
        String platform,
        String externalProblemKey,
        String title,
        String sourceUrl,
        String difficulty,
        String tags,
        ProblemStatusView status,
        String deactivationSource,
        String deactivationReason,
        Long deactivatedBy,
        LocalDateTime deactivationTime,
        Long creatorUserId,
        String creatorUsername,
        String creatorNickname,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {}
