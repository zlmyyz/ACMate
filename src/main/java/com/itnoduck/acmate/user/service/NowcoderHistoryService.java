package com.itnoduck.acmate.user.service;
import com.itnoduck.acmate.common.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

/** Account identity is copied onto each submission; missing historical identities remain explicitly unknown. */
@Service
public class NowcoderHistoryService {
 private final JdbcTemplate jdbc;
 public NowcoderHistoryService(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public record Account(String uid,long submissions){}
 public String currentUid(long user){
  require(user,null);var values=jdbc.queryForList("SELECT external_user_id FROM oj_account WHERE user_id=? AND platform='NOWCODER' AND verify_status=1",String.class,user);return values.isEmpty()?null:values.get(0);
 }
 private void require(long user,String uid){
  if(jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE id=?",Long.class,user)==0)throw new BusinessException(404,"用户不存在");
  if(uid!=null&&!uid.equals("UNKNOWN")&&!uid.matches("[1-9][0-9]{0,11}"))throw new BusinessException(400,"牛客 UID 无效");
 }
 private String condition(String uid){return uid.equals("UNKNOWN")?"nowcoder_uid IS NULL":"nowcoder_uid=?";}
 private Object[] args(long user,String uid,Object... extra){var result=new ArrayList<Object>();result.add(user);if(!uid.equals("UNKNOWN"))result.add(uid);result.addAll(Arrays.asList(extra));return result.toArray();}
 public List<Account> accounts(long user){
  require(user,null);
  var rows=new ArrayList<>(jdbc.query("SELECT COALESCE(nowcoder_uid,'UNKNOWN') uid,COUNT(*) n FROM oj_submission WHERE user_id=? AND platform='NOWCODER' GROUP BY nowcoder_uid ORDER BY MAX(submitted_time) DESC",(rs,n)->new Account(rs.getString("uid"),rs.getLong("n")),user));
  var current=jdbc.queryForList("SELECT external_user_id FROM oj_account WHERE user_id=? AND platform='NOWCODER' AND verify_status=1",String.class,user);
  if(!current.isEmpty()&&rows.stream().noneMatch(row->row.uid().equals(current.get(0))))rows.add(0,new Account(current.get(0),0));
  return rows;
 }
 public UserActivityService.Activity activity(long user,String uid){
  require(user,uid);var end=LocalDate.now(ZoneId.of("Asia/Shanghai"));var start=end.minusDays(364);var counts=new HashMap<LocalDate,Long>();
  jdbc.query("SELECT DATE(submitted_time) day,COUNT(*) n FROM oj_submission WHERE user_id=? AND platform='NOWCODER' AND "+condition(uid)+" AND submitted_time>=? AND submitted_time<? GROUP BY DATE(submitted_time)",(org.springframework.jdbc.core.RowCallbackHandler)rs->counts.put(rs.getDate("day").toLocalDate(),rs.getLong("n")),args(user,uid,start,end.plusDays(1)));
  var days=new ArrayList<UserActivityService.Day>();long total=0;for(var date=start;!date.isAfter(end);date=date.plusDays(1)){long count=counts.getOrDefault(date,0L);days.add(new UserActivityService.Day(date,count));total+=count;}return new UserActivityService.Activity(start,end,total,counts.size(),days);
 }
 public List<UserActivityService.Submission> recent(long user,String uid){
  require(user,uid);
  return jdbc.query("SELECT s.id,s.platform,s.external_problem_key,s.problem_id,COALESCE(p.title,NULLIF(s.external_problem_title,''),s.external_problem_key) title,s.verdict,s.language,s.submitted_time,p.status problem_status FROM oj_submission s LEFT JOIN problem p ON p.id=s.problem_id WHERE s.user_id=? AND s.platform='NOWCODER' AND s."+condition(uid)+" ORDER BY s.submitted_time DESC,s.id DESC LIMIT 10",(rs,n)->new UserActivityService.Submission(rs.getLong("id"),rs.getString("platform"),rs.getString("external_problem_key"),rs.getObject("problem_status")!=null&&rs.getInt("problem_status")==1?((Number)rs.getObject("problem_id")).longValue():null,rs.getString("title"),rs.getString("verdict"),rs.getString("language"),rs.getTimestamp("submitted_time").toLocalDateTime()),args(user,uid));
 }
 public UserActivityService.Overview overview(long user,String uid){
  require(user,uid);var today=LocalDate.now(ZoneId.of("Asia/Shanghai"));
  String base=" FROM oj_submission WHERE user_id=? AND platform='NOWCODER' AND "+condition(uid);
  var rows=jdbc.query("SELECT external_problem_key,MIN(submitted_time) first_ac"+base+" AND verdict='OK' GROUP BY external_problem_key",(rs,n)->rs.getTimestamp("first_ac").toLocalDateTime(),args(user,uid));
  long d30=0,d7=0,solvedToday=0;LocalDateTime latest=null;
  for(var time:rows){if(!time.isBefore(today.minusDays(30).atStartOfDay()))d30++;if(!time.isBefore(today.minusDays(7).atStartOfDay()))d7++;if(time.toLocalDate().equals(today))solvedToday++;if(latest==null||time.isAfter(latest))latest=time;}
  var daily=jdbc.queryForMap("SELECT COUNT(*) submissions,COUNT(DISTINCT external_problem_key) attempted"+base+" AND submitted_time>=? AND submitted_time<?",args(user,uid,today,today.plusDays(1)));
  var accounts=jdbc.query("SELECT platform,external_user_id,last_sync_time FROM oj_account WHERE user_id=? AND platform='NOWCODER' AND verify_status=1",(rs,n)->new UserActivityService.OjAccountView(rs.getString(1),rs.getString(2),rs.getTimestamp(3)==null?null:rs.getTimestamp(3).toLocalDateTime()),user);
  return new UserActivityService.Overview(accounts,new UserActivityService.OjStats(rows.size(),d30,d7,latest),((Number)daily.get("submissions")).longValue(),((Number)daily.get("attempted")).longValue(),solvedToday);
 }
}
