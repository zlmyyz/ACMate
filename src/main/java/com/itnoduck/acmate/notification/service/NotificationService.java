package com.itnoduck.acmate.notification.service;

import com.itnoduck.acmate.security.AuthenticatedUser;

import java.util.Map;

public interface NotificationService {
    Map<String, Object> listNotifications(AuthenticatedUser user, int page, int size);
    long countUnread(AuthenticatedUser user);
    void markRead(Long id, AuthenticatedUser user);
    void markAllRead(AuthenticatedUser user);
}
