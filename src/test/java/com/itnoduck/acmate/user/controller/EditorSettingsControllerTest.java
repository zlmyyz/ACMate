package com.itnoduck.acmate.user.controller;
import com.itnoduck.acmate.config.SecurityConfig;
import com.itnoduck.acmate.common.exception.GlobalExceptionHandler;
import com.itnoduck.acmate.security.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(EditorSettingsController.class)
@Import({SecurityConfig.class,GlobalExceptionHandler.class})
class EditorSettingsControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean JdbcTemplate jdbc;
    @MockitoBean DatabaseUserDetailsService details;
    final AuthenticatedUser member=new AuthenticatedUser(1L,"member","hash","Member",null,null,null,false,true,List.of());
    @Test void privatePreferencesRequireLoginAndWritesNeedCsrf()throws Exception{
        mvc.perform(get("/api/users/me/editor-settings")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/users/me/editor-settings").with(user(member)).contentType(MediaType.APPLICATION_JSON).content("{\"autocomplete\":false,\"accountId\":1}")).andExpect(status().isForbidden());verifyNoInteractions(jdbc);
    }
    @Test void readsStoredPreferenceAndWritesOnlyAuthenticatedAccount()throws Exception{
        when(jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE id=? AND status=1",Long.class,1L)).thenReturn(1L);
        when(jdbc.queryForList("SELECT autocomplete FROM user_editor_setting WHERE user_id=?",Integer.class,1L)).thenReturn(List.of(0));
        mvc.perform(get("/api/users/me/editor-settings").with(user(member))).andExpect(status().isOk()).andExpect(jsonPath("$.autocomplete").value(false));
        mvc.perform(put("/api/users/me/editor-settings").with(user(member)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"autocomplete\":true,\"accountId\":1}")).andExpect(status().isOk()).andExpect(jsonPath("$.autocomplete").value(true));
        verify(jdbc).update("INSERT INTO user_editor_setting(user_id,autocomplete) VALUES(?,?) ON DUPLICATE KEY UPDATE autocomplete=VALUES(autocomplete)",1L,true);
    }
    @Test void refusesStaleAccountRequestsAfterAccountSwitch()throws Exception{
        when(jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE id=? AND status=1",Long.class,1L)).thenReturn(1L);
        mvc.perform(put("/api/users/me/editor-settings").with(user(member)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"autocomplete\":false,\"accountId\":2}")).andExpect(status().isConflict());
        verify(jdbc,never()).update(anyString(),any(Object[].class));
    }
    @Test void comparesLargeAccountIdsByValue()throws Exception{
        var owner=new AuthenticatedUser(1000L,"owner","hash","Owner",null,null,null,false,true,List.of());
        when(jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE id=? AND status=1",Long.class,1000L)).thenReturn(1L);
        mvc.perform(put("/api/users/me/editor-settings").with(user(owner)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"autocomplete\":false,\"accountId\":1000}")).andExpect(status().isOk());
    }
    @Test void savesPreferredLanguageAndRejectsUnsupportedValues()throws Exception{
        when(jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE id=? AND status=1",Long.class,1L)).thenReturn(1L);
        when(jdbc.queryForList("SELECT preferred_language FROM user_editor_setting WHERE user_id=?",String.class,1L)).thenReturn(List.of("GO"));
        mvc.perform(put("/api/users/me/editor-settings").with(user(member)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"autocomplete\":true,\"accountId\":1,\"language\":\"GO\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.language").value("GO"));
        verify(jdbc).update("INSERT INTO user_editor_setting(user_id,autocomplete,preferred_language) VALUES(?,?,?) ON DUPLICATE KEY UPDATE autocomplete=VALUES(autocomplete),preferred_language=VALUES(preferred_language)",1L,true,"GO");
        mvc.perform(put("/api/users/me/editor-settings").with(user(member)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"autocomplete\":true,\"accountId\":1,\"language\":\"UNKNOWN\"}"))
            .andExpect(status().isBadRequest());
    }
}
