package com.itnoduck.acmate.problem.controller;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.problem.dto.UpdateProblemRequest;
import com.itnoduck.acmate.problem.entity.Problem;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import com.itnoduck.acmate.problem.service.ProblemCommandService;
import com.itnoduck.acmate.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ProblemReviewControllerTest {
 final JdbcTemplate jdbc=mock(JdbcTemplate.class);
 final ProblemMapper problems=mock(ProblemMapper.class);
 final ProblemCommandService command=mock(ProblemCommandService.class);
 final ProblemReviewController controller=new ProblemReviewController(jdbc,problems,command,new ObjectMapper());
 final AuthenticatedUser member=new AuthenticatedUser(2L,"member","hash","Member",null,null,null,false,true,List.of());
 @Test void ordinaryUsersCannotApprove(){assertEquals(403,assertThrows(BusinessException.class,()->controller.approve(1,new ProblemReviewController.Decision(null),member)).getCode());verifyNoInteractions(jdbc,command);}
 @Test void pendingQuotaRejectsFurtherSubmissions(){
  when(jdbc.queryForList(anyString(),eq(Long.class),eq(2L))).thenReturn(List.of(2L));
  when(jdbc.queryForObject(contains("COUNT(*)"),eq(Long.class),eq(2L))).thenReturn(5L);
  var request=new UpdateProblemRequest();request.setPlatform("NOWCODER");request.setExternalProblemKey("NC7");request.setTitle("new");
  assertEquals(429,assertThrows(BusinessException.class,()->controller.propose(7,request,member)).getCode());
  verifyNoInteractions(problems,command);
 }
 @Test void publicChangeBecomesPendingWithoutUpdatingOfficialProblem(){
  when(jdbc.queryForList(anyString(),eq(Long.class),eq(7L))).thenReturn(List.of(7L));
  when(jdbc.queryForList(anyString(),eq(Long.class),eq(2L))).thenReturn(List.of(2L));
  Problem p=new Problem();p.setId(7L);p.setPlatform("NOWCODER");p.setExternalProblemKey("NC7");p.setTitle("old");p.setUpdateTime(LocalDateTime.now());when(problems.selectById(7L)).thenReturn(p);
  var req=new UpdateProblemRequest();req.setPlatform("NOWCODER");req.setExternalProblemKey("NC7");req.setTitle("new");
  controller.propose(7,req,member);verify(jdbc).update(startsWith("INSERT INTO problem_change_request"),any(Object[].class));verifyNoInteractions(command);
 }
 @Test void approvedProposalUpdatesOnlyAfterAdminReview(){
  var admin=new AuthenticatedUser(1L,"admin","hash","Admin",null,null,null,true,true,List.of());
  Problem p=new Problem();p.setId(7L);p.setPlatform("NOWCODER");p.setExternalProblemKey("NC7");p.setTitle("old");p.setUpdateTime(LocalDateTime.now());when(problems.selectById(7L)).thenReturn(p);
  String fingerprint=com.itnoduck.acmate.common.cache.RedisJsonCache.keyFor("problem",new ObjectMapper().writeValueAsString(java.util.Arrays.asList(p.getPlatform(),p.getExternalProblemKey(),p.getTitle(),p.getSourceUrl(),p.getDifficulty(),p.getTags(),p.getContentMd()))).substring(8);
  when(jdbc.queryForList(contains("FROM problem_change_request"),eq(10L))).thenReturn(List.of(Map.of("problem_id",7L,"base_fingerprint",fingerprint,"proposed_content","{\"platform\":\"NOWCODER\",\"externalProblemKey\":\"NC7\",\"title\":\"new\"}","status","PENDING")));
  when(jdbc.queryForList(anyString(),eq(Long.class),eq(7L))).thenReturn(List.of(7L));
  controller.approve(10,new ProblemReviewController.Decision("通过"),admin);
  verify(command).updateProblem(eq(7L),any(UpdateProblemRequest.class),eq(1L),eq(true));
  verify(jdbc).update(contains("status='APPROVED'"),eq(1L),eq("通过"),eq(10L));
 }
}
