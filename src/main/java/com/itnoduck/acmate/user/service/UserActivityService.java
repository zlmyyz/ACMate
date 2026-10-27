package com.itnoduck.acmate.user.service;
import com.itnoduck.acmate.common.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

/** Reads stored submissions only. No external OJ requests are made by profile pages. */
@Service
public class UserActivityService {
    private final JdbcTemplate jdbc;
    public record Day(LocalDate date,long count) {}
    public record Activity(LocalDate start,LocalDate end,long submissions,int activeDays,List<Day> days) {}
    public record Submission(long id,String platform,String problemKey,Long problemId,String title,String verdict,String language,LocalDateTime submittedTime) {}
    public UserActivityService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    private void requireUser(long id) { if(jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE id=?",Long.class,id)==0) throw new BusinessException(404,"用户不存在"); }
    public Activity activity(long id) {return activity(id,null);}
    private String platform(String value) {
        if(value==null||value.equals("ALL"))return null;
        if(!List.of("CODEFORCES","NOWCODER").contains(value))throw new BusinessException(400,"不支持的 OJ 平台");
        return value;
    }
    public Activity activity(long id,String requestedPlatform) {
        requireUser(id);
        var end=LocalDate.now(ZoneId.of("Asia/Shanghai"));var start=end.minusDays(364);
        var counts=new HashMap<LocalDate,Long>();
        String selected=platform(requestedPlatform);
        var args=new ArrayList<Object>(List.of(id,start,end.plusDays(1)));if(selected!=null)args.add(selected);
        jdbc.query("SELECT DATE(submitted_time) AS day,COUNT(*) AS count FROM oj_submission WHERE user_id=? AND submitted_time>=? AND submitted_time<? "+(selected==null?"":" AND platform=?")+" GROUP BY DATE(submitted_time)",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> counts.put(rs.getDate("day").toLocalDate(),rs.getLong("count")),args.toArray());
        List<Day> days=new ArrayList<>();long total=0;
        for(var date=start;!date.isAfter(end);date=date.plusDays(1)) {long count=counts.getOrDefault(date,0L);days.add(new Day(date,count));total+=count;}
        return new Activity(start,end,total,counts.size(),days);
    }
    public List<Submission> recent(long id) {return recent(id,null);}
    public List<Submission> recent(long id,String requestedPlatform) {
        requireUser(id);
        String selected=platform(requestedPlatform);
        return jdbc.query("SELECT s.id,s.platform,s.external_problem_key,s.problem_id,COALESCE(p.title,NULLIF(s.external_problem_title,''),s.external_problem_key) AS title,s.verdict,s.language,s.submitted_time,p.status AS problem_status FROM oj_submission s LEFT JOIN problem p ON p.id=s.problem_id WHERE s.user_id=?"+(selected==null?"":" AND s.platform=?")+" ORDER BY s.submitted_time DESC,s.id DESC LIMIT 10",
                (rs,n)->new Submission(rs.getLong("id"),rs.getString("platform"),rs.getString("external_problem_key"),
                        rs.getObject("problem_status")!=null&&rs.getInt("problem_status")==1?((Number)rs.getObject("problem_id")).longValue():null,
                        rs.getString("title"),rs.getString("verdict"),rs.getString("language"),rs.getTimestamp("submitted_time").toLocalDateTime()),selected==null?new Object[]{id}:new Object[]{id,selected});
    }
    public record OjAccountView(String platform,String uid,LocalDateTime lastSyncTime) {}
    public record OjStats(long solvedCount,long solvedCount30d,long solvedCount7d,LocalDateTime lastAcceptedTime) {}
    public record Overview(List<OjAccountView> accounts,OjStats stats,long todaySubmissions,long todayAttempted,long todaySolved) {}
    public Overview overview(long id,String requestedPlatform) {
        requireUser(id);String selected=platform(requestedPlatform);
        var accounts=jdbc.query("SELECT platform,external_user_id,last_sync_time FROM oj_account WHERE user_id=? AND verify_status=1 AND platform IN ('CODEFORCES','NOWCODER') ORDER BY platform",(rs,n)->new OjAccountView(rs.getString(1),rs.getString(2),rs.getTimestamp(3)==null?null:rs.getTimestamp(3).toLocalDateTime()),id);
        var today=LocalDate.now(ZoneId.of("Asia/Shanghai"));
        var args=new ArrayList<Object>(List.of(today.minusDays(30),today.minusDays(7),today,today.plusDays(1),id));if(selected!=null)args.add(selected);
        var row=jdbc.queryForMap("SELECT COUNT(*) total,COALESCE(SUM(s.submitted_time>=?),0) d30,COALESCE(SUM(s.submitted_time>=?),0) d7,MAX(s.submitted_time) latest,COALESCE(SUM(s.submitted_time>=? AND s.submitted_time<?),0) today FROM oj_first_ac f JOIN oj_submission s ON s.id=f.submission_id JOIN oj_account a ON a.user_id=f.user_id AND a.platform=f.platform AND a.verify_status=1 WHERE f.user_id=?"+(selected==null?"":" AND f.platform=?"),args.toArray());
        var dailyArgs=new ArrayList<Object>(List.of(id,today,today.plusDays(1)));if(selected!=null)dailyArgs.add(selected);
        var daily=jdbc.queryForMap("SELECT COUNT(*) submissions,COUNT(DISTINCT platform,external_problem_key) attempted FROM oj_submission WHERE user_id=? AND submitted_time>=? AND submitted_time<?"+(selected==null?"":" AND platform=?"),dailyArgs.toArray());
        Object latest=row.get("latest");LocalDateTime time=latest==null?null:latest instanceof java.sql.Timestamp stamp?stamp.toLocalDateTime():(LocalDateTime)latest;
        return new Overview(accounts,new OjStats(((Number)row.get("total")).longValue(),((Number)row.get("d30")).longValue(),((Number)row.get("d7")).longValue(),time),((Number)daily.get("submissions")).longValue(),((Number)daily.get("attempted")).longValue(),((Number)row.get("today")).longValue());
    }
}
