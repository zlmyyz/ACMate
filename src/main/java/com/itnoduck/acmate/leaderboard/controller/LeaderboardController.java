package com.itnoduck.acmate.leaderboard.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import com.itnoduck.acmate.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final AppUserMapper userMapper;
    private final com.itnoduck.acmate.oj.mapper.OjSubmissionMapper submissionMapper;
    private final com.itnoduck.acmate.oj.mapper.OjAccountMapper accountMapper;

    public LeaderboardController(AppUserMapper userMapper,
                                  com.itnoduck.acmate.oj.mapper.OjSubmissionMapper submissionMapper,
                                  com.itnoduck.acmate.oj.mapper.OjAccountMapper accountMapper) {
        this.userMapper = userMapper;
        this.submissionMapper = submissionMapper;
        this.accountMapper = accountMapper;
    }

    @GetMapping
    public List<Map<String, Object>> leaderboard(
            @RequestParam(defaultValue = "total") String period,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        // Get verified account user ids
        var verifiedAccounts = accountMapper.selectList(
                new LambdaQueryWrapper<com.itnoduck.acmate.oj.entity.OjAccount>()
                        .eq(com.itnoduck.acmate.oj.entity.OjAccount::getVerifyStatus, 1));
        Set<Long> verifiedUserIds = new HashSet<>();
        for (var acc : verifiedAccounts) verifiedUserIds.add(acc.getUserId());

        // Get all first_ac submissions
        var allSubmissions = submissionMapper.selectList(
                new LambdaQueryWrapper<com.itnoduck.acmate.oj.entity.OjSubmission>()
                        .eq(com.itnoduck.acmate.oj.entity.OjSubmission::getIsFirstAc, 1));

        // Filter by time period
        java.time.LocalDateTime cutoff = null;
        if ("7d".equals(period)) cutoff = java.time.LocalDateTime.now().minusDays(7);
        else if ("30d".equals(period)) cutoff = java.time.LocalDateTime.now().minusDays(30);

        // Count unique problems per verified user
        Map<Long, Set<Long>> userProblems = new LinkedHashMap<>();
        for (var sub : allSubmissions) {
            if (!verifiedUserIds.contains(sub.getUserId())) continue;
            if (sub.getProblemId() == null) continue;
            if (cutoff != null && sub.getSubmittedTime() != null && sub.getSubmittedTime().isBefore(cutoff)) continue;
            userProblems.computeIfAbsent(sub.getUserId(), k -> new HashSet<>()).add(sub.getProblemId());
        }

        // Sort by count desc
        List<Map.Entry<Long, Set<Long>>> sorted = new ArrayList<>(userProblems.entrySet());
        sorted.sort((a, b) -> {
            int cmp = Integer.compare(b.getValue().size(), a.getValue().size());
            if (cmp != 0) return cmp;
            return Long.compare(a.getKey(), b.getKey());
        });

        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 0;
        for (var entry : sorted) {
            rank++;
            AppUser u = userMapper.selectById(entry.getKey());
            if (u == null || u.getStatus() == 0) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rank", rank);
            row.put("userId", u.getId());
            row.put("username", u.getUsername());
            row.put("nickname", u.getNickname());
            row.put("avatarUrl", u.getAvatarUrl());
            row.put("solvedCount", entry.getValue().size());
            row.put("isMe", u.getId().equals(currentUser.getId()));
            result.add(row);
        }
        return result;
    }
}
