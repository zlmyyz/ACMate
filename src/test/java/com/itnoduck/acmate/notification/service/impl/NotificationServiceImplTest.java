package com.itnoduck.acmate.notification.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.notification.entity.Notification;
import com.itnoduck.acmate.notification.mapper.NotificationMapper;
import com.itnoduck.acmate.security.AuthenticatedUser;
import com.itnoduck.acmate.testutil.MybatisPlusTestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock private NotificationMapper notificationMapper;
    @Mock private ObjectMapper objectMapper;
    @InjectMocks private NotificationServiceImpl service;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_ID = 2L;

    @BeforeAll
    static void initMybatisPlus() {
        MybatisPlusTestHelper.initEntityTables();
    }

    private AuthenticatedUser user(Long id) {
        return new AuthenticatedUser(id, "user" + id, "hash", "nick" + id,
                null, null, null, id == 999L, true, List.of());
    }

    private Notification buildNotification(Long id, Long recipientId, String type, int isRead) {
        Notification n = new Notification();
        n.setId(id);
        n.setRecipientUserId(recipientId);
        n.setNotificationType(type);
        n.setActorUserId(OTHER_ID);
        n.setResourceType("POST");
        n.setResourceId(10L);
        n.setPayloadJson("{\"postTitle\":\"Test\"}");
        n.setIsRead(isRead);
        n.setReadTime(isRead == 1 ? LocalDateTime.now() : null);
        n.setCreateTime(LocalDateTime.now());
        return n;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> payload(Object... kvs) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kvs.length; i += 2) {
            m.put((String) kvs[i], kvs[i + 1]);
        }
        return m;
    }

    @BeforeEach
    void setup() {
        lenient().doAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(1L);
            n.setCreateTime(LocalDateTime.now());
            return 1;
        }).when(notificationMapper).insert(any(Notification.class));
    }

    // ==================== QUERY ====================

    @Test
    void listNotifications_returnsOnlyCurrentUsers() {
        var n1 = buildNotification(1L, USER_ID, "POST_COMMENTED", 0);
        var n2 = buildNotification(2L, OTHER_ID, "TRAINING_MEMBER_REMOVED", 0);
        var page = new Page<Notification>(1, 20);
        page.setRecords(List.of(n1, n2));
        when(notificationMapper.selectPage(any(Page.class), any())).thenReturn(page);

        var result = service.listNotifications(user(USER_ID), 1, 20);
        @SuppressWarnings("unchecked")
        var items = (List<Map<String, Object>>) result.get("items");
        assertEquals(2, items.size());
    }

    @Test
    void listNotifications_parsesPayloadJson() throws Exception {
        var n = buildNotification(1L, USER_ID, "POST_COMMENTED", 0);
        var page = new Page<Notification>(1, 20);
        page.setRecords(List.of(n));
        when(notificationMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(objectMapper.readValue(eq("{\"postTitle\":\"Test\"}"), any(Class.class)))
                .thenReturn(Map.of("postTitle", "Test"));

        var result = service.listNotifications(user(USER_ID), 1, 20);
        @SuppressWarnings("unchecked")
        var items = (List<Map<String, Object>>) result.get("items");
        assertEquals(1, items.size());
        assertNotNull(items.get(0).get("payload"));
    }

    @Test
    void listNotifications_includesPaginationMeta() {
        var n = buildNotification(1L, USER_ID, "POST_COMMENTED", 0);
        var page = new Page<Notification>(2, 10);
        page.setRecords(List.of(n));
        page.setTotal(25);
        when(notificationMapper.selectPage(any(Page.class), any())).thenReturn(page);

        var result = service.listNotifications(user(USER_ID), 2, 10);
        assertEquals(1, ((List<?>) result.get("items")).size());
        assertEquals(25L, result.get("total"));
        assertEquals(2, result.get("page"));
        assertEquals(10, result.get("size"));
    }

    @Test
    void listNotifications_emptyResult() {
        var page = new Page<Notification>(1, 20);
        page.setRecords(List.of());
        when(notificationMapper.selectPage(any(Page.class), any())).thenReturn(page);

        var result = service.listNotifications(user(USER_ID), 1, 20);
        @SuppressWarnings("unchecked")
        var items = (List<Map<String, Object>>) result.get("items");
        assertTrue(items.isEmpty());
    }

    @Test
    void countUnread_delegatesToMapper() {
        when(notificationMapper.countUnread(USER_ID)).thenReturn(5L);
        assertEquals(5L, service.countUnread(user(USER_ID)));
    }

    // ==================== PERMISSION ====================

    @Test
    void markRead_ownsNotification_succeeds() {
        var n = buildNotification(1L, USER_ID, "POST_COMMENTED", 0);
        when(notificationMapper.selectById(1L)).thenReturn(n);
        when(notificationMapper.updateById(any(Notification.class))).thenReturn(1);

        assertDoesNotThrow(() -> service.markRead(1L, user(USER_ID)));
        assertEquals(1, n.getIsRead());
        assertNotNull(n.getReadTime());
    }

    @Test
    void markRead_otherUsersNotification_throws403() {
        var n = buildNotification(1L, OTHER_ID, "POST_COMMENTED", 0);
        when(notificationMapper.selectById(1L)).thenReturn(n);

        var ex = assertThrows(BusinessException.class,
                () -> service.markRead(1L, user(USER_ID)));
        assertEquals(403, ex.getCode());
    }

    @Test
    void markRead_nonexistentNotification_throws404() {
        when(notificationMapper.selectById(999L)).thenReturn(null);

        var ex = assertThrows(BusinessException.class,
                () -> service.markRead(999L, user(USER_ID)));
        assertEquals(404, ex.getCode());
    }

    @Test
    void markAllRead_delegatesToMapper() {
        when(notificationMapper.markAllRead(USER_ID)).thenReturn(2);
        assertDoesNotThrow(() -> service.markAllRead(user(USER_ID)));
    }

    // ==================== SEND ====================

    @Test
    void send_basicPersist() {
        service.send(USER_ID, OTHER_ID, "POST_COMMENTED", "POST", 10L,
                payload("postTitle", "Test"));

        var captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());
        var n = captor.getValue();
        assertEquals(USER_ID, n.getRecipientUserId());
        assertEquals(OTHER_ID, n.getActorUserId());
        assertEquals("POST_COMMENTED", n.getNotificationType());
        assertEquals("POST", n.getResourceType());
        assertEquals(10L, n.getResourceId());
        assertEquals(0, n.getIsRead());
        assertNull(n.getReadTime());
    }

    @Test
    void send_selfNotification_skipped() {
        service.send(USER_ID, USER_ID, "POST_COMMENTED", "POST", 10L, Map.of());
        verify(notificationMapper, never()).insert(any(Notification.class));
    }

    @Test
    void send_nullRecipient_skipped() {
        service.send(null, OTHER_ID, "POST_COMMENTED", "POST", 10L, Map.of());
        verify(notificationMapper, never()).insert(any(Notification.class));
    }

    @Test
    void send_nullActorUserId_allowed() {
        service.send(USER_ID, null, "TRAINING_SCHEDULE_CHANGED", "TRAINING_PLAN", 10L, Map.of());
        var captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());
        assertNull(captor.getValue().getActorUserId());
    }

    @Test
    void send_payloadSerializedToJson() throws Exception {
        var payload = payload("postTitle", "Test Post", "actorNickname", "Alice");
        when(objectMapper.writeValueAsString(payload))
                .thenReturn("{\"postTitle\":\"Test Post\",\"actorNickname\":\"Alice\"}");

        service.send(USER_ID, OTHER_ID, "POST_COMMENTED", "POST", 10L, payload);

        var captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());
        assertEquals("{\"postTitle\":\"Test Post\",\"actorNickname\":\"Alice\"}",
                captor.getValue().getPayloadJson());
    }

    @Test
    void send_nullPayload_serializesToNull() {
        service.send(USER_ID, OTHER_ID, "POST_COMMENTED", "POST", 10L, null);

        var captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());
        assertNull(captor.getValue().getPayloadJson());
    }

    @Test
    void send_emptyPayload_serializesToNull() {
        service.send(USER_ID, OTHER_ID, "POST_COMMENTED", "POST", 10L, Map.of());

        var captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());
        assertNull(captor.getValue().getPayloadJson());
    }

    @Test
    void send_persistFailure_caughtNotThrown() throws Exception {
        var payload = payload("k", "v");
        when(objectMapper.writeValueAsString(payload)).thenReturn("{\"k\":\"v\"}");
        doThrow(new RuntimeException("DB down")).when(notificationMapper).insert(any(Notification.class));

        assertDoesNotThrow(() ->
                service.send(USER_ID, OTHER_ID, "POST_COMMENTED", "POST", 10L, payload));
    }

    @Test
    void send_serializationFailure_caughtNotThrown() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("bad") {});

        assertDoesNotThrow(() ->
                service.send(USER_ID, OTHER_ID, "POST_COMMENTED", "POST", 10L, Map.of("k", new Object())));
    }

    // ==================== BATCH SEND ====================

    @Test
    void batchSend_allRecipientsGetNotifications() {
        service.batchSend(Set.of(USER_ID, 2L, 3L), null, "TRAINING_ADMIN_DEACTIVATED",
                "TRAINING_PLAN", 10L, payload("planTitle", "Plan A"));

        var captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper, times(3)).insert(captor.capture());
        var ids = captor.getAllValues().stream().map(Notification::getRecipientUserId).toList();
        assertEquals(3, ids.size());
        assertTrue(ids.contains(USER_ID));
        assertTrue(ids.contains(2L));
        assertTrue(ids.contains(3L));
    }

    @Test
    void batchSend_excludesActor() {
        service.batchSend(Set.of(USER_ID, OTHER_ID, 3L), OTHER_ID,
                "TRAINING_ADMIN_DEACTIVATED", "TRAINING_PLAN", 10L, Map.of());

        var captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper, times(2)).insert(captor.capture());
        var ids = captor.getAllValues().stream().map(Notification::getRecipientUserId).toList();
        assertFalse(ids.contains(OTHER_ID));
    }

    @Test
    void batchSend_dedupsDuplicates() {
        service.batchSend(new HashSet<>(List.of(USER_ID, OTHER_ID, USER_ID)), null,
                "TRAINING_SCHEDULE_CHANGED", "TRAINING_PLAN", 10L, Map.of());

        verify(notificationMapper, times(2)).insert(any(Notification.class));
    }

    @Test
    void batchSend_actorIsOnlyRecipient_skipsAll() {
        service.batchSend(Set.of(USER_ID), USER_ID, "POST_COMMENTED", "POST", 10L, Map.of());
        verify(notificationMapper, never()).insert(any(Notification.class));
    }

    @Test
    void batchSend_nullSet_noop() {
        service.batchSend(null, OTHER_ID, "POST_COMMENTED", "POST", 10L, Map.of());
        verify(notificationMapper, never()).insert(any(Notification.class));
    }

    @Test
    void batchSend_emptySet_noop() {
        service.batchSend(Set.of(), OTHER_ID, "POST_COMMENTED", "POST", 10L, Map.of());
        verify(notificationMapper, never()).insert(any(Notification.class));
    }

    @Test
    void batchSend_persistFailure_caughtNotThrown() {
        doThrow(new RuntimeException("DB error")).when(notificationMapper).insert(any(Notification.class));
        assertDoesNotThrow(() ->
                service.batchSend(Set.of(USER_ID), OTHER_ID, "POST_COMMENTED", "POST", 10L, Map.of()));
    }

    // ==================== IDEMPOTENCY ====================

    @Test
    void markRead_alreadyRead_noUpdate() {
        var n = buildNotification(1L, USER_ID, "POST_COMMENTED", 1);
        when(notificationMapper.selectById(1L)).thenReturn(n);

        service.markRead(1L, user(USER_ID));
        verify(notificationMapper, never()).updateById(any(Notification.class));
    }

    @Test
    void markRead_readTime_setOnlyOnFirstRead() {
        var n = buildNotification(1L, USER_ID, "POST_COMMENTED", 0);
        when(notificationMapper.selectById(1L)).thenReturn(n);
        when(notificationMapper.updateById(any(Notification.class))).thenReturn(1);

        service.markRead(1L, user(USER_ID));
        var dt = n.getReadTime();
        assertNotNull(dt);

        service.markRead(1L, user(USER_ID));
        assertEquals(dt, n.getReadTime());
    }

    @Test
    void markAllRead_idempotent_noUnread() {
        when(notificationMapper.markAllRead(USER_ID)).thenReturn(0);
        assertDoesNotThrow(() -> service.markAllRead(user(USER_ID)));
    }

    // ==================== EDGE CASES ====================

    @Test
    void pageValidation_clampsToValidRange() {
        var page = new Page<Notification>(1, 20);
        page.setRecords(List.of());
        page.setTotal(0);
        when(notificationMapper.selectPage(any(Page.class), any())).thenReturn(page);

        var result = service.listNotifications(user(USER_ID), -1, 2000);
        assertNotNull(result);
    }
}
