package com.itnoduck.acmate.notification.controller;

import com.itnoduck.acmate.notification.service.NotificationService;
import com.itnoduck.acmate.security.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @RequestParam(required = false) Boolean unreadOnly,
                                     @AuthenticationPrincipal AuthenticatedUser user) {
        if (page < 1) throw new com.itnoduck.acmate.common.exception.BusinessException(400, "页码无效");
        if (size < 1 || size > 100) throw new com.itnoduck.acmate.common.exception.BusinessException(400, "每页数量无效");
        return notificationService.listNotifications(user, page, size, unreadOnly != null && unreadOnly);
    }

    @GetMapping("/unread-count")
    public Map<String, Object> unreadCount(@AuthenticationPrincipal AuthenticatedUser user) {
        return Map.of("count", notificationService.countUnread(user));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id,
                                          @AuthenticationPrincipal AuthenticatedUser user) {
        notificationService.markRead(id, user);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal AuthenticatedUser user) {
        notificationService.markAllRead(user);
        return ResponseEntity.noContent().build();
    }
}
