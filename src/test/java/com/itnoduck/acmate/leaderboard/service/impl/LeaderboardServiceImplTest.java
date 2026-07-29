package com.itnoduck.acmate.leaderboard.service.impl;

import com.itnoduck.acmate.oj.mapper.OjSubmissionMapper;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceImplTest {

    @Mock private OjSubmissionMapper submissionMapper;
    @Mock private AppUserMapper userMapper;
    @InjectMocks private LeaderboardServiceImpl service;

    private AppUser user1;
    private AppUser user2;

    @BeforeEach
    void setup() {
        user1 = new AppUser();
        user1.setId(1L);
        user1.setUsername("alice");
        user1.setNickname("Alice");
        user1.setAvatarUrl(null);

        user2 = new AppUser();
        user2.setId(2L);
        user2.setUsername("bob");
        user2.setNickname("Bob");
        user2.setAvatarUrl(null);
    }

    private Map<String, Object> row(Long userId, int solvedCount, LocalDateTime lastAccepted) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("user_id", userId);
        m.put("solved_count", solvedCount);
        m.put("last_accepted_time", lastAccepted);
        return m;
    }

    @Test
    void getLeaderboard_total_returnsRowsWithLastAcceptedTime() {
        var r1 = row(1L, 10, LocalDateTime.of(2025, 6, 1, 12, 0));
        var r2 = row(2L, 5, LocalDateTime.of(2025, 6, 2, 12, 0));
        when(submissionMapper.aggregateLeaderboardAll(0L, 20)).thenReturn(List.of(r1, r2));
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(user1, user2));

        var result = service.getLeaderboard("total", 1L, 1, 20);
        assertEquals(2, result.size());

        var first = result.get(0);
        assertEquals(1, first.get("rank"));
        assertEquals("Alice", first.get("nickname"));
        assertEquals(10, first.get("solvedCount"));
        assertNotNull(first.get("lastAcceptedTime"));
        assertEquals(true, first.get("isMe"));

        var second = result.get(1);
        assertEquals(2, second.get("rank"));
        assertEquals("Bob", second.get("nickname"));
        assertEquals(5, second.get("solvedCount"));
        assertNotNull(second.get("lastAcceptedTime"));
        assertEquals(false, second.get("isMe"));
    }

    @Test
    void getLeaderboard_total_containsLastAcceptedTimeField() {
        var r1 = row(1L, 10, LocalDateTime.of(2025, 6, 1, 12, 0, 0));
        when(submissionMapper.aggregateLeaderboardAll(0L, 20)).thenReturn(List.of(r1));
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(user1));

        var result = service.getLeaderboard("total", 1L, 1, 20);
        assertEquals(1, result.size());
        assertTrue(result.get(0).containsKey("lastAcceptedTime"));
        assertEquals("2025-06-01T12:00", result.get(0).get("lastAcceptedTime"));
    }

    @Test
    void getLeaderboard_7d_usesCutoffAndContainsLastAcceptedTime() {
        var r1 = row(1L, 3, LocalDateTime.now().minusDays(1));
        when(submissionMapper.aggregateLeaderboard(any(LocalDateTime.class), eq(0L), eq(20))).thenReturn(List.of(r1));
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(user1));

        var result = service.getLeaderboard("7d", 1L, 1, 20);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).get("lastAcceptedTime"));
    }

    @Test
    void getLeaderboard_30d_usesCutoffAndContainsLastAcceptedTime() {
        var r1 = row(1L, 8, LocalDateTime.now().minusDays(15));
        when(submissionMapper.aggregateLeaderboard(any(LocalDateTime.class), eq(0L), eq(20))).thenReturn(List.of(r1));
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(user1));

        var result = service.getLeaderboard("30d", 1L, 1, 20);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).get("lastAcceptedTime"));
    }

    @Test
    void getLeaderboard_pagination_correctOffset() {
        when(submissionMapper.aggregateLeaderboardAll(20L, 20)).thenReturn(List.of());
        var result = service.getLeaderboard("total", 1L, 2, 20);
        assertEquals(0, result.size());
    }

    @Test
    void countLeaderboard_total_delegates() {
        when(submissionMapper.countLeaderboardUsersAll()).thenReturn(5L);
        assertEquals(5L, service.countLeaderboard("total"));
    }

    @Test
    void countLeaderboard_7d_delegates() {
        when(submissionMapper.countLeaderboardUsers(any(LocalDateTime.class))).thenReturn(3L);
        assertEquals(3L, service.countLeaderboard("7d"));
    }
}
