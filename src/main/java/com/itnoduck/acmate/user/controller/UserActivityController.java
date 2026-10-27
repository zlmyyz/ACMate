package com.itnoduck.acmate.user.controller;
import com.itnoduck.acmate.user.service.UserActivityService;
import com.itnoduck.acmate.user.service.PublicUserActivityService;
import com.itnoduck.acmate.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/users/{id}")
public class UserActivityController {
    private final UserActivityService activity;
    private final PublicUserActivityService publicActivity;
    public UserActivityController(UserActivityService activity,PublicUserActivityService publicActivity) {this.activity=activity;this.publicActivity=publicActivity;}
    private boolean self(long id,AuthenticatedUser viewer){return viewer!=null&&viewer.getId().longValue()==id;}
    @GetMapping("/activity") public UserActivityService.Activity activity(@PathVariable long id,@RequestParam(required=false) String platform,@AuthenticationPrincipal AuthenticatedUser viewer) {return self(id,viewer)?activity.activity(id,platform):publicActivity.activity(id,platform);}
    @GetMapping("/submissions/recent") public List<UserActivityService.Submission> recent(@PathVariable long id,@RequestParam(required=false) String platform,@AuthenticationPrincipal AuthenticatedUser viewer) {return self(id,viewer)?activity.recent(id,platform):publicActivity.recent(id,platform);}
    @GetMapping("/oj-overview") public UserActivityService.Overview overview(@PathVariable long id,@RequestParam(required=false) String platform,@AuthenticationPrincipal AuthenticatedUser viewer) {return self(id,viewer)?activity.overview(id,platform):publicActivity.overview(id,platform);}
}
