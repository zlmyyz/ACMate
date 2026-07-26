package com.itnoduck.acmate.discussion.service;

import com.itnoduck.acmate.discussion.dto.*;
import java.util.List;

public interface DiscussionService {
    PostDetailResponse createPost(CreatePostRequest req, Long userId);
    PostDetailResponse updatePost(Long postId, UpdatePostRequest req, Long userId);
    PostDetailResponse getPostDetail(Long postId, Long userId);
    List<PostSummaryResponse> listPosts(String postType, Long problemId, String keyword, int page, int size);
    int countPosts(String postType, Long problemId, String keyword);
    void deletePost(Long postId, Long userId);
    CommentResponse addComment(Long postId, CreateCommentRequest req, Long userId);
    void deleteComment(Long postId, Long commentId, Long userId);
    void toggleLike(Long postId, Long userId);
}
