package com.itnoduck.acmate.oj.service;
import com.itnoduck.acmate.oj.client.NowcoderPublicClient;
import com.itnoduck.acmate.problem.entity.Problem;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import com.itnoduck.acmate.problem.service.NowcoderProblemImportService;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.user.service.UserActivityService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import tools.jackson.databind.ObjectMapper;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
@EnabledIfSystemProperty(named="account.database-check",matches="true")
class NowcoderTrainingDatabaseTest {
 SingleConnectionDataSource source;JdbcTemplate jdbc;String schema,original;
 NowcoderPublicClient client;NowcoderTrainingService service;ProblemMapper problems;
 @BeforeEach void setup() throws Exception{
  var env=new StandardEnvironment();
  for(String file:List.of("application.yml","application-local.yml")){var yaml=new YamlPropertiesFactoryBean();yaml.setResources(new FileSystemResource("src/main/resources/"+file));env.getPropertySources().addFirst(new PropertiesPropertySource(file,yaml.getObject()));}
  var connection=DriverManager.getConnection(env.getRequiredProperty("spring.datasource.url"),env.getRequiredProperty("spring.datasource.username"),env.getRequiredProperty("spring.datasource.password"));
  source=new SingleConnectionDataSource(connection,true);jdbc=new JdbcTemplate(source);original=connection.getCatalog();schema="acmate_training_test_"+UUID.randomUUID().toString().replace("-","").substring(0,12);jdbc.execute("CREATE DATABASE "+schema+" CHARACTER SET utf8mb4");connection.setCatalog(schema);
  try(var files=java.nio.file.Files.list(java.nio.file.Path.of("src/main/resources/db/migration"))){for(var file:files.filter(p->p.toString().endsWith(".sql")).sorted(Comparator.comparingInt(p->Integer.parseInt(p.getFileName().toString().split("__")[0].substring(1)))).toList()){String sql=java.nio.file.Files.readString(file).replace("\ufeff","");for(String statement:sql.split(";"))if(!statement.isBlank())jdbc.execute(statement);}}
  jdbc.update("INSERT INTO app_user(id,username,password_hash,nickname) VALUES(1,'member','hash','Member'),(2,'other','hash','Other')");
  jdbc.update("INSERT INTO oj_account(id,user_id,platform,external_user_id,verify_status) VALUES(1,1,'NOWCODER','123',1),(2,1,'CODEFORCES','testcf',1)");
  jdbc.update("INSERT INTO nowcoder_account_owner(uid,user_id) VALUES('123',1)");
  jdbc.update("INSERT INTO problem(id,platform,external_problem_key,title,source_url,creator_user_id) VALUES(1,'NOWCODER','NC212955','Test','https://ac.nowcoder.com/acm/contest/19304/A',1)");
  problems=mock(ProblemMapper.class);var p=new Problem();p.setId(1L);p.setStatus(1);p.setPlatform("NOWCODER");p.setExternalProblemKey("NC212955");p.setSourceUrl("https://ac.nowcoder.com/acm/contest/19304/A");when(problems.selectById(1L)).thenReturn(p);
  client=mock(NowcoderPublicClient.class);service=new NowcoderTrainingService(jdbc,problems,new NowcoderProblemImportService(new ObjectMapper()),client,new DataSourceTransactionManager(source));
 }
 @AfterEach void cleanup()throws Exception{if(source==null)return;try{if(schema!=null&&schema.matches("acmate_training_test_[a-f0-9]{12}")){source.getConnection().setCatalog(original);jdbc.execute("DROP DATABASE "+schema);}}finally{source.destroy();}}
 long count(String sql){return jdbc.queryForObject(sql,Long.class);}
 @Test void officialResultsAreDeduplicatedAndUpdatePersonalActivity(){
  var now=LocalDateTime.now().minusMinutes(2);
  when(client.submissions(19304,123,212955)).thenReturn(List.of(new NowcoderPublicClient.OfficialSubmission(100,123,212955,"WRONG_ANSWER","C++",now),new NowcoderPublicClient.OfficialSubmission(101,123,212955,"OK","C++",now.plusMinutes(1))));
  assertEquals(2,service.sync(1,1).inserted());
  jdbc.update("UPDATE oj_account SET last_sync_time=NULL WHERE id=1");assertEquals(0,service.sync(1,1).inserted());
  assertEquals(2,count("SELECT COUNT(*) FROM oj_submission"));assertEquals(1,count("SELECT COUNT(*) FROM oj_first_ac"));assertEquals(2,count("SELECT attempt_count FROM user_problem_status"));assertEquals(2,count("SELECT status FROM user_problem_status"));
  assertEquals(1,count("SELECT COUNT(*) FROM oj_submission WHERE is_first_ac=1"));
  var activity=new UserActivityService(jdbc);assertEquals(2,activity.activity(1).submissions());assertEquals(2,activity.recent(1).size());
  assertEquals(2,count("SELECT COUNT(*) FROM oj_account WHERE user_id=1"));
  var config=new org.apache.ibatis.session.Configuration();
  config.setEnvironment(new org.apache.ibatis.mapping.Environment("test",new org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory(),source));
  config.addMapper(com.itnoduck.acmate.oj.mapper.OjSubmissionMapper.class);
  try(var session=new org.apache.ibatis.session.SqlSessionFactoryBuilder().build(config).openSession()){
   var mapper=session.getMapper(com.itnoduck.acmate.oj.mapper.OjSubmissionMapper.class);
   var stats=mapper.getUserOjStats(1L,now.minusDays(30),now.minusDays(7));
   assertEquals(1,((Number)stats.get("solved_count")).longValue());
   assertEquals(1,mapper.countLeaderboardUsersAll());
  }
 }
 @Test void earliestAcceptedSubmissionRemainsPairedAcrossOutOfOrderSync(){
  var old=LocalDateTime.of(2025,1,1,12,0); var newer=old.plusDays(1);
  when(client.submissions(19304,123,212955)).thenReturn(List.of(new NowcoderPublicClient.OfficialSubmission(101,123,212955,"OK","C++",newer),new NowcoderPublicClient.OfficialSubmission(100,123,212955,"OK","C++",old)));
  service.sync(1,1);
  assertEquals(count("SELECT id FROM oj_submission WHERE remote_submission_id='100'"),count("SELECT accepted_submission_id FROM user_problem_status WHERE user_id=1 AND problem_id=1"));
  assertEquals(jdbc.queryForObject("SELECT submitted_time FROM oj_submission WHERE remote_submission_id='100'",java.sql.Timestamp.class),jdbc.queryForObject("SELECT first_ac_time FROM user_problem_status WHERE user_id=1 AND problem_id=1",java.sql.Timestamp.class));
 }
 @Test void unbindingDoesNotAllowAnotherAccountToClaimUid(){
  service.unbind(1);
  var tx=new org.springframework.transaction.support.TransactionTemplate(new DataSourceTransactionManager(source));
  assertEquals(409,assertThrows(BusinessException.class,()->tx.executeWithoutResult(status->service.bind(2,"123"))).getCode());
  assertEquals(0,count("SELECT COUNT(*) FROM oj_account WHERE user_id=2 AND platform='NOWCODER'"));
  tx.executeWithoutResult(status->service.bind(1,"123"));assertTrue(service.account(1).hasAccount());
 }
 @Test void pendingUidDoesNotReserveOwnershipUntilVerified(){
  var tx=new org.springframework.transaction.support.TransactionTemplate(new DataSourceTransactionManager(source));
  service.unbind(1);
  tx.executeWithoutResult(status->service.bind(2,"456"));
  assertEquals(0,count("SELECT COUNT(*) FROM nowcoder_account_owner WHERE uid='456'"));
  service.unbind(2);
  tx.executeWithoutResult(status->service.bind(1,"456"));
  assertEquals(0,count("SELECT COUNT(*) FROM nowcoder_account_owner WHERE uid='456'"));
 }
 @Test void historyIsSeparatedByOriginalNowcoderUid(){
  var now=LocalDateTime.now().withNano(0);
  jdbc.update("INSERT INTO oj_submission(oj_account_id,user_id,platform,remote_submission_id,external_problem_key,verdict,submitted_time,nowcoder_uid) VALUES(1,1,'NOWCODER','a','NC1','OK',?,'123'),(1,1,'NOWCODER','b','NC1','OK',?,'456'),(1,1,'NOWCODER','c','NC2','WRONG_ANSWER',?,NULL),(1,1,'NOWCODER','d','NC3','OK',?,'456')",now,now,now,now);
  var history=new com.itnoduck.acmate.user.service.NowcoderHistoryService(jdbc);
  assertEquals(3,history.accounts(1).size());assertEquals(1,history.overview(1,"123").stats().solvedCount());assertEquals(2,history.overview(1,"456").stats().solvedCount());
  assertEquals(1,history.activity(1,"UNKNOWN").submissions());assertEquals(2,history.recent(1,"456").size());
  var visible=new com.itnoduck.acmate.user.service.PublicUserActivityService(jdbc);
  assertEquals(1,visible.activity(1,"NOWCODER").submissions());assertEquals(1,visible.recent(1,"NOWCODER").size());assertEquals(1,visible.overview(1,"NOWCODER").stats().solvedCount());
  var controller=new com.itnoduck.acmate.user.controller.NowcoderHistoryController(history);
  var owner=new com.itnoduck.acmate.security.AuthenticatedUser(1L,"member","hash","Member",null,null,null,false,true,List.of());
  assertEquals(3,controller.accounts(1,owner).size());assertEquals(1,controller.accounts(1,null).size());
  assertEquals(404,assertThrows(BusinessException.class,()->controller.recent(1,"456",null)).getCode());
  assertEquals(404,assertThrows(BusinessException.class,()->controller.recent(1,"UNKNOWN",null)).getCode());
  assertEquals(1,controller.recent(1,"123",null).size());
  var config=new org.apache.ibatis.session.Configuration();
  config.setEnvironment(new org.apache.ibatis.mapping.Environment("test",new org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory(),source));config.addMapper(com.itnoduck.acmate.oj.mapper.OjSubmissionMapper.class);
  try(var session=new org.apache.ibatis.session.SqlSessionFactoryBuilder().build(config).openSession()){
   var stats=session.getMapper(com.itnoduck.acmate.oj.mapper.OjSubmissionMapper.class).getUserOjStats(1L,now.minusDays(30),now.minusDays(7));
   assertEquals(1,((Number)stats.get("solved_count")).longValue());
  }
 }
 @Test void unverifiedAccountsCannotImportOfficialResults(){
  jdbc.update("UPDATE oj_account SET verify_status=0 WHERE id=1");assertEquals(409,assertThrows(BusinessException.class,()->service.sync(1,1)).getCode());verifyNoInteractions(client);assertEquals(0,count("SELECT COUNT(*) FROM oj_submission"));
 }
 @Test void synchronisationCooldownAndOwnershipConflictsNeverDuplicateOrStealRecords(){
  when(client.submissions(19304,123,212955)).thenReturn(List.of(new NowcoderPublicClient.OfficialSubmission(100,123,212955,"OK","C++",LocalDateTime.now())));
  service.sync(1,1);assertEquals(429,assertThrows(BusinessException.class,()->service.sync(1,1)).getCode());
  jdbc.update("UPDATE oj_submission SET user_id=2");jdbc.update("UPDATE oj_account SET last_sync_time=NULL WHERE id=1");
  assertEquals(409,assertThrows(BusinessException.class,()->service.sync(1,1)).getCode());assertEquals(2,count("SELECT user_id FROM oj_submission WHERE remote_submission_id='100'"));
 }
 @Test void standaloneAndPracticeLinksHaveHonestUnsupportedSyncState(){
  var p=problems.selectById(1L);p.setSourceUrl("https://ac.nowcoder.com/acm/problem/212955");
  assertFalse(service.training(1).syncSupported());assertEquals(422,assertThrows(BusinessException.class,()->service.sync(1,1)).getCode());verifyNoInteractions(client);
 }
 @Test void nowcoderUnbindingLeavesCodeforcesUntouched(){service.unbind(1);assertEquals(1,count("SELECT COUNT(*) FROM oj_account WHERE user_id=1 AND platform='CODEFORCES'"));assertFalse(service.account(1).hasAccount());}
 @Test void fullProfileQueueDeduplicatesHistoryAndOnlyImportsMissingAcceptedProblems(){
  var profileClient=mock(com.itnoduck.acmate.oj.client.NowcoderProfileClient.class);
  var importer=mock(NowcoderProblemImportService.class);
  var queue=new NowcoderProfileSyncService(jdbc,profileClient,importer,new DataSourceTransactionManager(source));
  var time=LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")).minusMinutes(5);
  var first=new com.itnoduck.acmate.oj.client.NowcoderProfileClient.Submission(100,212955,"已有题","OK","C++",time);
  var missing=new com.itnoduck.acmate.oj.client.NowcoderProfileClient.Submission(101,321947,"新题","OK","C++",time.plusMinutes(1));
  var failed=new com.itnoduck.acmate.oj.client.NowcoderProfileClient.Submission(102,321948,"未通过题","WRONG_ANSWER","C++",time.plusMinutes(2));
  when(profileClient.page(123,1)).thenReturn(new com.itnoduck.acmate.oj.client.NowcoderProfileClient.Page(1,2,List.of(first,missing,failed)));
  when(profileClient.page(123,2)).thenReturn(new com.itnoduck.acmate.oj.client.NowcoderProfileClient.Page(2,2,List.of(missing)));
  assertEquals("QUEUED",queue.enqueue(1).status());queue.enqueue(1);assertEquals(1,count("SELECT COUNT(*) FROM nowcoder_sync_job"));
  queue.processSync();assertEquals(3,count("SELECT COUNT(*) FROM oj_submission"));assertEquals(1,count("SELECT COUNT(*) FROM nowcoder_problem_import_job"));
  assertEquals("NC321947",jdbc.queryForObject("SELECT external_problem_key FROM nowcoder_problem_import_job",String.class));
  jdbc.update("UPDATE nowcoder_sync_job SET next_run_time=NOW()");queue.processSync();assertEquals("COMPLETED",queue.state(1).status());assertEquals(3,queue.state(1).inserted());
  when(importer.preview("https://ac.nowcoder.com/acm/problem/321947")).thenReturn(new com.itnoduck.acmate.problem.dto.ImportedProblemResponse("NOWCODER","NC321947","新题","https://ac.nowcoder.com/acm/problem/321947","","","公开题面",List.of()));
  queue.processImport();queue.processImport();verify(importer,times(1)).preview("https://ac.nowcoder.com/acm/problem/321947");
  assertEquals(2,count("SELECT COUNT(*) FROM problem"));assertEquals(1,count("SELECT COUNT(*) FROM problem WHERE external_problem_key='NC321947'"));
  assertEquals(3,count("SELECT COUNT(*) FROM oj_submission"));assertEquals(2,count("SELECT COUNT(*) FROM oj_first_ac"));assertEquals(2,count("SELECT COUNT(*) FROM user_problem_status WHERE status=2"));
  var metrics=new UserActivityService(jdbc);assertEquals(3,metrics.activity(1,"NOWCODER").submissions());assertEquals(0,metrics.activity(1,"CODEFORCES").submissions());
  assertEquals(2,metrics.overview(1,"NOWCODER").stats().solvedCount());assertEquals(3,metrics.overview(1,"NOWCODER").todayAttempted());assertEquals(2,metrics.overview(1,"NOWCODER").todaySolved());
  assertEquals(2,metrics.overview(1,"ALL").accounts().size());assertThrows(BusinessException.class,()->metrics.activity(1,"x' OR 1=1"));
  jdbc.update("UPDATE nowcoder_sync_job SET request_time=DATE_SUB(NOW(),INTERVAL 61 SECOND)");queue.enqueue(1);queue.processSync();
  assertEquals(3,count("SELECT COUNT(*) FROM oj_submission"));assertEquals(1,count("SELECT COUNT(*) FROM nowcoder_problem_import_job"));
 }
 @Test void importChecksDatabaseAgainBeforeMakingAnyRemoteRequest(){
  var client=mock(com.itnoduck.acmate.oj.client.NowcoderProfileClient.class);var importer=mock(NowcoderProblemImportService.class);
  var queue=new NowcoderProfileSyncService(jdbc,client,importer,new DataSourceTransactionManager(source));
  jdbc.update("INSERT INTO oj_submission(oj_account_id,user_id,platform,remote_submission_id,external_problem_key,verdict,submitted_time) VALUES(1,1,'NOWCODER','55','NC212955','OK',NOW())");
  long sid=count("SELECT id FROM oj_submission WHERE remote_submission_id='55'");
  jdbc.update("INSERT INTO oj_first_ac(user_id,platform,external_problem_key,submission_id,create_time) VALUES(1,'NOWCODER','NC212955',?,NOW())",sid);
  jdbc.update("INSERT INTO nowcoder_problem_import_job(external_problem_key) VALUES('NC212955')");
  queue.processImport();verifyNoInteractions(importer);assertEquals(1,count("SELECT COUNT(*) FROM problem"));assertEquals("COMPLETED",jdbc.queryForObject("SELECT status FROM nowcoder_problem_import_job",String.class));
  assertEquals(1,count("SELECT problem_id FROM oj_submission WHERE id="+sid));
 }
 @Test void queueRetriesFailuresAndResumesExpiredLeasesButCancelsChangedBindings(){
  var client=mock(com.itnoduck.acmate.oj.client.NowcoderProfileClient.class);var importer=mock(NowcoderProblemImportService.class);
  var queue=new NowcoderProfileSyncService(jdbc,client,importer,new DataSourceTransactionManager(source));
  when(client.page(123,1)).thenThrow(new BusinessException(502,"上游暂不可用"));queue.enqueue(1);queue.processSync();
  assertEquals("QUEUED",queue.state(1).status());assertEquals(1,count("SELECT attempts FROM nowcoder_sync_job"));assertEquals(0,count("SELECT COUNT(*) FROM oj_submission"));
  jdbc.update("UPDATE nowcoder_sync_job SET status='RUNNING',lease_until=DATE_SUB(NOW(),INTERVAL 1 SECOND)");
  doReturn(new com.itnoduck.acmate.oj.client.NowcoderProfileClient.Page(1,1,List.of())).when(client).page(123,1);queue.processSync();assertEquals("COMPLETED",queue.state(1).status());
  jdbc.update("UPDATE nowcoder_sync_job SET request_time=DATE_SUB(NOW(),INTERVAL 61 SECOND)");queue.enqueue(1);jdbc.update("UPDATE oj_account SET verify_status=0 WHERE id=1");queue.processSync();
  assertEquals("CANCELLED",queue.state(1).status());verify(client,times(2)).page(123,1);
 }
 @Test void historicalFirstAcIsAttributedToOfficialDayAndRejudgingDoesNotKeepFalseSolvedCount(){
  var client=mock(com.itnoduck.acmate.oj.client.NowcoderProfileClient.class);var importer=mock(NowcoderProblemImportService.class);
  var queue=new NowcoderProfileSyncService(jdbc,client,importer,new DataSourceTransactionManager(source));
  var today=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai"));
  when(client.page(123,1)).thenReturn(new com.itnoduck.acmate.oj.client.NowcoderProfileClient.Page(1,1,List.of(new com.itnoduck.acmate.oj.client.NowcoderProfileClient.Submission(77,212955,"题目","OK","C++",today.minusDays(2).atTime(23,0)))));
  queue.enqueue(1);queue.processSync();var metrics=new UserActivityService(jdbc);
  assertEquals(1,metrics.overview(1,"NOWCODER").stats().solvedCount());assertEquals(0,metrics.overview(1,"NOWCODER").todaySubmissions());assertEquals(0,metrics.overview(1,"NOWCODER").todaySolved());
  jdbc.update("UPDATE nowcoder_sync_job SET request_time=DATE_SUB(NOW(),INTERVAL 61 SECOND)");
  when(client.page(123,1)).thenReturn(new com.itnoduck.acmate.oj.client.NowcoderProfileClient.Page(1,1,List.of(new com.itnoduck.acmate.oj.client.NowcoderProfileClient.Submission(77,212955,"题目","WRONG_ANSWER","C++",today.minusDays(2).atTime(23,0)))));
  queue.enqueue(1);queue.processSync();assertEquals(0,metrics.overview(1,"NOWCODER").stats().solvedCount());assertEquals(1,count("SELECT COUNT(*) FROM oj_submission"));
 }
 @Test void repeatedlyFailingImportsStayFailedUntilOwnerRequestsRetry(){
  var client=mock(com.itnoduck.acmate.oj.client.NowcoderProfileClient.class);var importer=mock(NowcoderProblemImportService.class);
  var queue=new NowcoderProfileSyncService(jdbc,client,importer,new DataSourceTransactionManager(source));
  when(client.page(123,1)).thenReturn(new com.itnoduck.acmate.oj.client.NowcoderProfileClient.Page(1,1,List.of(new com.itnoduck.acmate.oj.client.NowcoderProfileClient.Submission(77,321947,"新题","OK","C++",LocalDateTime.now()))));
  when(importer.preview(anyString())).thenThrow(new BusinessException(502,"公开题面暂不可用"));queue.enqueue(1);queue.processSync();
  for(int attempt=0;attempt<5;attempt++){jdbc.update("UPDATE nowcoder_problem_import_job SET next_run_time=NOW()");queue.processImport();}
  assertEquals(1,queue.state(1).importFailed());assertEquals(0,queue.state(1).importPending());
  assertEquals(1,count("SELECT COUNT(*) FROM oj_submission"));assertEquals(1,count("SELECT COUNT(*) FROM problem"));
  queue.retryImports(1);assertEquals(1,queue.state(1).importPending());assertEquals(0,queue.state(1).importFailed());
 }
}
