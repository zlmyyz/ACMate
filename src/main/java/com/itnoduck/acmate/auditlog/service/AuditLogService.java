package com.itnoduck.acmate.auditlog.service;

import com.itnoduck.acmate.security.AuthenticatedUser;

import java.util.Map;

public interface AuditLogService {
    Map<String, Object> listLogs(int page, int size, String action, String resourceType, Long operatorId, AuthenticatedUser user);
    void log(Long operatorId, String action, String resourceType, Long resourceId, String reason, String beforeState, String afterState);
}
