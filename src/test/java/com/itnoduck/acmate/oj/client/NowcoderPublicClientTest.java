package com.itnoduck.acmate.oj.client;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
class NowcoderPublicClientTest {
 final NowcoderPublicClient client=new NowcoderPublicClient(new ObjectMapper());
 @Test void filtersOtherUsersProblemsTeamsAndUnfinishedVerdicts(){
  String body="""
  {"code":0,"data":{"data":[
  {"submissionId":11,"userId":12,"problemId":99,"statusMessage":"答案正确","submitTime":1793005029000,"language":"C++"},
  {"submissionId":12,"userId":13,"problemId":99,"statusMessage":"答案正确","submitTime":1793005029000},
  {"submissionId":13,"userId":12,"problemId":98,"statusMessage":"答案正确","submitTime":1793005029000},
  {"submissionId":14,"userId":12,"problemId":99,"statusMessage":"答案正确","submitTime":1793005029000,"isTeam":true},
  {"submissionId":15,"userId":12,"problemId":99,"statusMessage":"评测中","submitTime":1793005029000}
  ]}}
  """;
  var rows=client.parse(body,12,99);assertEquals(1,rows.size());assertEquals(11,rows.get(0).id());assertEquals("OK",rows.get(0).verdict());
 }
 @Test void unknownVerdictsNeverBecomeAccepted(){
  assertNull(NowcoderPublicClient.verdict("未知结果"));assertEquals("WRONG_ANSWER",NowcoderPublicClient.verdict("答案错误"));
  assertEquals("COMPILATION_ERROR",NowcoderPublicClient.verdict("编译错误"));assertNull(NowcoderPublicClient.verdict("部分正确"));
 }
 @Test @EnabledIfSystemProperty(named="nowcoder.live-check",matches="true") void readsPublicFilteredContestResultsWithoutCookies(){
  var rows=client.submissions(19304,534698258,212955);
  assertFalse(rows.isEmpty());assertTrue(rows.stream().allMatch(r->r.userId()==534698258&&r.problemId()==212955));
  assertTrue(rows.stream().anyMatch(r->"OK".equals(r.verdict())));
 }
}
