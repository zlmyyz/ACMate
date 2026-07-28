package com.itnoduck.acmate.notification.event;

import java.util.*;

public class NotificationEvent {

    private final Set<Long> recipientUserIds;
    private final Long actorUserId;
    private final String notificationType;
    private final String resourceType;
    private final Long resourceId;
    private final Map<String, Object> payload;

    public NotificationEvent(Set<Long> recipientUserIds, Long actorUserId,
                             String notificationType, String resourceType,
                             Long resourceId, Map<String, Object> payload) {
        this.recipientUserIds = new HashSet<>(recipientUserIds);
        this.actorUserId = actorUserId;
        this.notificationType = notificationType;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.payload = payload != null ? new LinkedHashMap<>(payload) : new LinkedHashMap<>();
    }

    public Set<Long> getRecipientUserIds() { return recipientUserIds; }
    public Long getActorUserId() { return actorUserId; }
    public String getNotificationType() { return notificationType; }
    public String getResourceType() { return resourceType; }
    public Long getResourceId() { return resourceId; }
    public Map<String, Object> getPayload() { return payload; }
}
