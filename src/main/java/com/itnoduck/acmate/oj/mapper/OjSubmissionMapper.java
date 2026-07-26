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

    @Select("SELECT s.user_id, COUNT(DISTINCT s.problem_id) AS solved_count " +
        "FROM oj_submission s " +
        "INNER JOIN oj_account a ON s.user_id = a.user_id AND a.verify_status = 1 " +
        "WHERE s.is_first_ac = 1 AND s.problem_id IS NOT NULL " +
        "AND s.submitted_time >= #{cutoff} " +
        "GROUP BY s.user_id " +
        "ORDER BY solved_count DESC, s.user_id ASC " +
        "LIMIT #{offset}, #{size}")
    List<Map<String, Object>> aggregateLeaderboard(@Param("cutoff") java.time.LocalDateTime cutoff,
                                                    @Param("offset") long offset,
                                                    @Param("size") int size);

    @Select("SELECT COUNT(DISTINCT s.user_id) " +
        "FROM oj_submission s " +
        "INNER JOIN oj_account a ON s.user_id = a.user_id AND a.verify_status = 1 " +
        "WHERE s.is_first_ac = 1 AND s.problem_id IS NOT NULL " +
        "AND s.submitted_time >= #{cutoff}")
    long countLeaderboardUsers(@Param("cutoff") java.time.LocalDateTime cutoff);

    @Select("SELECT s.user_id, COUNT(DISTINCT s.problem_id) AS solved_count " +
        "FROM oj_submission s " +
        "INNER JOIN oj_account a ON s.user_id = a.user_id AND a.verify_status = 1 " +
        "WHERE s.is_first_ac = 1 AND s.problem_id IS NOT NULL " +
        "AND s.user_id = #{userId} " +
        "AND s.submitted_time >= #{cutoff} " +
        "GROUP BY s.user_id")
    Map<String, Object> getUserSolvedCount(@Param("userId") Long userId, @Param("cutoff") java.time.LocalDateTime cutoff);

    @Select("SELECT s.user_id, COUNT(DISTINCT s.problem_id) AS solved_count " +
        "FROM oj_submission s " +
        "INNER JOIN oj_account a ON s.user_id = a.user_id AND a.verify_status = 1 " +
        "WHERE s.is_first_ac = 1 AND s.problem_id IS NOT NULL " +
        "GROUP BY s.user_id " +
        "ORDER BY solved_count DESC, s.user_id ASC " +
        "LIMIT #{offset}, #{size}")
    List<Map<String, Object>> aggregateLeaderboardAll(@Param("offset") long offset, @Param("size") int size);

    @Select("SELECT COUNT(DISTINCT s.user_id) " +
        "FROM oj_submission s " +
        "INNER JOIN oj_account a ON s.user_id = a.user_id AND a.verify_status = 1 " +
        "WHERE s.is_first_ac = 1 AND s.problem_id IS NOT NULL")
    long countLeaderboardUsersAll();

    @Select("SELECT s.user_id, COUNT(DISTINCT s.problem_id) AS solved_count " +
        "FROM oj_submission s " +
        "INNER JOIN oj_account a ON s.user_id = a.user_id AND a.verify_status = 1 " +
        "WHERE s.is_first_ac = 1 AND s.problem_id IS NOT NULL " +
        "AND s.user_id = #{userId} " +
        "GROUP BY s.user_id")
    Map<String, Object> getUserSolvedCountAll(@Param("userId") Long userId);
}
