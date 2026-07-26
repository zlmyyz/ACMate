package com.itnoduck.acmate.admin.controller;

import com.itnoduck.acmate.admin.service.AdminContentService;
import com.itnoduck.acmate.security.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminContentController {

    private final AdminContentService adminContentService;

    public AdminContentController(AdminContentService adminContentService) {
        this.adminContentService = adminContentService;
    }

    @GetMapping("/posts")
    public Map<String, Object> listPosts(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          @RequestParam(defaultValue = "") String keyword,
                                          @RequestParam(required = false) String postType,
                                          @RequestParam(required = false) Integer status,
                                          @AuthenticationPrincipal AuthenticatedUser user) {
        return adminContentService.listPosts(page, size, keyword, postType, status, user);
    }

    @PostMapping("/posts/{id}/deactivate")
    public ResponseEntity<Void> deactivatePost(@PathVariable Long id,
                                                @RequestBody Map<String, String> body,
                                                @AuthenticationPrincipal AuthenticatedUser user) {
        adminContentService.deactivatePost(id, body.get("reason"), user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/posts/{id}/restore")
    public ResponseEntity<Void> restorePost(@PathVariable Long id,
                                             @AuthenticationPrincipal AuthenticatedUser user) {
        adminContentService.restorePost(id, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/comments")
    public Map<String, Object> listComments(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(required = false) Long postId,
                                             @RequestParam(defaultValue = "") String keyword,
                                             @AuthenticationPrincipal AuthenticatedUser user) {
        return adminContentService.listComments(page, size, postId, keyword, user);
    }

    @PostMapping("/comments/{id}/deactivate")
    public ResponseEntity<Void> deactivateComment(@PathVariable Long id,
                                                   @RequestBody Map<String, String> body,
                                                   @AuthenticationPrincipal AuthenticatedUser user) {
        adminContentService.deactivateComment(id, body.get("reason"), user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/comments/{id}/restore")
    public ResponseEntity<Void> restoreComment(@PathVariable Long id,
                                                @AuthenticationPrincipal AuthenticatedUser user) {
        adminContentService.restoreComment(id, user);
        return ResponseEntity.noContent().build();
    }
}
