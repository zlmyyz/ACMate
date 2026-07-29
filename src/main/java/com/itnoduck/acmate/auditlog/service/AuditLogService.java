package com.itnoduck.acmate.auditlog.service;

import com.itnoduck.acmate.auditlog.dto.AuditLogListResponse;
import com.itnoduck.acmate.security.AuthenticatedUser;

public interface AuditLogService {
    AuditLogListResponse listLogs(int page, int size, String actorKeyword, String actionType,
                                   String targetType, Long targetId,
                                   String startTime, String endTime, AuthenticatedUser user);
    void log(Long operatorId, String action, String resourceType, Long resourceId, String reason, String beforeState, String afterState);
}
