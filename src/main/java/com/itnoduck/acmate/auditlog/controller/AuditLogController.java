package com.itnoduck.acmate.auditlog.controller;

import com.itnoduck.acmate.auditlog.dto.AuditLogListResponse;
import com.itnoduck.acmate.auditlog.service.AuditLogService;
import com.itnoduck.acmate.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public AuditLogListResponse list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String actorKeyword,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @AuthenticationPrincipal AuthenticatedUser user) {
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        if (size > 100) size = 100;
        return auditLogService.listLogs(page, size, actorKeyword, actionType, targetType, targetId, startTime, endTime, user);
    }
}
