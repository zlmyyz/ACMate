package com.itnoduck.acmate.user.service;

import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.security.AuthenticatedUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

/** Only called after both identities have been proved and the user explicitly confirms. */
@Service
public class AccountMergeService {
    private final JdbcTemplate jdbc;
    private final SessionRegistry sessions;
    private final StringRedisTemplate redis;

    public AccountMergeService(JdbcTemplate jdbc, SessionRegistry sessions, StringRedisTemplate redis) {
        this.jdbc = jdbc; this.sessions = sessions; this.redis = redis;
    }

    @Transactional
    public void merge(long source, long target) {
        if (source == target) return;
        var locked = jdbc.queryForList("SELECT id,status,email,is_admin FROM app_user WHERE id IN (?,?) ORDER BY id FOR UPDATE", source, target);
        if (locked.size() != 2 || locked.stream().anyMatch(row -> ((Number) row.get("status")).intValue() != 1))
            throw new BusinessException(409, "账号状态已变化，请重新验证后绑定");
        if (locked.stream().anyMatch(row -> Boolean.TRUE.equals(row.get("is_admin")) || (row.get("is_admin") instanceof Number n && n.intValue() != 0)))
            throw new BusinessException(403, "涉及管理员账号的合并请联系管理员处理");
        // Never silently discard an existing GitHub, mailbox, WeChat, or OJ identity.
        if (count("SELECT COUNT(*) FROM user_login_identity a JOIN user_login_identity b ON a.provider=b.provider AND a.identifier<>b.identifier WHERE a.user_id=? AND b.user_id=?", source, target) > 0
                || count("SELECT COUNT(*) FROM user_wechat a JOIN user_wechat b ON a.user_id=? AND b.user_id=?", source, target) > 0
                || count("SELECT COUNT(*) FROM oj_account a JOIN oj_account b ON a.platform=b.platform WHERE a.user_id=? AND b.user_id=?", source, target) > 0)
            throw new BusinessException(409, "两个账号存在重复的平台绑定，请先联系管理员处理，账号数据尚未更改");
        var emails = locked.stream().map(row -> row.get("email")).filter(java.util.Objects::nonNull).distinct().toList();
        if (emails.size() > 1) throw new BusinessException(409, "两个账号均已绑定不同邮箱，不能直接合并，请联系管理员");
        if (count("SELECT COUNT(*) FROM user_problem_status s JOIN user_problem_status t ON t.problem_id=s.problem_id AND t.user_id=? WHERE s.user_id=? AND s.performance_note IS NOT NULL AND t.performance_note IS NOT NULL AND s.performance_note<>t.performance_note AND CHAR_LENGTH(CONCAT(t.performance_note,'\\n',s.performance_note))>500", target, source)>0)
            throw new BusinessException(409,"两个账号的同题训练备注合并后超过 500 字，请先整理备注再绑定");

        jdbc.update("UPDATE problem SET creator_user_id=? WHERE creator_user_id=?", target, source);
        jdbc.update("UPDATE training_plan SET creator_user_id=? WHERE creator_user_id=?", target, source);
        jdbc.update("UPDATE post SET author_user_id=? WHERE author_user_id=?", target, source);
        jdbc.update("UPDATE post_comment SET user_id=? WHERE user_id=?", target, source);
        jdbc.update("UPDATE post_comment SET reply_to_user_id=? WHERE reply_to_user_id=?", target, source);
        jdbc.update("UPDATE notification SET recipient_user_id=? WHERE recipient_user_id=?", target, source);
        jdbc.update("UPDATE notification SET actor_user_id=? WHERE actor_user_id=?", target, source);

        jdbc.update("UPDATE training_plan_member t JOIN training_plan_member s ON t.plan_id=s.plan_id AND s.user_id=? SET t.remove_time=IF(GREATEST(t.status,s.status)=1,NULL,t.remove_time),t.removed_by=IF(GREATEST(t.status,s.status)=1,NULL,t.removed_by),t.status=GREATEST(t.status,s.status),t.join_time=LEAST(t.join_time,s.join_time) WHERE t.user_id=?", source, target);
        jdbc.update("DELETE s FROM training_plan_member s JOIN training_plan_member t ON s.plan_id=t.plan_id AND t.user_id=? WHERE s.user_id=?", target, source);
        jdbc.update("UPDATE training_plan_member SET user_id=? WHERE user_id=?", target, source);
        jdbc.update("UPDATE training_plan_member SET removed_by=? WHERE removed_by=?", target, source);

        jdbc.update("UPDATE user_problem_status t JOIN user_problem_status s ON t.problem_id=s.problem_id AND s.user_id=? SET t.status=GREATEST(t.status,s.status),t.attempt_count=t.attempt_count+s.attempt_count,t.first_submit_time=COALESCE(LEAST(t.first_submit_time,s.first_submit_time),t.first_submit_time,s.first_submit_time),t.last_submit_time=COALESCE(GREATEST(t.last_submit_time,s.last_submit_time),t.last_submit_time,s.last_submit_time),t.accepted_submission_id=IF(s.first_ac_time IS NOT NULL AND (t.first_ac_time IS NULL OR s.first_ac_time<t.first_ac_time),COALESCE(s.accepted_submission_id,t.accepted_submission_id),COALESCE(t.accepted_submission_id,s.accepted_submission_id)),t.solve_source=IF(s.first_ac_time IS NOT NULL AND (t.first_ac_time IS NULL OR s.first_ac_time<t.first_ac_time),COALESCE(s.solve_source,t.solve_source),COALESCE(t.solve_source,s.solve_source)),t.first_ac_time=COALESCE(LEAST(t.first_ac_time,s.first_ac_time),t.first_ac_time,s.first_ac_time),t.performance_note=CASE WHEN t.performance_note IS NULL THEN s.performance_note WHEN s.performance_note IS NULL OR t.performance_note=s.performance_note THEN t.performance_note ELSE CONCAT(t.performance_note,'\\n',s.performance_note) END WHERE t.user_id=?", source, target);
        jdbc.update("DELETE s FROM user_problem_status s JOIN user_problem_status t ON s.problem_id=t.problem_id AND t.user_id=? WHERE s.user_id=?", target, source);
        jdbc.update("UPDATE user_problem_status SET user_id=? WHERE user_id=?", target, source);

        jdbc.update("UPDATE post p JOIN post_like s ON s.post_id=p.id AND s.user_id=? JOIN post_like t ON t.post_id=p.id AND t.user_id=? SET p.like_count=IF(p.like_count>0,p.like_count-1,0)", source, target);
        jdbc.update("DELETE s FROM post_like s JOIN post_like t ON s.post_id=t.post_id AND t.user_id=? WHERE s.user_id=?", target, source);
        jdbc.update("UPDATE post_like SET user_id=? WHERE user_id=?", target, source);
        jdbc.update("INSERT INTO post_favorite(user_id,post_id,create_time) SELECT ?,s.post_id,s.create_time FROM post_favorite s WHERE s.user_id=? ON DUPLICATE KEY UPDATE create_time=LEAST(post_favorite.create_time,VALUES(create_time))",target,source);
        jdbc.update("DELETE FROM post_favorite WHERE user_id=?",source);
        jdbc.update("INSERT IGNORE INTO user_follow(follower_id,followed_id,create_time) SELECT ?,followed_id,create_time FROM user_follow WHERE follower_id=? AND followed_id NOT IN (?,?)",target,source,source,target);
        jdbc.update("INSERT IGNORE INTO user_follow(follower_id,followed_id,create_time) SELECT follower_id,?,create_time FROM user_follow WHERE followed_id=? AND follower_id NOT IN (?,?)",target,source,source,target);
        jdbc.update("DELETE FROM user_follow WHERE follower_id=? OR followed_id=?",source,source);

        jdbc.update("UPDATE oj_first_ac t JOIN oj_first_ac s ON t.platform=s.platform AND t.external_problem_key=s.external_problem_key AND s.user_id=? JOIN oj_submission ts ON ts.id=t.submission_id JOIN oj_submission ss ON ss.id=s.submission_id SET t.submission_id=IF(ss.submitted_time<ts.submitted_time,s.submission_id,t.submission_id),t.create_time=LEAST(t.create_time,s.create_time) WHERE t.user_id=?", source, target);
        jdbc.update("DELETE s FROM oj_first_ac s JOIN oj_first_ac t ON s.platform=t.platform AND s.external_problem_key=t.external_problem_key AND t.user_id=? WHERE s.user_id=?", target, source);
        jdbc.update("UPDATE oj_first_ac SET user_id=? WHERE user_id=?", target, source);
        jdbc.update("UPDATE oj_submission SET user_id=? WHERE user_id=?", target, source);
        jdbc.update("UPDATE oj_submission s LEFT JOIN oj_first_ac f ON f.submission_id=s.id AND f.user_id=? SET s.is_first_ac=IF(f.id IS NULL,0,1) WHERE s.user_id=?", target, target);
        for (String table : java.util.List.of("oj_account", "user_wechat"))
            jdbc.update("UPDATE " + table + " SET user_id=? WHERE user_id=?", target, source);
        jdbc.update("UPDATE nowcoder_account_owner SET user_id=? WHERE user_id=?",target,source);
        // A leased worker must not persist with the pre-merge user ID.
        jdbc.update("UPDATE nowcoder_sync_job SET user_id=?,status=IF(status='RUNNING','QUEUED',status),lease_token=NULL,lease_until=NULL WHERE user_id=?",target,source);
        jdbc.update("INSERT IGNORE INTO user_editor_setting(user_id,autocomplete,preferred_language) SELECT ?,autocomplete,preferred_language FROM user_editor_setting WHERE user_id=?",target,source);
        jdbc.update("DELETE FROM user_editor_setting WHERE user_id=?",source);

        // One identity per provider: duplicate identities must never silently overwrite a binding.
        jdbc.update("DELETE s FROM user_login_identity s JOIN user_login_identity t ON s.provider=t.provider AND s.identifier=t.identifier AND t.user_id=? WHERE s.user_id=?", target, source);
        jdbc.update("UPDATE user_login_identity SET user_id=? WHERE user_id=?", target, source);
        var sourceEmail = locked.stream().filter(row -> ((Number)row.get("id")).longValue() == source).map(row -> row.get("email")).filter(java.util.Objects::nonNull).findFirst().orElse(null);
        jdbc.update("UPDATE app_user SET email=NULL WHERE id=?", source);
        if (sourceEmail != null) jdbc.update("UPDATE app_user SET email=COALESCE(email,?) WHERE id=?", sourceEmail, target);

        jdbc.update("UPDATE app_user t JOIN app_user s ON s.id=? SET t.avatar_url=COALESCE(NULLIF(t.avatar_url,''),s.avatar_url),t.location=COALESCE(t.location,s.location),t.gender=COALESCE(t.gender,s.gender) WHERE t.id=?", source, target);
        jdbc.update("UPDATE app_user SET status=0,merged_into_user_id=? WHERE id=? AND status=1", target, source);
        jdbc.update("INSERT INTO account_merge_log(source_user_id,target_user_id) VALUES(?,?)", source, target);
        afterCommit(() -> { expireSessions(source); expireSessions(target); invalidateReadCaches(); });
    }

    private long count(String sql, Object... args) { return jdbc.queryForObject(sql, Long.class, args); }

    public void expireSessions(long userId) {
        for (Object principal : sessions.getAllPrincipals()) {
            if (principal instanceof AuthenticatedUser user && user.getId() == userId)
                sessions.getAllSessions(principal, false).forEach(org.springframework.security.core.session.SessionInformation::expireNow);
        }
    }

    public static void afterCommit(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) { task.run(); return; }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { task.run(); }
        });
    }

    private void invalidateReadCaches() {
        try (var cursor = redis.scan(ScanOptions.scanOptions().match("acmate:cache:*").count(200).build())) {
            java.util.List<String> keys = new java.util.ArrayList<>();
            cursor.forEachRemaining(keys::add);
            if (!keys.isEmpty()) redis.delete(keys);
        } catch (org.springframework.dao.DataAccessException ignored) {
            // Public caches retain their short TTL if Redis is temporarily unavailable.
        }
    }
}
