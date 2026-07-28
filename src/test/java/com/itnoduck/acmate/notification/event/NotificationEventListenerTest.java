package com.itnoduck.acmate.notification.event;

import com.itnoduck.acmate.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock private NotificationService notificationService;
    @InjectMocks private NotificationEventListener listener;

    @Test
    void singleRecipient_routesToSend() {
        var payload = Map.<String, Object>of("postTitle", "Test");
        var event = new NotificationEvent(Set.of(1L), 2L, "POST_COMMENTED", "POST", 10L, payload);

        listener.onNotificationEvent(event);
        verify(notificationService).send(1L, 2L, "POST_COMMENTED", "POST", 10L, payload);
        verify(notificationService, never()).batchSend(anySet(), any(), any(), any(), any(), anyMap());
    }

    @Test
    void multipleRecipients_routesToBatchSend() {
        var payload = Map.<String, Object>of("planTitle", "Plan A");
        var recipients = Set.of(1L, 2L, 3L);
        var event = new NotificationEvent(recipients, 4L, "TRAINING_ADMIN_DEACTIVATED", "TRAINING_PLAN", 10L, payload);

        listener.onNotificationEvent(event);
        verify(notificationService).batchSend(recipients, 4L, "TRAINING_ADMIN_DEACTIVATED", "TRAINING_PLAN", 10L, payload);
        verify(notificationService, never()).send(anyLong(), any(), any(), any(), any(), anyMap());
    }

    @Test
    void singleRecipient_eventConstructedWithSetOfOne() {
        var event = new NotificationEvent(Set.of(42L), null, "TRAINING_SCHEDULE_CHANGED", "TRAINING_PLAN", 5L, Map.of());
        assertEquals(1, event.getRecipientUserIds().size());
    }

    @Test
    void event_payloadIsDefensiveCopy() {
        var mutable = new LinkedHashMap<String, Object>();
        mutable.put("k", "v");
        var event = new NotificationEvent(Set.of(1L), 2L, "T", "R", 1L, mutable);
        mutable.put("extra", "should not appear");
        assertFalse(event.getPayload().containsKey("extra"));
    }

    @Test
    void event_recipientsAreDefensiveCopy() {
        var mutable = new HashSet<>(Set.of(1L, 2L));
        var event = new NotificationEvent(mutable, 3L, "T", "R", 1L, Map.of());
        mutable.add(99L);
        assertEquals(2, event.getRecipientUserIds().size());
    }

    @Test
    void event_nullPayload_becomesEmptyMap() {
        var event = new NotificationEvent(Set.of(1L), 2L, "T", "R", 1L, null);
        assertNotNull(event.getPayload());
        assertTrue(event.getPayload().isEmpty());
    }

    @Test
    void listener_swallowsExceptionWithoutRethrow() {
        doThrow(new RuntimeException("service down")).when(notificationService)
                .send(anyLong(), any(), anyString(), any(), any(), anyMap());

        assertDoesNotThrow(() -> listener.onNotificationEvent(
                new NotificationEvent(Set.of(1L), 2L, "POST_COMMENTED", "POST", 10L, Map.of())));
    }

    @Test
    void listener_batchSend_swallowsExceptionWithoutRethrow() {
        doThrow(new RuntimeException("service down")).when(notificationService)
                .batchSend(anySet(), any(), anyString(), any(), any(), anyMap());

        assertDoesNotThrow(() -> listener.onNotificationEvent(
                new NotificationEvent(Set.of(1L, 2L), 3L, "TRAINING_ADMIN_DEACTIVATED", "TRAINING_PLAN", 10L, Map.of())));
    }
}
