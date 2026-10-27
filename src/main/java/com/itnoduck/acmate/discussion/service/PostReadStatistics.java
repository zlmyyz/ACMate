package com.itnoduck.acmate.discussion.service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.context.event.EventListener;
import java.time.LocalDate;
import java.time.ZoneId;
@Service
public class PostReadStatistics {
    public record Read(long postId) {}
    private final JdbcTemplate jdbc;
    public PostReadStatistics(JdbcTemplate jdbc) {this.jdbc=jdbc;}
    @EventListener public void record(Read event) {
        jdbc.update("INSERT INTO post_daily_views(post_id,stat_date,view_count) VALUES(?,?,1) ON DUPLICATE KEY UPDATE view_count=view_count+1",event.postId(),LocalDate.now(ZoneId.of("Asia/Shanghai")));
    }
}
