package com.itnoduck.acmate.user.controller;
import com.itnoduck.acmate.security.AuthenticatedUser;
import com.itnoduck.acmate.user.service.UserSocialService;
import com.itnoduck.acmate.common.dto.PageResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/users")
public class UserSocialController {
    private final UserSocialService social;
    public UserSocialController(UserSocialService social) { this.social=social; }
    @GetMapping("/{id}/summary")
    public UserSocialService.Summary summary(@PathVariable long id,@AuthenticationPrincipal AuthenticatedUser viewer) { return social.summary(id,viewer==null?null:viewer.getId()); }
    @GetMapping("/{id}/following")
    public PageResponse<UserSocialService.Member> following(@PathVariable long id,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,@AuthenticationPrincipal AuthenticatedUser viewer) { return social.follows(id,false,viewer==null?null:viewer.getId(),page,size); }
    @GetMapping("/{id}/followers")
    public PageResponse<UserSocialService.Member> followers(@PathVariable long id,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,@AuthenticationPrincipal AuthenticatedUser viewer) { return social.follows(id,true,viewer==null?null:viewer.getId(),page,size); }
    @PutMapping("/{id}/follow")
    public ResponseEntity<Void> follow(@PathVariable long id,@AuthenticationPrincipal AuthenticatedUser viewer) { social.follow(viewer.getId(),id,true);return ResponseEntity.noContent().build(); }
    @DeleteMapping("/{id}/follow")
    public ResponseEntity<Void> unfollow(@PathVariable long id,@AuthenticationPrincipal AuthenticatedUser viewer) { social.follow(viewer.getId(),id,false);return ResponseEntity.noContent().build(); }
}
