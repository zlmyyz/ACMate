package com.itnoduck.acmate.admin.service;

import com.itnoduck.acmate.security.AuthenticatedUser;
import java.util.Map;

public interface AdminContentService {
    Map<String, Object> listPosts(int page, int size, String keyword, String postType, Integer status, AuthenticatedUser user);
    void deactivatePost(Long id, String reason, AuthenticatedUser user);
    void restorePost(Long id, AuthenticatedUser user);
    Map<String, Object> listComments(int page, int size, Long postId, String keyword, AuthenticatedUser user);
    void deactivateComment(Long id, String reason, AuthenticatedUser user);
    void restoreComment(Long id, AuthenticatedUser user);
}
