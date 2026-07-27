package com.itnoduck.acmate.discussion.controller;

import com.itnoduck.acmate.discussion.dto.*;
import com.itnoduck.acmate.discussion.service.DiscussionService;
import com.itnoduck.acmate.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class DiscussionController {

    private final DiscussionService discussionService;

    public DiscussionController(DiscussionService discussionService) {
        this.discussionService = discussionService;
    }

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "") String postType,
            @RequestParam(required = false) Long problemId,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<PostSummaryResponse> posts = discussionService.listPosts(postType, problemId, keyword, page, size);
        int total = discussionService.countPosts(postType, problemId, keyword);
        return Map.of("posts", posts, "total", total, "page", page, "size", size);
    }

    @PostMapping
    public PostDetailResponse create(@Valid @RequestBody CreatePostRequest request,
                                      @AuthenticationPrincipal AuthenticatedUser user) {
        return discussionService.createPost(request, user.getId());
    }

    @GetMapping("/{id}")
    public PostDetailResponse detail(@PathVariable Long id,
                                      @AuthenticationPrincipal AuthenticatedUser user) {
        return discussionService.getPostDetail(id, user.getId());
    }

    @PutMapping("/{id}")
    public PostDetailResponse update(@PathVariable Long id,
                                      @Valid @RequestBody UpdatePostRequest request,
                                      @AuthenticationPrincipal AuthenticatedUser user) {
        return discussionService.updatePost(id, request, user.getId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                        @AuthenticationPrincipal AuthenticatedUser user) {
        discussionService.deletePost(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable Long id,
                                        @AuthenticationPrincipal AuthenticatedUser user) {
        discussionService.restorePost(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/comments")
    public CommentResponse addComment(@PathVariable Long id,
                                       @Valid @RequestBody CreateCommentRequest request,
                                       @AuthenticationPrincipal AuthenticatedUser user) {
        return discussionService.addComment(id, request, user.getId());
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id,
                                               @PathVariable Long commentId,
                                               @AuthenticationPrincipal AuthenticatedUser user) {
        discussionService.deleteComment(id, commentId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/like")
    public ResponseEntity<Void> like(@PathVariable Long id,
                                    @AuthenticationPrincipal AuthenticatedUser user) {
        discussionService.like(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<Void> unlike(@PathVariable Long id,
                                       @AuthenticationPrincipal AuthenticatedUser user) {
        discussionService.unlike(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
