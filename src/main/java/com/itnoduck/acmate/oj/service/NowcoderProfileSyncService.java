package com.itnoduck.acmate.oj.service;

import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.oj.client.NowcoderProfileClient;
import com.itnoduck.acmate.problem.service.NowcoderProblemImportService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import java.util.*;

/** Durable, leased queues; each worker invocation performs at most one remote page or one import. */
@Service
public class NowcoderProfileSyncService {
    private final JdbcTemplate jdbc;
    private final NowcoderProfileClient client;
    private final NowcoderProblemImportService importer;
    private final TransactionTemplate tx;
    public NowcoderProfileSyncService(JdbcTemplate jdbc, NowcoderProfileClient client, NowcoderProblemImportService importer, PlatformTransactionManager manager) {
        this.jdbc=jdbc;this.client=client;this.importer=importer;this.tx=new TransactionTemplate(manager);
    }
    public record State(String status,int nextPage,int totalPages,int fetched,int inserted,String error,LocalDateTime updatedAt,long importPending,long importDone,long importFailed) {}
    private long number(Map<String,Object> row,String field){return ((Number)row.get(field)).longValue();}
    private LocalDateTime time(Object value){return value==null?null:value instanceof java.sql.Timestamp stamp?stamp.toLocalDateTime():(LocalDateTime)value;}
    private Map<String,Object> account(long user) {
        var rows=jdbc.queryForList("SELECT a.* FROM oj_account a JOIN app_user u ON u.id=a.user_id WHERE a.user_id=? AND a.platform='NOWCODER' AND a.verify_status=1 AND a.sync_enabled=1 AND u.status=1",user);
        if(rows.isEmpty())throw new BusinessException(409,"请先绑定牛客账号并通过管理员审核");
        return rows.get(0);
    }
    public State state(long user) {
        var rows=jdbc.queryForList("SELECT j.* FROM nowcoder_sync_job j JOIN oj_account a ON a.id=j.account_id AND a.user_id=j.user_id AND a.external_user_id=j.uid WHERE a.user_id=? AND a.platform='NOWCODER'",user);
        var counts=jdbc.queryForMap("SELECT COALESCE(SUM(q.status IN ('QUEUED','RUNNING')),0) pending,COALESCE(SUM(q.status='COMPLETED'),0) done,COALESCE(SUM(q.status='FAILED'),0) failed FROM nowcoder_problem_import_job q JOIN oj_first_ac f ON f.platform='NOWCODER' AND f.external_problem_key=q.external_problem_key WHERE f.user_id=?",user);
        var row=rows.isEmpty()?null:rows.get(0);
        return new State(row==null?"NOT_STARTED":(String)row.get("status"),row==null?1:(int)number(row,"next_page"),row==null?0:(int)number(row,"total_pages"),row==null?0:(int)number(row,"fetched_count"),row==null?0:(int)number(row,"inserted_count"),row==null?null:(String)row.get("error_message"),row==null?null:time(row.get("update_time")),number(counts,"pending"),number(counts,"done"),number(counts,"failed"));
    }
    public State enqueue(long user) {
        var account=account(user);long aid=number(account,"id");String uid=(String)account.get("external_user_id");
        if(!uid.matches("[1-9][0-9]{0,11}"))throw new BusinessException(409,"牛客 UID 无效，请重新绑定");
        tx.executeWithoutResult(status->{
            int created=jdbc.update("INSERT IGNORE INTO nowcoder_sync_job(account_id,user_id,uid) VALUES(?,?,?)",aid,user,uid);
            if(created==0) {
                var job=jdbc.queryForMap("SELECT * FROM nowcoder_sync_job WHERE account_id=? FOR UPDATE",aid);
                if(List.of("QUEUED","RUNNING").contains(job.get("status")))return;
                boolean resume="FAILED".equals(job.get("status"));
                int changed=jdbc.update("UPDATE nowcoder_sync_job SET status='QUEUED',next_page=IF(?,next_page,1),total_pages=IF(?,total_pages,0),fetched_count=IF(?,fetched_count,0),inserted_count=IF(?,inserted_count,0),attempts=0,error_message=NULL,lease_token=NULL,lease_until=NULL,next_run_time=NOW(),request_time=NOW(),user_id=?,uid=? WHERE id=? AND request_time<DATE_SUB(NOW(),INTERVAL 60 SECOND)",resume,resume,resume,resume,user,uid,number(job,"id"));
                if(changed==0)throw new BusinessException(429,"请间隔 60 秒后再发起同步");
            }
        });
        return state(user);
    }
    public void enqueueEligible() {
        var users=jdbc.queryForList("SELECT a.user_id FROM oj_account a JOIN app_user u ON u.id=a.user_id LEFT JOIN nowcoder_sync_job j ON j.account_id=a.id WHERE a.platform='NOWCODER' AND a.verify_status=1 AND a.sync_enabled=1 AND u.status=1 AND (j.id IS NULL OR (j.status='COMPLETED' AND j.request_time<DATE_SUB(NOW(),INTERVAL 1 HOUR))) ORDER BY a.id LIMIT 10",Long.class);
        for(long user:users)try{enqueue(user);}catch(BusinessException ignored){}
    }
    private Map<String,Object> claim(String table) {
        // table is selected by internal callers, never by request data.
        return tx.execute(status->{
            var rows=jdbc.queryForList("SELECT * FROM "+table+" WHERE (status='QUEUED' AND next_run_time<=NOW()) OR (status='RUNNING' AND lease_until<NOW()) ORDER BY next_run_time,id LIMIT 1 FOR UPDATE SKIP LOCKED");
            if(rows.isEmpty())return null;
            var row=rows.get(0);String token=UUID.randomUUID().toString();
            jdbc.update("UPDATE "+table+" SET status='RUNNING',lease_token=?,lease_until=DATE_ADD(NOW(),INTERVAL 2 MINUTE) WHERE id=?",token,row.get("id"));
            row.put("lease_token",token);return row;
        });
    }
    private boolean lockJob(String table,Map<String,Object> job) {
        return !jdbc.queryForList("SELECT id FROM "+table+" WHERE id=? AND lease_token=? AND status='RUNNING' FOR UPDATE",job.get("id"),job.get("lease_token")).isEmpty();
    }
    private void fail(String table,Map<String,Object> job,Exception ex) {
        boolean cancelled=ex instanceof BusinessException b&&b.getCode()==409;
        String message=ex instanceof BusinessException?ex.getMessage():"后台任务暂时失败，请稍后重试";
        if(message.length()>255)message=message.substring(0,255);
        jdbc.update("UPDATE "+table+" SET status=IF(?,'CANCELLED',IF(attempts>=4,'FAILED','QUEUED')),attempts=attempts+1,error_message=?,next_run_time=DATE_ADD(NOW(),INTERVAL 30 SECOND),lease_token=NULL,lease_until=NULL WHERE id=? AND lease_token=?",cancelled,message,job.get("id"),job.get("lease_token"));
    }
    public void processSync() {
        var job=claim("nowcoder_sync_job");if(job==null)return;
        try {
            long user=number(job,"user_id");var bound=account(user);
            if(number(bound,"id")!=number(job,"account_id")||!bound.get("external_user_id").equals(job.get("uid")))throw new BusinessException(409,"牛客绑定已变化，任务已取消");
            int next=(int)number(job,"next_page");var page=client.page(Long.parseLong((String)job.get("uid")),next);
            tx.executeWithoutResult(status->{
                if(!lockJob("nowcoder_sync_job",job))return;
                if(jdbc.queryForList("SELECT id FROM oj_account WHERE id=? AND user_id=? AND external_user_id=? AND verify_status=1 AND sync_enabled=1 FOR UPDATE",job.get("account_id"),user,job.get("uid")).isEmpty())throw new BusinessException(409,"牛客绑定已变化，任务已取消");
                int inserted=0;
                var keys=new HashSet<String>();
                for(var row:page.submissions()) {
                    String key="NC"+row.problemId();keys.add(key);Long local=findProblem(key);
                    inserted+=jdbc.update("INSERT IGNORE INTO oj_submission(oj_account_id,user_id,platform,remote_submission_id,problem_id,external_problem_key,external_problem_title,verdict,language,submitted_time,is_first_ac,nowcoder_uid) VALUES(?,?,'NOWCODER',?,?,?,?,?,?,?,0,?)",job.get("account_id"),user,Long.toString(row.id()),local,key,row.title(),row.verdict(),row.language(),row.time(),job.get("uid"));
                    var stored=jdbc.queryForMap("SELECT id,user_id FROM oj_submission WHERE platform='NOWCODER' AND remote_submission_id=? FOR UPDATE",Long.toString(row.id()));
                    if(number(stored,"user_id")!=user)throw new BusinessException(409,"官方提交归属冲突，请联系管理员");
                    long sid=number(stored,"id");
                    jdbc.update("UPDATE oj_submission SET verdict=?,language=?,external_problem_title=?,problem_id=?,submitted_time=?,nowcoder_uid=COALESCE(nowcoder_uid,?) WHERE id=?",row.verdict(),row.language(),row.title(),local,row.time(),job.get("uid"),sid);
                    if("OK".equals(row.verdict())) {
                        jdbc.update("INSERT INTO oj_first_ac(user_id,platform,external_problem_key,submission_id,create_time) VALUES(?,'NOWCODER',?,?,?) ON DUPLICATE KEY UPDATE submission_id=IF(VALUES(create_time)<create_time,VALUES(submission_id),submission_id),create_time=LEAST(create_time,VALUES(create_time))",user,key,sid,row.time());
                        // Database first: an existing NC number never triggers another remote import.
                        if(local==null)jdbc.update("INSERT INTO nowcoder_problem_import_job(external_problem_key) VALUES(?) ON DUPLICATE KEY UPDATE attempts=IF(status='CANCELLED',0,attempts),next_run_time=IF(status='CANCELLED',NOW(),next_run_time),status=IF(status='CANCELLED','QUEUED',status)",key);
                    }
                }
                for(String key:keys) {
                    reconcileFirstAc(user,key);
                    jdbc.update("UPDATE oj_submission s LEFT JOIN oj_first_ac f ON f.submission_id=s.id SET s.is_first_ac=IF(f.id IS NULL,0,1) WHERE s.user_id=? AND s.platform='NOWCODER' AND s.external_problem_key=?",user,key);
                    Long local=findProblem(key);if(local!=null)linkProblem(key,local,user);
                }
                boolean complete=next>=page.totalPages();
                jdbc.update("UPDATE nowcoder_sync_job SET status=?,next_page=?,total_pages=?,fetched_count=fetched_count+?,inserted_count=inserted_count+?,attempts=0,error_message=NULL,lease_token=NULL,lease_until=NULL,next_run_time=DATE_ADD(NOW(),INTERVAL 2 SECOND) WHERE id=? AND lease_token=?",complete?"COMPLETED":"QUEUED",next+1,page.totalPages(),page.submissions().size(),inserted,job.get("id"),job.get("lease_token"));
                if(complete)jdbc.update("UPDATE oj_account SET last_sync_time=NOW(),last_sync_success=1 WHERE id=?",job.get("account_id"));
            });
        }catch(Exception ex){fail("nowcoder_sync_job",job,ex);}
    }
    private Long findProblem(String key) {
        var rows=jdbc.queryForList("SELECT id FROM problem WHERE platform='NOWCODER' AND external_problem_key=?",Long.class,key);
        return rows.isEmpty()?null:rows.get(0);
    }
    private void reconcileFirstAc(long user,String key) {
        var accepted=jdbc.queryForList("SELECT id,submitted_time FROM oj_submission WHERE user_id=? AND platform='NOWCODER' AND external_problem_key=? AND verdict='OK' ORDER BY submitted_time,id LIMIT 1",user,key);
        if(accepted.isEmpty())jdbc.update("DELETE FROM oj_first_ac WHERE user_id=? AND platform='NOWCODER' AND external_problem_key=?",user,key);
        else jdbc.update("INSERT INTO oj_first_ac(user_id,platform,external_problem_key,submission_id,create_time) VALUES(?,'NOWCODER',?,?,?) ON DUPLICATE KEY UPDATE submission_id=VALUES(submission_id),create_time=VALUES(create_time)",user,key,accepted.get(0).get("id"),accepted.get(0).get("submitted_time"));
    }
    /** Also backfills failed attempts and first AC after the asynchronously imported statement arrives. */
    private void linkProblem(String key,long id,Long user) {
        if(user==null) jdbc.update("UPDATE oj_submission SET problem_id=? WHERE platform='NOWCODER' AND external_problem_key=? AND (problem_id IS NULL OR problem_id<>?)",id,key,id);
        else jdbc.update("UPDATE oj_submission SET problem_id=? WHERE platform='NOWCODER' AND external_problem_key=? AND user_id=? AND (problem_id IS NULL OR problem_id<>?)",id,key,user,id);
        jdbc.update("INSERT INTO user_problem_status(user_id,problem_id,status,attempt_count,first_submit_time,last_submit_time,first_ac_time,accepted_submission_id,solve_source) SELECT s.user_id,?,IF(MIN(f.submission_id) IS NULL,1,2),COUNT(*),MIN(s.submitted_time),MAX(s.submitted_time),MIN(CASE WHEN f.submission_id IS NOT NULL THEN s.submitted_time END),MIN(f.submission_id),'NOWCODER' FROM oj_submission s LEFT JOIN oj_first_ac f ON f.submission_id=s.id WHERE s.platform='NOWCODER' AND s.external_problem_key=?"+(user==null?"":" AND s.user_id=?")+" GROUP BY s.user_id ON DUPLICATE KEY UPDATE status=IF(solve_source='NOWCODER',VALUES(status),GREATEST(status,VALUES(status))),attempt_count=GREATEST(attempt_count,VALUES(attempt_count)),first_submit_time=COALESCE(LEAST(first_submit_time,VALUES(first_submit_time)),first_submit_time,VALUES(first_submit_time)),last_submit_time=COALESCE(GREATEST(last_submit_time,VALUES(last_submit_time)),VALUES(last_submit_time)),accepted_submission_id=IF(solve_source='NOWCODER' OR (VALUES(first_ac_time) IS NOT NULL AND (first_ac_time IS NULL OR VALUES(first_ac_time)<first_ac_time)),VALUES(accepted_submission_id),accepted_submission_id),solve_source=IF(VALUES(first_ac_time) IS NOT NULL AND (solve_source='NOWCODER' OR first_ac_time IS NULL OR VALUES(first_ac_time)<first_ac_time),'NOWCODER',solve_source),first_ac_time=IF(solve_source='NOWCODER',VALUES(first_ac_time),COALESCE(LEAST(first_ac_time,VALUES(first_ac_time)),first_ac_time,VALUES(first_ac_time)))",user==null ? new Object[]{id,key} : new Object[]{id,key,user});
    }
    public void processImport() {
        var job=claim("nowcoder_problem_import_job");if(job==null)return;
        try {
            String key=(String)job.get("external_problem_key");if(!key.matches("NC[1-9][0-9]*"))throw new BusinessException(409,"牛客题号无效");
            var owners=jdbc.queryForList("SELECT f.user_id FROM oj_first_ac f JOIN oj_account a ON a.user_id=f.user_id AND a.platform=f.platform AND a.verify_status=1 JOIN app_user u ON u.id=f.user_id AND u.status=1 WHERE f.platform='NOWCODER' AND f.external_problem_key=? ORDER BY f.user_id LIMIT 1",Long.class,key);
            if(owners.isEmpty())throw new BusinessException(409,"没有有效的牛客绑定，任务已取消");
            Long existing=findProblem(key);
            var imported=existing==null?importer.preview("https://ac.nowcoder.com/acm/problem/"+key.substring(2)):null;
            if(imported!=null&&!key.equals(imported.externalProblemKey()))throw new BusinessException(422,"牛客题号与题面不一致，已停止导入");
            tx.executeWithoutResult(status->{
                if(!lockJob("nowcoder_problem_import_job",job))return;
                if(imported!=null)jdbc.update("INSERT IGNORE INTO problem(platform,external_problem_key,title,source_url,difficulty,tags,content_md,creator_user_id,status) VALUES('NOWCODER',?,?,?,?,?,?,NULL,1)",key,imported.title(),imported.sourceUrl(),imported.difficulty(),imported.tags(),imported.contentMd());
                Long id=findProblem(key);if(id==null)throw new IllegalStateException("Problem insert was not persisted");
                linkProblem(key,id,null);
                jdbc.update("UPDATE nowcoder_problem_import_job SET status='COMPLETED',problem_id=?,error_message=NULL,lease_token=NULL,lease_until=NULL WHERE id=? AND lease_token=?",id,job.get("id"),job.get("lease_token"));
            });
        }catch(Exception ex){fail("nowcoder_problem_import_job",job,ex);}
    }
    public void retryImports(long user) {
        account(user);
        jdbc.update("UPDATE nowcoder_problem_import_job q JOIN oj_first_ac f ON f.external_problem_key=q.external_problem_key AND f.platform='NOWCODER' SET q.status='QUEUED',q.attempts=0,q.error_message=NULL,q.next_run_time=NOW() WHERE f.user_id=? AND q.status='FAILED'",user);
    }
}
