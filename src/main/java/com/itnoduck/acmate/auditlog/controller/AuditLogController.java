package com.itnoduck.acmate.auditlog.controller;

import com.itnoduck.acmate.auditlog.service.AuditLogService;
import com.itnoduck.acmate.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @RequestParam(required = false) String action,
                                     @RequestParam(required = false) String resourceType,
                                     @RequestParam(required = false) Long operatorId,
                                     @AuthenticationPrincipal AuthenticatedUser user) {
        return auditLogService.listLogs(page, size, action, resourceType, operatorId, user);
    }
}
