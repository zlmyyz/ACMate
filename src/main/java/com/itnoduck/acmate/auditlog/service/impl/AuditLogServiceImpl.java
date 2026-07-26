package com.itnoduck.acmate.auditlog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itnoduck.acmate.auditlog.entity.AuditLog;
import com.itnoduck.acmate.auditlog.mapper.AuditLogMapper;
import com.itnoduck.acmate.auditlog.service.AuditLogService;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;

    public AuditLogServiceImpl(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    public Map<String, Object> listLogs(int page, int size, String action, String resourceType, Long operatorId, AuthenticatedUser user) {
        if (!user.isAdmin()) throw new BusinessException(403, "无权访问");
        var qw = new LambdaQueryWrapper<AuditLog>();
        if (action != null && !action.isBlank()) qw.eq(AuditLog::getAction, action);
        if (resourceType != null && !resourceType.isBlank()) qw.eq(AuditLog::getResourceType, resourceType);
        if (operatorId != null && operatorId > 0) qw.eq(AuditLog::getOperatorId, operatorId);
        qw.orderByDesc(AuditLog::getCreateTime);
        var result = auditLogMapper.selectPage(new Page<>(page, size), qw);
        List<Map<String, Object>> items = new ArrayList<>();
        for (var l : result.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", l.getId());
            m.put("operatorId", l.getOperatorId());
            m.put("action", l.getAction());
            m.put("resourceType", l.getResourceType());
            m.put("resourceId", l.getResourceId());
            m.put("reason", l.getReason());
            m.put("beforeState", l.getBeforeState());
            m.put("afterState", l.getAfterState());
            m.put("createTime", l.getCreateTime() != null ? l.getCreateTime().toString() : null);
            items.add(m);
        }
        return Map.of("items", items, "total", result.getTotal(), "page", page, "size", size);
    }

    @Override
    @Transactional
    public void log(Long operatorId, String action, String resourceType, Long resourceId, String reason, String beforeState, String afterState) {
        AuditLog log = new AuditLog();
        log.setOperatorId(operatorId);
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setReason(reason);
        log.setBeforeState(beforeState);
        log.setAfterState(afterState);
        auditLogMapper.insert(log);
    }
}
