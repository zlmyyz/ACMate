package com.itnoduck.acmate.practice;
import com.itnoduck.acmate.config.SecurityConfig;
import com.itnoduck.acmate.common.exception.GlobalExceptionHandler;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import com.itnoduck.acmate.problem.entity.Problem;
import com.itnoduck.acmate.problem.service.NowcoderProblemImportService;
import com.itnoduck.acmate.security.DatabaseUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(PracticeController.class)
@Import({SecurityConfig.class,GlobalExceptionHandler.class})
class PracticeControllerTest {
 @Autowired MockMvc mvc;
 @MockitoBean PracticeRunner runner;
 @MockitoBean com.itnoduck.acmate.common.cache.RedisJsonCache cache;
 @MockitoBean PracticeRateLimit rateLimit;
 @MockitoBean PracticeAccess access;
 @org.junit.jupiter.api.BeforeEach void cachePassThrough(){lenient().when(cache.getOrLoad(anyString(),any(),any(),any())).thenAnswer(i -> ((java.util.function.Supplier<?>)i.getArgument(3)).get());}
 final com.itnoduck.acmate.security.AuthenticatedUser member=new com.itnoduck.acmate.security.AuthenticatedUser(1L,"member","hash","member",null,null,null,false,true,List.of());
 @MockitoBean ProblemMapper problems;
 @MockitoBean NowcoderProblemImportService importer;
 @MockitoBean DatabaseUserDetailsService details;
 void sample(){var p=new Problem();p.setStatus(1);p.setPlatform("NOWCODER");p.setSourceUrl("https://ac.nowcoder.com/acm/contest/1/A");when(problems.selectById(1L)).thenReturn(p);when(importer.samples(p.getSourceUrl())).thenReturn(List.of(new NowcoderProblemImportService.Sample("1 2","3")));}
 @Test void ignoresCustomInputAndUsesServerFetchedPublicSample()throws Exception{
  sample();when(runner.run("CPP17","int main(){}","1 2")).thenReturn(new PracticeRunner.Result("FINISHED","3","",1,100));
  mvc.perform(post("/api/practice/run").with(user(member)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"problemId\":1,\"sampleIndex\":0,\"language\":\"CPP17\",\"sourceCode\":\"int main(){}\",\"input\":\"custom attacker input\"}")).andExpect(status().isOk());
  verify(runner).run("CPP17","int main(){}","1 2");
 }
 @Test void rejectsMissingSampleAndCustomOnlyRequests()throws Exception{
  sample();mvc.perform(post("/api/practice/run").with(user(member)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"problemId\":1,\"sampleIndex\":1,\"language\":\"CPP17\",\"sourceCode\":\"int main(){}\"}")).andExpect(status().isBadRequest());
  mvc.perform(post("/api/practice/run").with(user(member)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"language\":\"PYTHON3\",\"sourceCode\":\"print(1)\",\"input\":\"custom\"}")).andExpect(status().isBadRequest());verifyNoInteractions(runner);
 }
 @Test void protectsExecutionWithLoginAndCsrf()throws Exception{
  mvc.perform(post("/api/practice/run").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/practice/run").with(user(member)).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());verifyNoInteractions(runner);
 }
 @Test void customInputIsAllowedWithoutFetchingPublicSamples()throws Exception{
  sample();when(runner.run("GO","package main","5 7\n")).thenReturn(new PracticeRunner.Result("FINISHED","12","",1,100));
  mvc.perform(post("/api/practice/run").with(user(member)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"problemId\":1,\"inputMode\":\"CUSTOM\",\"language\":\"GO\",\"sourceCode\":\"package main\",\"input\":\"5 7\\n\"}")).andExpect(status().isOk());
  verify(runner).run("GO","package main","5 7\n");verifyNoInteractions(importer);
 }
 @Test void rejectsOversizedInputMissingCustomInputAndUnsupportedLanguages()throws Exception{
  sample();
  for(String body:List.of("{\"problemId\":1,\"inputMode\":\"CUSTOM\",\"language\":\"GO\",\"sourceCode\":\"source\"}","{\"problemId\":1,\"inputMode\":\"CUSTOM\",\"language\":\"SHELL\",\"sourceCode\":\"source\",\"input\":\"\"}","{\"problemId\":1,\"inputMode\":\"CUSTOM\",\"language\":\"GO\",\"sourceCode\":\"source\",\"input\":\""+"x".repeat(65537)+"\"}"))
   mvc.perform(post("/api/practice/run").with(user(member)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
  verifyNoInteractions(runner);
 }
}
