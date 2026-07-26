package com.itnoduck.acmate.export.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.export.service.DataExportService;
import com.itnoduck.acmate.oj.mapper.OjSubmissionMapper;
import com.itnoduck.acmate.problem.entity.Problem;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import com.itnoduck.acmate.security.AuthenticatedUser;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class DataExportServiceImpl implements DataExportService {

    private final ProblemMapper problemMapper;
    private final OjSubmissionMapper submissionMapper;

    public DataExportServiceImpl(ProblemMapper problemMapper, OjSubmissionMapper submissionMapper) {
        this.problemMapper = problemMapper;
        this.submissionMapper = submissionMapper;
    }

    @Override
    public String exportProblems(AuthenticatedUser user) {
        if (!user.isAdmin()) throw new BusinessException(403, "无权访问");
        var problems = problemMapper.selectList(new LambdaQueryWrapper<Problem>().orderByAsc(Problem::getId));
        StringBuilder sb = new StringBuilder("ID,Title,Platform,Difficulty,Status,CreatorUserId,CreateTime\n");
        for (var p : problems) {
            sb.append(p.getId()).append(',')
              .append(escapeCsv(p.getTitle())).append(',')
              .append(escapeCsv(p.getPlatform())).append(',')
              .append(escapeCsv(p.getDifficulty())).append(',')
              .append(p.getStatus()).append(',')
              .append(p.getCreatorUserId()).append(',')
              .append(p.getCreateTime()).append('\n');
        }
        return sb.toString();
    }

    @Override
    public String exportLeaderboard(String period, AuthenticatedUser user) {
        if (!user.isAdmin()) throw new BusinessException(403, "无权访问");
        LocalDateTime cutoff = null;
        if ("7d".equals(period)) cutoff = LocalDateTime.now().minusDays(7);
        else if ("30d".equals(period)) cutoff = LocalDateTime.now().minusDays(30);

        List<Map<String, Object>> rows;
        if (cutoff != null) rows = submissionMapper.aggregateLeaderboard(cutoff, 0, 1000);
        else rows = submissionMapper.aggregateLeaderboardAll(0, 1000);

        StringBuilder sb = new StringBuilder("Rank,UserId,SolvedCount\n");
        int rank = 0;
        for (var row : rows) {
            rank++;
            sb.append(rank).append(',')
              .append(row.get("user_id")).append(',')
              .append(row.get("solved_count")).append('\n');
        }
        return sb.toString();
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        String escaped = s;
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) escaped = '"' + escaped.replace("\"", "\"\"") + '"';
        if (escaped.startsWith("=") || escaped.startsWith("+") || escaped.startsWith("-") || escaped.startsWith("@")) escaped = "'" + escaped;
        return escaped;
    }
}
