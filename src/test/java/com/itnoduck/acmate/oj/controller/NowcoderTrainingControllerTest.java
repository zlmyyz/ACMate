package com.itnoduck.acmate.oj.controller;
import com.itnoduck.acmate.config.SecurityConfig;
import com.itnoduck.acmate.common.exception.GlobalExceptionHandler;
import com.itnoduck.acmate.oj.service.NowcoderTrainingService;
import com.itnoduck.acmate.security.DatabaseUserDetailsService;
import com.itnoduck.acmate.security.AuthenticatedUser;
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
@WebMvcTest(NowcoderTrainingController.class)
@Import({SecurityConfig.class,GlobalExceptionHandler.class})
class NowcoderTrainingControllerTest {
 @Autowired MockMvc mvc;
 @MockitoBean NowcoderTrainingService service;
 @MockitoBean DatabaseUserDetailsService details;
 final AuthenticatedUser member=new AuthenticatedUser(1L,"member","hash","Member",null,null,null,false,true,List.of());
 @Test void officialSyncRequiresLoginAndCsrf()throws Exception{
  mvc.perform(post("/api/nowcoder/problems/1/sync").with(csrf())).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/nowcoder/problems/1/sync").with(user(member))).andExpect(status().isForbidden());verifyNoInteractions(service);
 }
 @Test void usesServerAuthenticatedUserRatherThanBrowserSuppliedIdentity()throws Exception{
  when(service.sync(1,1)).thenReturn(new NowcoderTrainingService.Sync(1,1,1,"同步成功"));
  mvc.perform(post("/api/nowcoder/problems/1/sync?userId=999").with(user(member)).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.inserted").value(1));verify(service).sync(1,1);
 }
 @Test void rejectsMalformedBindingUidBeforeWriting()throws Exception{
  mvc.perform(post("/api/nowcoder/account").with(user(member)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"uid\":\"http://localhost\"}")).andExpect(status().isBadRequest());verifyNoInteractions(service);
 }
}
