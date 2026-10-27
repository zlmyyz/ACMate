package com.itnoduck.acmate.user.service;
import com.itnoduck.acmate.common.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

/** Public profile excludes submissions of previously unbound Nowcoder identities. */
@Service
public class PublicUserActivityService {
 private final JdbcTemplate jdbc;
 public PublicUserActivityService(JdbcTemplate jdbc){this.jdbc=jdbc;}
 private void requireUser(long id){if(jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE id=?",Long.class,id)==0)throw new BusinessException(404,"用户不存在");}
 private String platform(String value){if(value==null||value.equals("ALL"))return null;if(!List.of("CODEFORCES","NOWCODER").contains(value))throw new BusinessException(400,"不支持的 OJ 平台");return value;}
 private String visible(){return " AND (s.platform<>'NOWCODER' OR EXISTS(SELECT 1 FROM oj_account a WHERE a.user_id=s.user_id AND a.platform='NOWCODER' AND a.verify_status=1 AND a.external_user_id=s.nowcoder_uid))";}
 private String filter(String selected){return selected==null?"":" AND s.platform=?";}
 private Object[] args(long user,String selected,Object... tail){var args=new ArrayList<Object>();args.add(user);if(selected!=null)args.add(selected);args.addAll(Arrays.asList(tail));return args.toArray();}
 public UserActivityService.Activity activity(long id,String requested){
  requireUser(id);String selected=platform(requested);var end=LocalDate.now(ZoneId.of("Asia/Shanghai"));var start=end.minusDays(364);var counts=new HashMap<LocalDate,Long>();
  jdbc.query("SELECT DATE(s.submitted_time) day,COUNT(*) n FROM oj_submission s WHERE s.user_id=?"+filter(selected)+visible()+" AND s.submitted_time>=? AND s.submitted_time<? GROUP BY DATE(s.submitted_time)",(org.springframework.jdbc.core.RowCallbackHandler)rs->counts.put(rs.getDate("day").toLocalDate(),rs.getLong("n")),args(id,selected,start,end.plusDays(1)));
  List<UserActivityService.Day> days=new ArrayList<>();long total=0;for(var date=start;!date.isAfter(end);date=date.plusDays(1)){long n=counts.getOrDefault(date,0L);days.add(new UserActivityService.Day(date,n));total+=n;}return new UserActivityService.Activity(start,end,total,counts.size(),days);
 }
 public List<UserActivityService.Submission> recent(long id,String requested){
  requireUser(id);String selected=platform(requested);
  return jdbc.query("SELECT s.id,s.platform,s.external_problem_key,s.problem_id,COALESCE(p.title,NULLIF(s.external_problem_title,''),s.external_problem_key) title,s.verdict,s.language,s.submitted_time,p.status problem_status FROM oj_submission s LEFT JOIN problem p ON p.id=s.problem_id WHERE s.user_id=?"+filter(selected)+visible()+" ORDER BY s.submitted_time DESC,s.id DESC LIMIT 10",(rs,n)->new UserActivityService.Submission(rs.getLong("id"),rs.getString("platform"),rs.getString("external_problem_key"),rs.getObject("problem_status")!=null&&rs.getInt("problem_status")==1?((Number)rs.getObject("problem_id")).longValue():null,rs.getString("title"),rs.getString("verdict"),rs.getString("language"),rs.getTimestamp("submitted_time").toLocalDateTime()),args(id,selected));
 }
 public UserActivityService.Overview overview(long id,String requested){
  requireUser(id);String selected=platform(requested);var today=LocalDate.now(ZoneId.of("Asia/Shanghai"));
  String base=" FROM oj_submission s WHERE s.user_id=?"+filter(selected)+visible();
  var rows=jdbc.query("SELECT s.platform,s.external_problem_key,MIN(s.submitted_time) first_ac"+base+" AND s.verdict='OK' AND EXISTS(SELECT 1 FROM oj_account a WHERE a.user_id=s.user_id AND a.platform=s.platform AND a.verify_status=1) GROUP BY s.platform,s.external_problem_key",(rs,n)->rs.getTimestamp("first_ac").toLocalDateTime(),args(id,selected));
  long d30=0,d7=0,solvedToday=0;LocalDateTime latest=null;for(var t:rows){if(!t.isBefore(today.minusDays(30).atStartOfDay()))d30++;if(!t.isBefore(today.minusDays(7).atStartOfDay()))d7++;if(t.toLocalDate().equals(today))solvedToday++;if(latest==null||t.isAfter(latest))latest=t;}
  var daily=jdbc.queryForMap("SELECT COUNT(*) submissions,COUNT(DISTINCT s.platform,s.external_problem_key) attempted"+base+" AND s.submitted_time>=? AND s.submitted_time<?",args(id,selected,today,today.plusDays(1)));
  var accounts=jdbc.query("SELECT platform,external_user_id,last_sync_time FROM oj_account WHERE user_id=? AND verify_status=1 AND platform IN ('CODEFORCES','NOWCODER') ORDER BY platform",(rs,n)->new UserActivityService.OjAccountView(rs.getString(1),rs.getString(2),rs.getTimestamp(3)==null?null:rs.getTimestamp(3).toLocalDateTime()),id);
  return new UserActivityService.Overview(accounts,new UserActivityService.OjStats(rows.size(),d30,d7,latest),((Number)daily.get("submissions")).longValue(),((Number)daily.get("attempted")).longValue(),solvedToday);
 }
}
