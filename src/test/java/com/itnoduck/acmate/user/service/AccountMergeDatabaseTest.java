package com.itnoduck.acmate.user.service;

import com.itnoduck.acmate.common.exception.BusinessException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.transaction.support.TransactionTemplate;
import java.sql.DriverManager;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Runs in a disposable, uniquely named schema; never writes application tables. */
@EnabledIfSystemProperty(named="account.database-check",matches="true")
class AccountMergeDatabaseTest {
    SingleConnectionDataSource source;
    JdbcTemplate jdbc;
    AccountMergeService merger;
    TransactionTemplate transaction;
    String testSchema;
    String applicationSchema;
    @BeforeEach void setup() throws Exception {
        var environment=new StandardEnvironment();
        for(String file:List.of("application.yml","application-local.yml")) {
            var yaml=new YamlPropertiesFactoryBean(); yaml.setResources(new FileSystemResource("src/main/resources/"+file));
            environment.getPropertySources().addFirst(new PropertiesPropertySource(file,yaml.getObject()));
        }
        var connection=DriverManager.getConnection(environment.getRequiredProperty("spring.datasource.url"),environment.getRequiredProperty("spring.datasource.username"),environment.getRequiredProperty("spring.datasource.password"));
        source=new SingleConnectionDataSource(connection,true); jdbc=new JdbcTemplate(source);
        applicationSchema=connection.getCatalog();
        if(!applicationSchema.matches("[A-Za-z0-9_]+")) throw new IllegalStateException("Unexpected schema identifier");
        String candidate="acmate_binding_test_"+java.util.UUID.randomUUID().toString().replace("-", "").substring(0,12);
        jdbc.execute("CREATE DATABASE "+candidate+" CHARACTER SET utf8mb4");
        testSchema=candidate;
        connection.setCatalog(testSchema);
        var migrations=java.nio.file.Files.list(java.nio.file.Path.of("src/main/resources/db/migration"));
        try(migrations) {
            for(var file:migrations.filter(p->p.toString().endsWith(".sql")).sorted(java.util.Comparator.comparingInt(p->Integer.parseInt(p.getFileName().toString().split("__")[0].substring(1)))).toList()) {
                if(file.getFileName().toString().startsWith("V21__")) {
                    jdbc.update("INSERT INTO app_user(id,username,password_hash,nickname,email) VALUES(1,'github_user','hash','Github User',NULL),(2,'email_user','hash','Email User','mail@example.com')");
                    jdbc.update("INSERT INTO user_github(github_id,user_id,github_login,github_name) VALUES(123,1,'github-user','Github User')");
                }
                String sql=java.nio.file.Files.readString(file).replace("\ufeff", "");
                for(String statement:sql.split(";")) if(!statement.isBlank()) jdbc.execute(statement);
            }
        }
        var redis=mock(StringRedisTemplate.class);
        when(redis.scan(any())).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("test-cache-disabled"));
        merger=new AccountMergeService(jdbc,mock(SessionRegistry.class),redis);
        transaction=new TransactionTemplate(new DataSourceTransactionManager(source));

