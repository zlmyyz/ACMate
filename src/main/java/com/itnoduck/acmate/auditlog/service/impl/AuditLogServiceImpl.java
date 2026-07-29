package com.itnoduck.acmate.auditlog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itnoduck.acmate.auditlog.AuditLogConstants;
import com.itnoduck.acmate.auditlog.dto.AuditLogListResponse;
import com.itnoduck.acmate.auditlog.dto.AuditLogResponse;
import com.itnoduck.acmate.auditlog.entity.AuditLog;
import com.itnoduck.acmate.auditlog.mapper.AuditLogMapper;
import com.itnoduck.acmate.auditlog.service.AuditLogService;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.security.AuthenticatedUser;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;
    private final AppUserMapper appUserMapper;

    public AuditLogServiceImpl(AuditLogMapper auditLogMapper, AppUserMapper appUserMapper) {
        this.auditLogMapper = auditLogMapper;
        this.appUserMapper = appUserMapper;
    }

    @Override
    public AuditLogListResponse listLogs(int page, int size, String actorKeyword, String actionType,
                                          String targetType, Long targetId,
                                          String startTime, String endTime, AuthenticatedUser user) {
        if (!user.isAdmin()) throw new BusinessException(403, "无权访问");

        // validate actionType whitelist
        if (actionType != null && !actionType.isBlank()
                && !AuditLogConstants.VALID_ACTION_TYPES.contains(actionType.trim())) {
            throw new BusinessException(400, "非法的操作类型: " + actionType);
        }
        if (targetType != null && !targetType.isBlank()
                && !AuditLogConstants.VALID_TARGET_TYPES.contains(targetType.trim())) {
            throw new BusinessException(400, "非法的资源类型: " + targetType);
        }

        var qw = new LambdaQueryWrapper<AuditLog>();
        if (actionType != null && !actionType.isBlank()) {
            qw.eq(AuditLog::getAction, actionType.trim());
        }
        if (targetType != null && !targetType.isBlank()) {
            qw.eq(AuditLog::getResourceType, targetType.trim());
        }
        if (targetId != null && targetId > 0) {
            qw.eq(AuditLog::getResourceId, targetId);
        }
        if (startTime != null && !startTime.isBlank()) {
            try {
                qw.ge(AuditLog::getCreateTime, LocalDateTime.parse(startTime.trim()));
            } catch (Exception e) {
                throw new BusinessException(400, "开始时间格式无效");
            }
        }
        if (endTime != null && !endTime.isBlank()) {
            try {
                qw.le(AuditLog::getCreateTime, LocalDateTime.parse(endTime.trim()));
            } catch (Exception e) {
                throw new BusinessException(400, "结束时间格式无效");
            }
        }

        // stable sort: create_time DESC, id DESC
        qw.orderByDesc(AuditLog::getCreateTime).orderByDesc(AuditLog::getId);

        var result = auditLogMapper.selectPage(new Page<>(page, size), qw);
        List<AuditLog> records = result.getRecords();

        // batch load actor info
        Map<Long, AppUser> actorMap = loadActors(records);

        List<AuditLogResponse> items = new ArrayList<>(records.size());
        for (var l : records) {
            AppUser actor = l.getOperatorId() != null ? actorMap.get(l.getOperatorId()) : null;
            items.add(new AuditLogResponse(
                l.getId(),
                l.getAction(),
                l.getOperatorId(),
                actor != null ? actor.getUsername() : null,
                actor != null ? actor.getNickname() : null,
                l.getResourceType(),
                l.getResourceId(),
                l.getBeforeState(),
                l.getAfterState(),
                l.getReason(),
                l.getCreateTime() != null ? l.getCreateTime().toString() : null
            ));
        }

        // if actorKeyword is present, filter in-memory after batch loading
        if (actorKeyword != null && !actorKeyword.isBlank()) {
            String kw = actorKeyword.trim().toLowerCase();
            items = items.stream()
                .filter(i -> (i.actorUsername() != null && i.actorUsername().toLowerCase().contains(kw))
                          || (i.actorNickname() != null && i.actorNickname().toLowerCase().contains(kw)))
                .collect(Collectors.toList());
        }

        return new AuditLogListResponse(items, result.getTotal(), page, size);
    }

    private Map<Long, AppUser> loadActors(List<AuditLog> records) {
        Set<Long> actorIds = records.stream()
            .map(AuditLog::getOperatorId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (actorIds.isEmpty()) return Map.of();
        List<AppUser> actors = appUserMapper.selectBatchIds(actorIds);
        Map<Long, AppUser> map = new HashMap<>();
        for (var a : actors) {
            map.put(a.getId(), a);
        }
        return map;
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
