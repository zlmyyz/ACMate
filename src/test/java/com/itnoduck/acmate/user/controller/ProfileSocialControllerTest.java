package com.itnoduck.acmate.user.controller;
import com.itnoduck.acmate.config.SecurityConfig;
import com.itnoduck.acmate.common.exception.GlobalExceptionHandler;
import com.itnoduck.acmate.security.AuthenticatedUser;
import com.itnoduck.acmate.security.DatabaseUserDetailsService;
import com.itnoduck.acmate.user.service.*;
import com.itnoduck.acmate.discussion.service.PostFavoriteService;
import com.itnoduck.acmate.discussion.controller.PostFavoriteController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest({UserSocialController.class,UserActivityController.class,PostFavoriteController.class})
@Import({SecurityConfig.class,GlobalExceptionHandler.class})
class ProfileSocialControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean UserSocialService social;
    @MockitoBean UserActivityService activity;
    @MockitoBean PublicUserActivityService publicActivity;
    @MockitoBean PostFavoriteService favorites;
    @MockitoBean DatabaseUserDetailsService details;
    final AuthenticatedUser principal=new AuthenticatedUser(1L,"member","hash","Member",null,null,null,false,true,List.of());
    @Test void publicSummaryOnlyExposesRegionAndCounts() throws Exception {
        when(social.summary(2,null)).thenReturn(new UserSocialService.Summary(1,2,false,"河北",new UserSocialService.Contributions(3,1,2,1,0,0)));
        mvc.perform(get("/api/users/2/summary")).andExpect(status().isOk()).andExpect(jsonPath("$.ipRegion").value("河北"))
                .andExpect(jsonPath("$.lastLoginIp").doesNotExist()).andExpect(jsonPath("$.email").doesNotExist());
    }
    @Test void activityAndRecentInterfacesArePublicAndOnlyReadStoredData() throws Exception {
        when(publicActivity.recent(2,null)).thenReturn(List.of());
        mvc.perform(get("/api/users/2/submissions/recent")).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/users/2/activity")).andExpect(status().isOk());
        verify(publicActivity).activity(2,null);
    }
    @Test void ownerUsesPrivateActivityWhileVisitorsUseFilteredActivity() throws Exception {
        when(activity.recent(1,null)).thenReturn(List.of());
        when(publicActivity.recent(1,null)).thenReturn(List.of());
        mvc.perform(get("/api/users/1/submissions/recent").with(user(principal))).andExpect(status().isOk());
        mvc.perform(get("/api/users/1/submissions/recent")).andExpect(status().isOk());
        verify(activity).recent(1,null);verify(publicActivity).recent(1,null);
    }
    @Test void followAndFavoriteWritesRequireLoginAndCsrf() throws Exception {
        for(String path:List.of("/api/users/2/follow","/api/posts/3/favorite")) {
            mvc.perform(put(path).with(csrf())).andExpect(status().isUnauthorized());
            mvc.perform(put(path).with(user(principal))).andExpect(status().isForbidden());
        }
        verifyNoInteractions(social,favorites);
    }
    @Test void platformQueriesRemainPublicReadOnlyAndValidateInService()throws Exception {
        mvc.perform(get("/api/users/2/activity?platform=NOWCODER")).andExpect(status().isOk());verify(publicActivity).activity(2,"NOWCODER");
        mvc.perform(get("/api/users/2/submissions/recent?platform=NOWCODER")).andExpect(status().isOk());verify(publicActivity).recent(2,"NOWCODER");
        mvc.perform(get("/api/users/2/oj-overview?platform=NOWCODER")).andExpect(status().isOk());verify(publicActivity).overview(2,"NOWCODER");
    }
    @Test void followUsesCurrentPrincipalAndSupportsUnfollow() throws Exception {
        mvc.perform(put("/api/users/2/follow").param("userId","999").with(user(principal)).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/users/2/follow").with(user(principal)).with(csrf())).andExpect(status().isNoContent());
        verify(social).follow(1,2,true);verify(social).follow(1,2,false);
    }
    @Test void favoritesUseCurrentPrincipalAndSupportRemoval() throws Exception {
        when(favorites.set(3,1,true)).thenReturn(Map.of("favorited",true,"count",1));
        when(favorites.set(3,1,false)).thenReturn(Map.of("favorited",false,"count",0));
        mvc.perform(put("/api/posts/3/favorite").param("userId","999").with(user(principal)).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.favorited").value(true));
        mvc.perform(delete("/api/posts/3/favorite").with(user(principal)).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.count").value(0));
        verify(favorites).set(3,1,true);verify(favorites).set(3,1,false);
    }
}
