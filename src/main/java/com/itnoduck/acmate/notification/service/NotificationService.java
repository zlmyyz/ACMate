package com.itnoduck.acmate.notification.service;

import com.itnoduck.acmate.security.AuthenticatedUser;

import java.util.*;

public interface NotificationService {
    Map<String, Object> listNotifications(AuthenticatedUser user, int page, int size, boolean unreadOnly);
    long countUnread(AuthenticatedUser user);
    void markRead(Long id, AuthenticatedUser user);
    void markAllRead(AuthenticatedUser user);

    void send(Long recipientUserId, Long actorUserId, String notificationType,
              String resourceType, Long resourceId, Map<String, Object> payload);
    void batchSend(Set<Long> recipientUserIds, Long actorUserId, String notificationType,
                   String resourceType, Long resourceId, Map<String, Object> payload);
}
