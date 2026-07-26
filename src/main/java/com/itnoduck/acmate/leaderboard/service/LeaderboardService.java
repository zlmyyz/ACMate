package com.itnoduck.acmate.leaderboard.service;

import java.util.List;
import java.util.Map;

public interface LeaderboardService {
    List<Map<String, Object>> getLeaderboard(String period, Long currentUserId, int page, int size);
    long countLeaderboard(String period);
}
