package com.itnoduck.acmate.oj.controller;
import com.itnoduck.acmate.config.SecurityConfig;
import com.itnoduck.acmate.common.exception.GlobalExceptionHandler;
import com.itnoduck.acmate.oj.service.NowcoderProfileSyncService;
import com.itnoduck.acmate.security.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(NowcoderProfileController.class)
@Import({SecurityConfig.class,GlobalExceptionHandler.class})
class NowcoderProfileControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean NowcoderProfileSyncService service;
    @MockitoBean DatabaseUserDetailsService details;
    final AuthenticatedUser member=new AuthenticatedUser(1L,"member","hash","Member",null,null,null,false,true,List.of());
    @Test void queueWriteRequiresLoginAndCsrf()throws Exception {
        mvc.perform(post("/api/nowcoder/profile-sync").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/nowcoder/profile-sync").with(user(member))).andExpect(status().isForbidden());
        mvc.perform(get("/api/nowcoder/profile-sync")).andExpect(status().isUnauthorized());verifyNoInteractions(service);
    }
    @Test void queueAlwaysUsesAuthenticatedUserAndReturnsAccepted()throws Exception {
        when(service.enqueue(1)).thenReturn(new NowcoderProfileSyncService.State("QUEUED",1,0,0,0,null,null,0,0,0));
        mvc.perform(post("/api/nowcoder/profile-sync?userId=999").with(csrf()).with(user(member))).andExpect(status().isAccepted()).andExpect(jsonPath("$.status").value("QUEUED"));
        verify(service).enqueue(1);
        mvc.perform(post("/api/nowcoder/profile-sync/retry-imports?userId=999").with(csrf()).with(user(member))).andExpect(status().isOk());verify(service).retryImports(1);
    }
}
