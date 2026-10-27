package com.itnoduck.acmate.problem.controller;
import com.itnoduck.acmate.config.SecurityConfig;
import com.itnoduck.acmate.common.exception.GlobalExceptionHandler;
import com.itnoduck.acmate.security.DatabaseUserDetailsService;
import com.itnoduck.acmate.problem.service.NowcoderProblemImportService;
import com.itnoduck.acmate.problem.dto.ImportedProblemResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(ProblemImportController.class)
@Import({SecurityConfig.class,GlobalExceptionHandler.class})
class ProblemImportControllerTest {
 @Autowired MockMvc mvc;
 @MockitoBean NowcoderProblemImportService importer;
 @MockitoBean DatabaseUserDetailsService details;
 static final String BODY="{\"url\":\"https://ac.nowcoder.com/acm/problem/123\"}";
 @Test void requiresLogin() throws Exception {
  mvc.perform(post("/api/problems/import/nowcoder").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isUnauthorized());
  verifyNoInteractions(importer);
 }
 @Test void requiresCsrf() throws Exception {
  mvc.perform(post("/api/problems/import/nowcoder").with(user("member")).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
  verifyNoInteractions(importer);
 }
 @Test void rejectsEmptyUrlBeforeFetching() throws Exception {
  mvc.perform(post("/api/problems/import/nowcoder").with(user("member")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"\"}")).andExpect(status().isBadRequest());
  verifyNoInteractions(importer);
 }
 @Test void loggedInMembersCanPreviewWithoutCreatingDatabaseRecords() throws Exception {
  when(importer.preview("https://ac.nowcoder.com/acm/problem/123")).thenReturn(new ImportedProblemResponse("NOWCODER","NC123","测试题","https://ac.nowcoder.com/acm/problem/123","","","题面",List.of()));
  mvc.perform(post("/api/problems/import/nowcoder").with(user("member")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isOk()).andExpect(jsonPath("$.externalProblemKey").value("NC123"));
 }
}
