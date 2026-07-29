package com.itnoduck.acmate.auditlog.dto;

public record AuditLogResponse(
    Long id,
    String actionType,
    Long actorUserId,
    String actorUsername,
    String actorNickname,
    String targetType,
    Long targetId,
    String beforeState,
    String afterState,
    String reason,
    String createTime
) {}
