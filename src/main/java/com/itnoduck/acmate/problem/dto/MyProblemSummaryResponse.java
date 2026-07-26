package com.itnoduck.acmate.problem.dto;

import java.time.LocalDateTime;

/**
 * "我的题目"列表项响应。
 * 相比公共列表增加了 {@code status} 和 {@code updateTime}，移除了 {@code creatorUserId}（都是当前用户自己）。
 * 公共列表不返回 status，避免向其他用户暴露题目管理状态。
 */
public record MyProblemSummaryResponse(
        Long id,
        String platform,
        String externalProblemKey,
        String title,
        String sourceUrl,
        String difficulty,
        String tags,
        ProblemStatusView status,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {}
