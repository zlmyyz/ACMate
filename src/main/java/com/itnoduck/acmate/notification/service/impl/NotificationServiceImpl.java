package com.itnoduck.acmate.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.notification.entity.Notification;
import com.itnoduck.acmate.notification.mapper.NotificationMapper;
import com.itnoduck.acmate.notification.service.NotificationService;
import com.itnoduck.acmate.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationMapper notificationMapper;
    private final ObjectMapper objectMapper;

    public NotificationServiceImpl(NotificationMapper notificationMapper, ObjectMapper objectMapper) {
        this.notificationMapper = notificationMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> listNotifications(AuthenticatedUser user, int page, int size) {
        var qw = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getRecipientUserId, user.getId())
                .orderByDesc(Notification::getCreateTime);
        var result = notificationMapper.selectPage(new Page<>(page, size), qw);
        List<Map<String, Object>> items = new ArrayList<>();
        for (var n : result.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", n.getId());
            m.put("notificationType", n.getNotificationType());
            m.put("actorUserId", n.getActorUserId());
            m.put("resourceType", n.getResourceType());
            m.put("resourceId", n.getResourceId());
            m.put("payload", parsePayload(n.getPayloadJson()));
            m.put("isRead", n.getIsRead() != null && n.getIsRead() == 1);
            m.put("readTime", n.getReadTime() != null ? n.getReadTime().toString() : null);
            m.put("createTime", n.getCreateTime() != null ? n.getCreateTime().toString() : null);
            items.add(m);
        }
        return Map.of("items", items, "total", result.getTotal(), "page", page, "size", size);
    }

    @Override
    public long countUnread(AuthenticatedUser user) {
        return notificationMapper.countUnread(user.getId());
    }

    @Override
    @Transactional
    public void markRead(Long id, AuthenticatedUser user) {
        var n = notificationMapper.selectById(id);
        if (n == null) throw new BusinessException(404, "通知不存在");
        if (!n.getRecipientUserId().equals(user.getId())) throw new BusinessException(403, "无权操作");
        if (n.getIsRead() == null || n.getIsRead() == 0) {
            n.setIsRead(1);
            n.setReadTime(LocalDateTime.now());
            notificationMapper.updateById(n);
        }
    }

    @Override
    @Transactional
    public void markAllRead(AuthenticatedUser user) {
        notificationMapper.markAllRead(user.getId());
    }

    @Override
    @Transactional
    public void send(Long recipientUserId, Long actorUserId, String notificationType,
                     String resourceType, Long resourceId, Map<String, Object> payload) {
        if (recipientUserId == null) return;
        if (actorUserId != null && actorUserId.equals(recipientUserId)) return;

        Notification n = new Notification();
        n.setRecipientUserId(recipientUserId);
        n.setNotificationType(notificationType);
        n.setActorUserId(actorUserId);
        n.setResourceType(resourceType);
        n.setResourceId(resourceId);
        n.setPayloadJson(toJson(payload));
        n.setIsRead(0);
        try {
            notificationMapper.insert(n);
        } catch (Exception e) {
            log.error("Failed to persist notification: type={} recipient={}", notificationType, recipientUserId, e);
        }
    }

    @Override
    @Transactional
    public void batchSend(Set<Long> recipientUserIds, Long actorUserId, String notificationType,
                          String resourceType, Long resourceId, Map<String, Object> payload) {
        if (recipientUserIds == null || recipientUserIds.isEmpty()) return;

        Set<Long> deduped = new HashSet<>(recipientUserIds);
        deduped.remove(actorUserId);

        String json = toJson(payload);
        List<Notification> batch = new ArrayList<>();
        for (Long uid : deduped) {
            Notification n = new Notification();
            n.setRecipientUserId(uid);
            n.setNotificationType(notificationType);
            n.setActorUserId(actorUserId);
            n.setResourceType(resourceType);
            n.setResourceId(resourceId);
            n.setPayloadJson(json);
            n.setIsRead(0);
            batch.add(n);
        }
        if (batch.isEmpty()) return;

        try {
            for (Notification n : batch) {
                notificationMapper.insert(n);
            }
        } catch (Exception e) {
            log.error("Failed to persist batch notifications: type={} count={}", notificationType, batch.size(), e);
        }
    }

    private String toJson(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize notification payload", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parsePayload(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return null;
        }
    }
}