        jdbc.update("INSERT INTO problem(id,platform,title,creator_user_id) VALUES(1,'CUSTOM','Test Problem',1)");
        jdbc.update("INSERT INTO training_plan(id,title,creator_user_id) VALUES(1,'Test Plan',1)");
        jdbc.update("INSERT INTO training_plan_member(plan_id,user_id,status) VALUES(1,1,1),(1,2,0)");
        jdbc.update("INSERT INTO post(id,author_user_id,post_type,title,content_md,like_count) VALUES(1,1,'DISCUSSION','Test Post','test',2)");
        jdbc.update("INSERT INTO post_like(post_id,user_id) VALUES(1,1),(1,2)");
        jdbc.update("INSERT INTO user_problem_status(user_id,problem_id,status,attempt_count,performance_note) VALUES(1,1,2,2,'source note'),(2,1,1,3,'target note')");
        jdbc.update("INSERT INTO oj_submission(id,oj_account_id,user_id,platform,remote_submission_id,external_problem_key,verdict,submitted_time) VALUES(1,10,1,'CODEFORCES','1','1A','OK','2026-01-01'),(2,11,2,'CODEFORCES','2','1A','OK','2026-02-01')");
        jdbc.update("INSERT INTO oj_first_ac(user_id,platform,external_problem_key,submission_id) VALUES(1,'CODEFORCES','1A',1),(2,'CODEFORCES','1A',2)");
    }
    @AfterEach void cleanup() throws Exception {
        if(source==null) return;
        try {
            if(testSchema!=null && testSchema.matches("acmate_binding_test_[a-f0-9]{12}")) {
                source.getConnection().setCatalog(applicationSchema);
                jdbc.execute("DROP DATABASE "+testSchema);
            }
        } finally { source.destroy(); }
    }
    long value(String sql) { return jdbc.queryForObject(sql,Long.class); }
    @Test void problemWritesInvalidateCachedVersionOnlyOnCommit() {
        long before=value("SELECT revision FROM problem_cache_revision WHERE id=1");
        transaction.executeWithoutResult(status->jdbc.update("UPDATE problem SET title='changed' WHERE id=1"));
        assertEquals(before+1,value("SELECT revision FROM problem_cache_revision WHERE id=1"));
        transaction.executeWithoutResult(status->{jdbc.update("UPDATE problem SET title='rollback' WHERE id=1");status.setRollbackOnly();});
        assertEquals(before+1,value("SELECT revision FROM problem_cache_revision WHERE id=1"));
    }
    @Test void publicProblemSuggestionRequiresAdminApprovalAndPreservesHistory() {
        jdbc.update("UPDATE problem SET platform='NOWCODER',external_problem_key='NC42' WHERE id=1");
        var problem=new com.itnoduck.acmate.problem.entity.Problem();problem.setId(1L);problem.setPlatform("NOWCODER");problem.setExternalProblemKey("NC42");problem.setTitle("Test Problem");problem.setUpdateTime(jdbc.queryForObject("SELECT update_time FROM problem WHERE id=1",java.sql.Timestamp.class).toLocalDateTime());
        var mapper=mock(com.itnoduck.acmate.problem.mapper.ProblemMapper.class);when(mapper.selectById(1L)).thenReturn(problem);
        var command=mock(com.itnoduck.acmate.problem.service.ProblemCommandService.class);
        when(command.updateProblem(eq(1L),any(),eq(2L),eq(true))).thenAnswer(inv->{jdbc.update("UPDATE problem SET title=? WHERE id=1",((com.itnoduck.acmate.problem.dto.UpdateProblemRequest)inv.getArgument(1)).getTitle());return null;});
        var controller=new com.itnoduck.acmate.problem.controller.ProblemReviewController(jdbc,mapper,command,new tools.jackson.databind.ObjectMapper());
        var member=new com.itnoduck.acmate.security.AuthenticatedUser(1L,"member","hash","Member",null,null,null,false,true,List.of());
        var admin=new com.itnoduck.acmate.security.AuthenticatedUser(2L,"admin","hash","Admin",null,null,null,true,true,List.of());
        var request=new com.itnoduck.acmate.problem.dto.UpdateProblemRequest();request.setPlatform("NOWCODER");request.setExternalProblemKey("NC42");request.setTitle("New Title");
        transaction.executeWithoutResult(status->controller.propose(1,request,member));
        assertEquals("Test Problem",jdbc.queryForObject("SELECT title FROM problem WHERE id=1",String.class));
        assertThrows(BusinessException.class,()->controller.approve(1,new com.itnoduck.acmate.problem.controller.ProblemReviewController.Decision(null),member));
        transaction.executeWithoutResult(status->controller.approve(1,new com.itnoduck.acmate.problem.controller.ProblemReviewController.Decision("OK"),admin));
        assertEquals("New Title",jdbc.queryForObject("SELECT title FROM problem WHERE id=1",String.class));
        assertEquals("APPROVED",jdbc.queryForObject("SELECT status FROM problem_change_request WHERE id=1",String.class));
        assertEquals(2,value("SELECT reviewer_user_id FROM problem_change_request WHERE id=1"));
        assertTrue(jdbc.queryForObject("SELECT original_content FROM problem_change_request WHERE id=1",String.class).contains("Test Problem"));
    }
    @Test void migrationBackfillsBothProvidersAndKeepsLegacyData() {
        assertEquals(1,value("SELECT user_id FROM user_login_identity WHERE provider='GITHUB' AND identifier='123'"));
        assertEquals(2,value("SELECT user_id FROM user_login_identity WHERE provider='EMAIL' AND identifier='mail@example.com'"));
        assertEquals(1,value("SELECT COUNT(*) FROM user_github_legacy"));
        assertEquals("github-user",jdbc.queryForObject("SELECT login_name FROM user_login_identity WHERE provider='GITHUB'",String.class));
    }
    @Test void identitiesAreUniqueAcrossAccountsAndPerProvider() {
        assertThrows(org.springframework.dao.DuplicateKeyException.class,()->jdbc.update("INSERT INTO user_login_identity(user_id,provider,identifier) VALUES(2,'GITHUB','123')"));
        assertThrows(org.springframework.dao.DuplicateKeyException.class,()->jdbc.update("INSERT INTO user_login_identity(user_id,provider,identifier) VALUES(1,'GITHUB','456')"));
        assertEquals(2,value("SELECT COUNT(*) FROM user_login_identity"));
    }
    @Test void changingMailboxReleasesOldAddressAndUnlinkingGithubKeepsEmail() {
        transaction.executeWithoutResult(status->merger.merge(1,2));
        jdbc.update("UPDATE user_login_identity SET identifier='new@example.com' WHERE provider='EMAIL' AND user_id=2");
        assertEquals(0,value("SELECT COUNT(*) FROM user_login_identity WHERE provider='EMAIL' AND identifier='mail@example.com'"));
        jdbc.update("DELETE FROM user_login_identity WHERE provider='GITHUB' AND user_id=2");
        assertEquals(1,value("SELECT COUNT(*) FROM user_login_identity WHERE user_id=2 AND provider='EMAIL'"));
    }
    @Test void mergesDataAndDeduplicatesSharedProgressAndLikes() {
        transaction.executeWithoutResult(status->merger.merge(1,2));
        assertEquals(2,value("SELECT user_id FROM user_github WHERE github_id=123"));
        assertEquals(2,value("SELECT COUNT(*) FROM user_login_identity WHERE user_id=2"));
        assertEquals(0,value("SELECT COUNT(*) FROM user_login_identity WHERE user_id=1"));
        assertEquals(2,value("SELECT creator_user_id FROM problem WHERE id=1"));
        assertEquals(2,value("SELECT author_user_id FROM post WHERE id=1"));
        assertEquals(1,value("SELECT COUNT(*) FROM post_like")); assertEquals(1,value("SELECT like_count FROM post WHERE id=1"));
        assertEquals(1,value("SELECT COUNT(*) FROM user_problem_status"));assertEquals(2,value("SELECT status FROM user_problem_status"));assertEquals(5,value("SELECT attempt_count FROM user_problem_status"));
        assertTrue(jdbc.queryForObject("SELECT performance_note FROM user_problem_status",String.class).contains("source note"));
        assertEquals(1,value("SELECT status FROM training_plan_member"));
        assertEquals(1,value("SELECT COUNT(*) FROM oj_first_ac"));assertEquals(1,value("SELECT submission_id FROM oj_first_ac"));
        assertEquals(2,value("SELECT COUNT(*) FROM oj_submission WHERE user_id=2"));assertEquals(1,value("SELECT COUNT(*) FROM oj_submission WHERE is_first_ac=1"));
        assertEquals(0,value("SELECT status FROM app_user WHERE id=1"));assertEquals(2,value("SELECT merged_into_user_id FROM app_user WHERE id=1"));
        assertEquals(1,value("SELECT COUNT(*) FROM account_merge_log"));
    }
    @Test void conflictingPlatformsLeaveEveryRecordUntouched() {
        jdbc.update("INSERT INTO oj_account(user_id,platform,external_user_id) VALUES(1,'CODEFORCES','handle1'),(2,'CODEFORCES','handle2')");
        assertThrows(BusinessException.class,()->transaction.executeWithoutResult(status->merger.merge(1,2)));
        assertEquals(1,value("SELECT user_id FROM user_github WHERE github_id=123"));assertEquals(1,value("SELECT creator_user_id FROM problem WHERE id=1"));
        assertEquals(2,value("SELECT COUNT(*) FROM post_like"));assertEquals(1,value("SELECT status FROM app_user WHERE id=1"));assertEquals(0,value("SELECT COUNT(*) FROM account_merge_log"));
    }
    @Test void lateDatabaseFailureRollsBackAllEarlierTransfers() {
        // A duplicate merge audit row forces a failure after all data updates.
        jdbc.update("INSERT INTO account_merge_log(source_user_id,target_user_id) VALUES(1,2)");
        assertThrows(org.springframework.dao.DuplicateKeyException.class,()->transaction.executeWithoutResult(status->merger.merge(1,2)));
        assertEquals(1,value("SELECT creator_user_id FROM problem WHERE id=1"));assertEquals(1,value("SELECT user_id FROM user_github WHERE github_id=123"));
        assertEquals(2,value("SELECT COUNT(*) FROM user_problem_status"));assertEquals(2,value("SELECT COUNT(*) FROM post_like"));assertEquals(1,value("SELECT status FROM app_user WHERE id=1"));
    }
    @Test void mergingDeduplicatesFavoritesAndFollowEdgesWithoutSelfFollowing() {
        jdbc.update("INSERT INTO app_user(id,username,password_hash,nickname) VALUES(3,'friend','hash','Friend')");
        jdbc.update("INSERT INTO post_favorite(user_id,post_id) VALUES(1,1),(2,1)");
        jdbc.update("INSERT INTO user_follow(follower_id,followed_id) VALUES(1,3),(2,3),(3,1),(3,2),(1,2),(2,1)");
        jdbc.update("UPDATE app_user SET avatar_url='/uploads/photo.png',location='河北',gender='MALE' WHERE id=1");
        transaction.executeWithoutResult(status->merger.merge(1,2));
        assertEquals(1,value("SELECT COUNT(*) FROM post_favorite"));assertEquals(2,value("SELECT user_id FROM post_favorite"));
        assertEquals(2,value("SELECT COUNT(*) FROM user_follow"));assertEquals(0,value("SELECT COUNT(*) FROM user_follow WHERE follower_id=followed_id OR follower_id=1 OR followed_id=1"));
        assertEquals("/uploads/photo.png",jdbc.queryForObject("SELECT avatar_url FROM app_user WHERE id=2",String.class));
    }
    @Test void followsAndFavoritesAreIdempotentAndContributionsUseRealDailyRecords() {
        var social=new UserSocialService(jdbc);var favorites=new com.itnoduck.acmate.discussion.service.PostFavoriteService(jdbc);
        transaction.executeWithoutResult(status->{social.follow(1,2,true);social.follow(1,2,true);favorites.set(1,2,true);favorites.set(1,2,true);});
        assertEquals(1,value("SELECT COUNT(*) FROM user_follow"));assertEquals(1,value("SELECT COUNT(*) FROM post_favorite"));
        assertThrows(BusinessException.class,()->social.follow(1,1,true));
        var yesterday=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).minusDays(1);
        jdbc.update("UPDATE post SET view_count=12 WHERE id=1");
        jdbc.update("INSERT INTO post_daily_views(post_id,stat_date,view_count) VALUES(1,?,4)",yesterday);
        jdbc.update("UPDATE post_like SET create_time=? WHERE user_id=2",yesterday);
        jdbc.update("UPDATE post_favorite SET create_time=?",yesterday);
        var summary=social.summary(1,2L);
        assertEquals(1,summary.followingCount());assertEquals(12,summary.contributions().views());assertEquals(4,summary.contributions().yesterdayViews());
        assertEquals(1,summary.contributions().yesterdayLikes());assertEquals(1,summary.contributions().favorites());assertEquals(1,summary.contributions().yesterdayFavorites());
        var people=social.follows(1,false,1L,1,20);assertEquals(2,people.records().get(0).id());assertTrue(people.records().get(0).following());
        transaction.executeWithoutResult(status->{favorites.set(1,2,false);favorites.set(1,2,false);social.follow(1,2,false);social.follow(1,2,false);});
        assertEquals(0,value("SELECT COUNT(*) FROM post_favorite"));assertEquals(0,value("SELECT COUNT(*) FROM user_follow"));
    }
    @Test void activityHas365DaysAndRecentSubmissionsAreLimitedToTen() {
        var activity=new UserActivityService(jdbc);
        var today=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai"));
        for(int index=0;index<12;index++) jdbc.update("INSERT INTO oj_submission(oj_account_id,user_id,platform,remote_submission_id,external_problem_key,verdict,submitted_time,problem_id) VALUES(10,1,'CODEFORCES',?,'1A','OK',?,1)","new"+index,today.atStartOfDay().plusMinutes(index));
        var graph=activity.activity(1);assertEquals(365,graph.days().size());assertTrue(graph.submissions()>=12);assertTrue(graph.activeDays()>=1);
        var recent=activity.recent(1);assertEquals(10,recent.size());assertEquals(today.atStartOfDay().plusMinutes(11),recent.get(0).submittedTime());assertEquals(1L,recent.get(0).problemId());
    }
    @Test void editorPreferencesPersistAcrossReadsAndFollowTheMergedAccount() {
        var controller=new com.itnoduck.acmate.user.controller.EditorSettingsController(jdbc);
        var owner=new com.itnoduck.acmate.security.AuthenticatedUser(1L,"github_user","hash","Github User",null,null,null,false,true,List.of());
        var target=new com.itnoduck.acmate.security.AuthenticatedUser(2L,"email_user","hash","Email User",null,null,null,false,true,List.of());
        controller.put(owner,new com.itnoduck.acmate.user.controller.EditorSettingsController.Update(false,1L));
        assertFalse(new com.itnoduck.acmate.user.controller.EditorSettingsController(jdbc).get(owner).autocomplete());
        assertTrue(controller.get(target).autocomplete());
        transaction.executeWithoutResult(status->merger.merge(1,2));
        assertFalse(controller.get(target).autocomplete());assertEquals(0,value("SELECT COUNT(*) FROM user_editor_setting WHERE user_id=1"));
    }
}
