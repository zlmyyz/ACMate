package com.itnoduck.acmate.leaderboard.service.impl;

import com.itnoduck.acmate.common.cache.RedisJsonCache;
import com.itnoduck.acmate.leaderboard.service.LeaderboardService;
import com.itnoduck.acmate.oj.mapper.OjSubmissionMapper;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class LeaderboardServiceImpl implements LeaderboardService {

    private final OjSubmissionMapper submissionMapper;
    private final AppUserMapper userMapper;
    private final RedisJsonCache redisCache;
    private static final Duration CACHE_TTL = Duration.ofSeconds(30);
    private static final TypeReference<List<Map<String, Object>>> LEADERBOARD_TYPE = new TypeReference<>() {};
    private static final TypeReference<Long> LEADERBOARD_COUNT_TYPE = new TypeReference<>() {};

    @Autowired
    public LeaderboardServiceImpl(OjSubmissionMapper submissionMapper, AppUserMapper userMapper,
                                  RedisJsonCache redisCache) {
        this.submissionMapper = submissionMapper;
        this.userMapper = userMapper;
        this.redisCache = redisCache;
    }

    public LeaderboardServiceImpl(OjSubmissionMapper submissionMapper, AppUserMapper userMapper) {
        this(submissionMapper, userMapper, null);
    }

    @Override
    public List<Map<String, Object>> getLeaderboard(String period, Long currentUserId, int page, int size) {
        List<Map<String, Object>> baseRows = loadLeaderboard(period, page, size);
        return markCurrentUser(baseRows, currentUserId);
    }

    private List<Map<String, Object>> loadLeaderboard(String period, int page, int size) {
        LocalDateTime cutoff = computeCutoff(period);
        long offset = (long) (page - 1) * size;

        List<Map<String, Object>> rows;
        if (cutoff != null) {
            rows = submissionMapper.aggregateLeaderboard(cutoff, offset, size);
        } else {
            rows = submissionMapper.aggregateLeaderboardAll(offset, size);
        }

        return enrichRows(rows, null, (page - 1) * size);
    }

    @Override
    public long countLeaderboard(String period) {
        return loadLeaderboardCount(period);
    }

    private Long loadLeaderboardCount(String period) {
        LocalDateTime cutoff = computeCutoff(period);
        return cutoff != null ? submissionMapper.countLeaderboardUsers(cutoff)
                : submissionMapper.countLeaderboardUsersAll();
    }

    private List<Map<String, Object>> markCurrentUser(List<Map<String, Object>> rows, Long currentUserId) {
        return rows.stream().map(row -> {
            Map<String, Object> copy = new LinkedHashMap<>(row);
            Object userId = copy.get("userId");
            copy.put("isMe", userId instanceof Number number && Objects.equals(number.longValue(), currentUserId));
            return copy;
        }).toList();
    }

    private String normalizedPeriod(String period) {
        return "7d".equals(period) || "30d".equals(period) ? period : "total";
    }

    private List<Map<String, Object>> enrichRows(List<Map<String, Object>> rows, Long currentUserId, int startRank) {
        List<Map<String, Object>> result = new ArrayList<>();
        Set<Long> userIds = new LinkedHashSet<>();
        for (var row : rows) {
            Object uidObj = row.get("user_id");
            if (uidObj != null) userIds.add(((Number) uidObj).longValue());
        }

        Map<Long, AppUser> userMap = Collections.emptyMap();
        if (!userIds.isEmpty()) {
            userMap = new HashMap<>();
            for (var u : userMapper.selectBatchIds(userIds)) {
                userMap.put(u.getId(), u);
            }
        }

        int rank = startRank;
        for (var row : rows) {
            Object uidObj = row.get("user_id");
            if (uidObj == null) continue;
            Long uid = ((Number) uidObj).longValue();
            AppUser u = userMap.get(uid);
            if (u == null) continue;

            rank++;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", rank);
            item.put("userId", u.getId());
            item.put("username", u.getUsername());
            item.put("nickname", u.getNickname());
            item.put("avatarUrl", u.getAvatarUrl());
            item.put("solvedCount", ((Number) row.get("solved_count")).intValue());
            item.put("lastAcceptedTime", row.get("last_accepted_time") != null ? row.get("last_accepted_time").toString() : null);
            item.put("isMe", u.getId().equals(currentUserId));
            result.add(item);
        }
        return result;
    }

    private LocalDateTime computeCutoff(String period) {
        if ("7d".equals(period)) return LocalDateTime.now().minusDays(7);
        if ("30d".equals(period)) return LocalDateTime.now().minusDays(30);
        return null;
    }
}
