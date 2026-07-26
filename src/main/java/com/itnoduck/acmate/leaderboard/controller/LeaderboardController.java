package com.itnoduck.acmate.leaderboard.controller;

import com.itnoduck.acmate.leaderboard.service.LeaderboardService;
import com.itnoduck.acmate.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    public Map<String, Object> leaderboard(
            @RequestParam(defaultValue = "total") String period,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 20;
        var entries = leaderboardService.getLeaderboard(period, currentUser.getId(), page, size);
        long total = leaderboardService.countLeaderboard(period);
        return Map.of("entries", entries, "total", total, "page", page, "size", size);
    }
}
