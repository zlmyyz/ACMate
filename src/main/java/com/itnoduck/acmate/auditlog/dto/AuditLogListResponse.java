package com.itnoduck.acmate.auditlog.dto;

import java.util.List;

public record AuditLogListResponse(
    List<AuditLogResponse> items,
    long total,
    int page,
    int size
) {}
