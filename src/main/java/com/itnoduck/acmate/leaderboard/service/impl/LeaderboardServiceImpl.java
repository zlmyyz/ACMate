package com.itnoduck.acmate.leaderboard.service.impl;

import com.itnoduck.acmate.leaderboard.service.LeaderboardService;
import com.itnoduck.acmate.oj.mapper.OjSubmissionMapper;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class LeaderboardServiceImpl implements LeaderboardService {

    private final OjSubmissionMapper submissionMapper;
    private final AppUserMapper userMapper;

    public LeaderboardServiceImpl(OjSubmissionMapper submissionMapper, AppUserMapper userMapper) {
        this.submissionMapper = submissionMapper;
        this.userMapper = userMapper;
    }

    @Override
    public List<Map<String, Object>> getLeaderboard(String period, Long currentUserId, int page, int size) {
        LocalDateTime cutoff = computeCutoff(period);
        long offset = (long) (page - 1) * size;

        List<Map<String, Object>> rows;
        if (cutoff != null) {
            rows = submissionMapper.aggregateLeaderboard(cutoff, offset, size);
        } else {
            rows = submissionMapper.aggregateLeaderboardAll(offset, size);
        }

        return enrichRows(rows, currentUserId, (page - 1) * size);
    }

    @Override
    public long countLeaderboard(String period) {
        LocalDateTime cutoff = computeCutoff(period);
        if (cutoff != null) return submissionMapper.countLeaderboardUsers(cutoff);
        return submissionMapper.countLeaderboardUsersAll();
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
                if (u.getStatus() != null && u.getStatus() == 1) userMap.put(u.getId(), u);
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
