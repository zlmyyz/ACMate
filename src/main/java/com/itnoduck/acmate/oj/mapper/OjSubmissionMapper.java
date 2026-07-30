package com.itnoduck.acmate.oj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itnoduck.acmate.oj.entity.OjSubmission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface OjSubmissionMapper extends BaseMapper<OjSubmission> {

    @Select("SELECT f.user_id, COUNT(*) AS solved_count, MAX(s.submitted_time) AS last_accepted_time " +
        "FROM oj_first_ac f " +
        "INNER JOIN oj_submission s ON f.submission_id = s.id " +
        "INNER JOIN oj_account a ON f.user_id = a.user_id AND a.verify_status = 1 " +
        "INNER JOIN app_user u ON f.user_id = u.id AND u.status = 1 " +
        "WHERE s.submitted_time >= #{cutoff} " +
        "GROUP BY f.user_id " +
        "ORDER BY solved_count DESC, last_accepted_time ASC, f.user_id ASC " +
        "LIMIT #{offset}, #{size}")
    List<Map<String, Object>> aggregateLeaderboard(@Param("cutoff") java.time.LocalDateTime cutoff,
                                                    @Param("offset") long offset,
                                                    @Param("size") int size);

    @Select("SELECT COUNT(DISTINCT f.user_id) " +
        "FROM oj_first_ac f " +
        "INNER JOIN oj_submission s ON f.submission_id = s.id " +
        "INNER JOIN oj_account a ON f.user_id = a.user_id AND a.verify_status = 1 " +
        "INNER JOIN app_user u ON f.user_id = u.id AND u.status = 1 " +
        "WHERE s.submitted_time >= #{cutoff}")
    long countLeaderboardUsers(@Param("cutoff") java.time.LocalDateTime cutoff);

    @Select("SELECT f.user_id, COUNT(*) AS solved_count, MAX(s.submitted_time) AS last_accepted_time " +
        "FROM oj_first_ac f " +
        "INNER JOIN oj_submission s ON f.submission_id = s.id " +
        "INNER JOIN oj_account a ON f.user_id = a.user_id AND a.verify_status = 1 " +
        "INNER JOIN app_user u ON f.user_id = u.id AND u.status = 1 " +
        "WHERE f.user_id = #{userId} " +
        "AND s.submitted_time >= #{cutoff} " +
        "GROUP BY f.user_id")
    Map<String, Object> getUserSolvedCount(@Param("userId") Long userId, @Param("cutoff") java.time.LocalDateTime cutoff);

    @Select("SELECT f.user_id, COUNT(*) AS solved_count, MAX(s.submitted_time) AS last_accepted_time " +
        "FROM oj_first_ac f " +
        "INNER JOIN oj_submission s ON f.submission_id = s.id " +
        "INNER JOIN oj_account a ON f.user_id = a.user_id AND a.verify_status = 1 " +
        "INNER JOIN app_user u ON f.user_id = u.id AND u.status = 1 " +
        "GROUP BY f.user_id " +
        "ORDER BY solved_count DESC, last_accepted_time ASC, f.user_id ASC " +
        "LIMIT #{offset}, #{size}")
    List<Map<String, Object>> aggregateLeaderboardAll(@Param("offset") long offset, @Param("size") int size);

    @Select("SELECT COUNT(DISTINCT f.user_id) " +
        "FROM oj_first_ac f " +
        "INNER JOIN oj_account a ON f.user_id = a.user_id AND a.verify_status = 1 " +
        "INNER JOIN app_user u ON f.user_id = u.id AND u.status = 1")
    long countLeaderboardUsersAll();

    @Select("SELECT f.user_id, COUNT(*) AS solved_count, MAX(s.submitted_time) AS last_accepted_time " +
        "FROM oj_first_ac f " +
        "INNER JOIN oj_submission s ON f.submission_id = s.id " +
        "INNER JOIN oj_account a ON f.user_id = a.user_id AND a.verify_status = 1 " +
        "INNER JOIN app_user u ON f.user_id = u.id AND u.status = 1 " +
        "WHERE f.user_id = #{userId} " +
        "GROUP BY f.user_id")
    Map<String, Object> getUserSolvedCountAll(@Param("userId") Long userId);

    @Select("SELECT " +
        "COUNT(*) AS solved_count, " +
        "COUNT(CASE WHEN s.submitted_time >= #{cutoff30d} THEN 1 END) AS solved_30d, " +
        "COUNT(CASE WHEN s.submitted_time >= #{cutoff7d} THEN 1 END) AS solved_7d, " +
        "MAX(s.submitted_time) AS last_accepted_time " +
        "FROM oj_first_ac f " +
        "INNER JOIN oj_submission s ON f.submission_id = s.id " +
        "INNER JOIN oj_account a ON f.user_id = a.user_id AND a.platform = 'CODEFORCES' AND a.verify_status = 1 " +
        "WHERE f.user_id = #{userId}")
    Map<String, Object> getUserOjStats(@Param("userId") Long userId,
                                       @Param("cutoff30d") java.time.LocalDateTime cutoff30d,
                                       @Param("cutoff7d") java.time.LocalDateTime cutoff7d);
}
