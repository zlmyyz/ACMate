package com.itnoduck.acmate.oj.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itnoduck.acmate.oj.entity.OjSubmission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
@Mapper
public interface OjSubmissionMapper extends BaseMapper<OjSubmission> {
 String CURRENT_SOLVES=" FROM (SELECT s.user_id,MIN(s.submitted_time) accepted_time FROM oj_submission s INNER JOIN oj_account a ON a.user_id=s.user_id AND a.platform=s.platform AND a.verify_status=1 WHERE s.verdict='OK' AND (s.platform<>'NOWCODER' OR s.nowcoder_uid=a.external_user_id) GROUP BY s.user_id,s.platform,s.external_problem_key) x INNER JOIN app_user u ON u.id=x.user_id AND u.status=1 ";
 @Select("SELECT x.user_id,COUNT(*) solved_count,MAX(x.accepted_time) last_accepted_time"+CURRENT_SOLVES+"WHERE x.accepted_time>=#{cutoff} GROUP BY x.user_id ORDER BY solved_count DESC,last_accepted_time ASC,x.user_id ASC LIMIT #{offset},#{size}")
 List<Map<String,Object>> aggregateLeaderboard(@Param("cutoff") LocalDateTime cutoff,@Param("offset") long offset,@Param("size") int size);
 @Select("SELECT COUNT(DISTINCT x.user_id)"+CURRENT_SOLVES+"WHERE x.accepted_time>=#{cutoff}")
 long countLeaderboardUsers(@Param("cutoff") LocalDateTime cutoff);
 @Select("SELECT x.user_id,COUNT(*) solved_count,MAX(x.accepted_time) last_accepted_time"+CURRENT_SOLVES+"WHERE x.user_id=#{userId} AND x.accepted_time>=#{cutoff} GROUP BY x.user_id")
 Map<String,Object> getUserSolvedCount(@Param("userId") Long userId,@Param("cutoff") LocalDateTime cutoff);
 @Select("SELECT x.user_id,COUNT(*) solved_count,MAX(x.accepted_time) last_accepted_time"+CURRENT_SOLVES+"GROUP BY x.user_id ORDER BY solved_count DESC,last_accepted_time ASC,x.user_id ASC LIMIT #{offset},#{size}")
 List<Map<String,Object>> aggregateLeaderboardAll(@Param("offset") long offset,@Param("size") int size);
 @Select("SELECT COUNT(DISTINCT x.user_id)"+CURRENT_SOLVES)
 long countLeaderboardUsersAll();
 @Select("SELECT x.user_id,COUNT(*) solved_count,MAX(x.accepted_time) last_accepted_time"+CURRENT_SOLVES+"WHERE x.user_id=#{userId} GROUP BY x.user_id")
 Map<String,Object> getUserSolvedCountAll(@Param("userId") Long userId);
 @Select("SELECT COUNT(*) solved_count,COUNT(CASE WHEN x.accepted_time>=#{cutoff30d} THEN 1 END) solved_30d,COUNT(CASE WHEN x.accepted_time>=#{cutoff7d} THEN 1 END) solved_7d,MAX(x.accepted_time) last_accepted_time"+CURRENT_SOLVES+"WHERE x.user_id=#{userId}")
 Map<String,Object> getUserOjStats(@Param("userId") Long userId,@Param("cutoff30d") LocalDateTime cutoff30d,@Param("cutoff7d") LocalDateTime cutoff7d);
}
