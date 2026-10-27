package com.itnoduck.acmate.oj.service;

import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.oj.client.NowcoderPublicClient;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import com.itnoduck.acmate.problem.service.NowcoderProblemImportService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.time.*;
import java.util.*;
import java.util.regex.*;

@Service
public class NowcoderTrainingService {
    private final JdbcTemplate jdbc;
    private final ProblemMapper problems;
    private final NowcoderProblemImportService importer;
    private final NowcoderPublicClient client;
    private final TransactionTemplate transaction;
    public NowcoderTrainingService(JdbcTemplate jdbc,ProblemMapper problems,NowcoderProblemImportService importer,NowcoderPublicClient client,PlatformTransactionManager manager){
        this.jdbc=jdbc;this.problems=problems;this.importer=importer;this.client=client;transaction=new TransactionTemplate(manager);
    }
    public record Account(boolean hasAccount,Long id,String uid,Integer verifyStatus){}
    public record Training(String officialUrl,boolean syncSupported,String syncNotice){}
    public record Sync(int fetched,int inserted,int accepted,String message){}
    public Account account(long user){
        var rows=jdbc.query("SELECT id,external_user_id,verify_status FROM oj_account WHERE user_id=? AND platform='NOWCODER'",(rs,n)->new Account(true,rs.getLong(1),rs.getString(2),rs.getInt(3)),user);
        return rows.isEmpty()?new Account(false,null,null,null):rows.get(0);
    }
    @org.springframework.transaction.annotation.Transactional
    public void bind(long user,String uid){
        if(uid==null||!uid.strip().matches("[1-9][0-9]{0,11}"))throw new BusinessException(400,"请输入牛客个人主页中的数字 UID");
        if(account(user).hasAccount())throw new BusinessException(409,"已绑定牛客账号，请先解绑");
        var owners=jdbc.queryForList("SELECT user_id FROM nowcoder_account_owner WHERE uid=?",Long.class,uid.strip());
        if(!owners.isEmpty() && owners.get(0).longValue()!=user)throw new BusinessException(409,"该牛客账号曾绑定其他用户，请联系管理员处理");
        try{jdbc.update("INSERT INTO oj_account(user_id,platform,external_user_id,display_name,verify_status,sync_enabled) VALUES(?,'NOWCODER',?,?,0,1)",user,uid.strip(),uid.strip());}
        catch(org.springframework.dao.DuplicateKeyException ex){throw new BusinessException(409,"该牛客账号已被绑定");}
    }
    public void unbind(long user){jdbc.update("DELETE FROM oj_account WHERE user_id=? AND platform='NOWCODER'",user);}
    public Training training(long id){
        var problem=problems.selectById(id);
        if(problem==null||!Integer.valueOf(1).equals(problem.getStatus()))throw new BusinessException(404,"题目不存在或已停用");
        if(!"NOWCODER".equals(problem.getPlatform())||problem.getSourceUrl()==null)throw new BusinessException(400,"此题没有可用的牛客来源链接");
        String url=importer.canonicalUrl(problem.getSourceUrl()).toString();
        boolean supported=Pattern.matches("https://ac\\.nowcoder\\.com/acm/contest/[0-9]+/[A-Z][A-Z0-9]*",url)&&problem.getExternalProblemKey()!=null&&problem.getExternalProblemKey().matches("NC[1-9][0-9]*");
        return new Training(url,supported,supported?"同步此题最近最多 60 条公开正式记录；需绑定并审核牛客 UID":"此来源暂不支持公开正式记录同步，请在牛客查看结果；可使用公开竞赛题链接重新导入");
    }
    public List<Map<String,Object>> records(long problem,long user){
        return jdbc.queryForList("SELECT remote_submission_id AS remoteId,verdict,language,submitted_time AS submittedTime FROM oj_submission WHERE platform='NOWCODER' AND problem_id=? AND user_id=? ORDER BY submitted_time DESC,id DESC LIMIT 10",problem,user);
    }
    public Sync sync(long id,long user){
        var info=training(id);
        if(!info.syncSupported())throw new BusinessException(422,info.syncNotice());
        var account=account(user);
        if(!account.hasAccount()||!Integer.valueOf(1).equals(account.verifyStatus()))throw new BusinessException(409,"请先绑定牛客 UID 并等待管理员审核");
        // A conditional database update rate-limits concurrent requests across instances.
        if(jdbc.update("UPDATE oj_account SET last_sync_time=NOW() WHERE id=? AND (last_sync_time IS NULL OR last_sync_time<DATE_SUB(NOW(),INTERVAL 20 SECOND))",account.id())!=1)
            throw new BusinessException(429,"请间隔 20 秒后再同步");
        var problem=problems.selectById(id);
        long contest=Long.parseLong(info.officialUrl().split("/")[5]);
        long remoteProblem=Long.parseLong(problem.getExternalProblemKey().substring(2));
        var rows=client.submissions(contest,Long.parseLong(account.uid()),remoteProblem);
        int[] counts={0,0};
        transaction.executeWithoutResult(status->{
            // Binding changes cannot race with persistence of official results.
            var locked=jdbc.queryForList("SELECT id FROM oj_account WHERE id=? AND user_id=? AND platform='NOWCODER' AND external_user_id=? AND verify_status=1 FOR UPDATE",account.id(),user,account.uid());
            if(locked.isEmpty())throw new BusinessException(409,"绑定信息已变化，请重新同步");
            for(var row:rows){
                int inserted=jdbc.update("INSERT IGNORE INTO oj_submission(oj_account_id,user_id,platform,remote_submission_id,problem_id,external_problem_key,verdict,language,submitted_time,is_first_ac,nowcoder_uid) VALUES(?,?,'NOWCODER',?,?,?,?,?,?,0,?)",account.id(),user,Long.toString(row.id()),id,problem.getExternalProblemKey(),row.verdict(),row.language(),row.submittedTime(),account.uid());
                var owners=jdbc.queryForList("SELECT id,user_id FROM oj_submission WHERE platform='NOWCODER' AND remote_submission_id=? FOR UPDATE",Long.toString(row.id()));
                if(owners.isEmpty()||((Number)owners.get(0).get("user_id")).longValue()!=user)throw new BusinessException(409,"官方提交归属冲突，请联系管理员");
                long localId=((Number)owners.get(0).get("id")).longValue();counts[0]+=inserted;
                if(inserted>0)jdbc.update("INSERT INTO user_problem_status(user_id,problem_id,status,attempt_count,first_submit_time,last_submit_time) VALUES(?,?,1,1,?,?) ON DUPLICATE KEY UPDATE status=GREATEST(status,1),attempt_count=attempt_count+1,first_submit_time=COALESCE(LEAST(first_submit_time,VALUES(first_submit_time)),VALUES(first_submit_time)),last_submit_time=COALESCE(GREATEST(last_submit_time,VALUES(last_submit_time)),VALUES(last_submit_time))",user,id,row.submittedTime(),row.submittedTime());
                jdbc.update("UPDATE oj_submission SET verdict=?,problem_id=?,external_problem_key=?,nowcoder_uid=COALESCE(nowcoder_uid,?) WHERE id=?",row.verdict(),id,problem.getExternalProblemKey(),account.uid(),localId);
                if("OK".equals(row.verdict())){
                    counts[1]++;
                    jdbc.update("INSERT INTO oj_first_ac(user_id,platform,external_problem_key,submission_id,create_time) VALUES(?,'NOWCODER',?,?,?) ON DUPLICATE KEY UPDATE submission_id=IF(VALUES(create_time)<create_time,VALUES(submission_id),submission_id),create_time=LEAST(create_time,VALUES(create_time))",user,problem.getExternalProblemKey(),localId,row.submittedTime());
                    jdbc.update("INSERT INTO user_problem_status(user_id,problem_id,status,first_ac_time,first_submit_time,last_submit_time,accepted_submission_id,solve_source) VALUES(?,?,2,?,?,?,?, 'NOWCODER') ON DUPLICATE KEY UPDATE status=2,accepted_submission_id=IF(first_ac_time IS NULL OR VALUES(first_ac_time)<first_ac_time OR (VALUES(first_ac_time)=first_ac_time AND accepted_submission_id IS NOT NULL AND VALUES(accepted_submission_id)<accepted_submission_id),VALUES(accepted_submission_id),accepted_submission_id),solve_source=IF(first_ac_time IS NULL OR VALUES(first_ac_time)<first_ac_time,'NOWCODER',solve_source),first_ac_time=COALESCE(LEAST(first_ac_time,VALUES(first_ac_time)),first_ac_time,VALUES(first_ac_time)),last_submit_time=COALESCE(GREATEST(last_submit_time,VALUES(last_submit_time)),VALUES(last_submit_time))",user,id,row.submittedTime(),row.submittedTime(),row.submittedTime(),localId);
                }
            }
            var earliest=jdbc.queryForList("SELECT id,submitted_time FROM oj_submission WHERE user_id=? AND platform='NOWCODER' AND external_problem_key=? AND verdict='OK' ORDER BY submitted_time,id LIMIT 1",user,problem.getExternalProblemKey());
            if(earliest.isEmpty()) {
                jdbc.update("DELETE FROM oj_first_ac WHERE user_id=? AND platform='NOWCODER' AND external_problem_key=?",user,problem.getExternalProblemKey());
                jdbc.update("UPDATE user_problem_status SET status=1,first_ac_time=NULL,accepted_submission_id=NULL,solve_source=NULL WHERE user_id=? AND problem_id=? AND solve_source='NOWCODER'",user,id);
            } else {
                var first=earliest.get(0);
                jdbc.update("INSERT INTO oj_first_ac(user_id,platform,external_problem_key,submission_id,create_time) VALUES(?,'NOWCODER',?,?,?) ON DUPLICATE KEY UPDATE submission_id=VALUES(submission_id),create_time=VALUES(create_time)",user,problem.getExternalProblemKey(),first.get("id"),first.get("submitted_time"));
                jdbc.update("UPDATE user_problem_status SET accepted_submission_id=IF(solve_source='NOWCODER' OR first_ac_time IS NULL OR ?<first_ac_time,?,accepted_submission_id),solve_source=IF(solve_source='NOWCODER' OR first_ac_time IS NULL OR ?<first_ac_time,'NOWCODER',solve_source),first_ac_time=IF(solve_source='NOWCODER',?,first_ac_time),status=2 WHERE user_id=? AND problem_id=?",first.get("submitted_time"),first.get("id"),first.get("submitted_time"),first.get("submitted_time"),user,id);
            }
            jdbc.update("UPDATE oj_submission s LEFT JOIN oj_first_ac f ON f.submission_id=s.id AND f.user_id=s.user_id SET s.is_first_ac=IF(f.id IS NULL,0,1) WHERE s.user_id=? AND s.platform='NOWCODER' AND s.external_problem_key=?",user,problem.getExternalProblemKey());
            jdbc.update("UPDATE oj_account SET last_sync_success=1 WHERE id=?",account.id());
        });
        return new Sync(rows.size(),counts[0],counts[1],rows.isEmpty()?"未找到本人此题的公开终态记录；可能仍在评测、记录未公开或超出最近查询范围":"已同步牛客官方结果");
    }
}
