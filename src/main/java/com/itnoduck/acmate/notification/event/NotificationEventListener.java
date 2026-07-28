package com.itnoduck.acmate.notification.event;

import com.itnoduck.acmate.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationEvent(NotificationEvent event) {
        try {
            if (event.getRecipientUserIds().size() == 1) {
                Long uid = event.getRecipientUserIds().iterator().next();
                notificationService.send(uid, event.getActorUserId(),
                        event.getNotificationType(), event.getResourceType(),
                        event.getResourceId(), event.getPayload());
            } else {
                notificationService.batchSend(event.getRecipientUserIds(), event.getActorUserId(),
                        event.getNotificationType(), event.getResourceType(),
                        event.getResourceId(), event.getPayload());
            }
        } catch (Exception e) {
            log.error("Notification event handling failed: type={}", event.getNotificationType(), e);
        }
    }
}
