package com.itnoduck.acmate.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itnoduck.acmate.admin.service.AdminContentService;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.discussion.entity.Post;
import com.itnoduck.acmate.discussion.entity.PostComment;
import com.itnoduck.acmate.discussion.mapper.PostCommentMapper;
import com.itnoduck.acmate.discussion.mapper.PostMapper;
import com.itnoduck.acmate.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AdminContentServiceImpl implements AdminContentService {

    private final PostMapper postMapper;
    private final PostCommentMapper commentMapper;

    public AdminContentServiceImpl(PostMapper postMapper, PostCommentMapper commentMapper) {
        this.postMapper = postMapper;
        this.commentMapper = commentMapper;
    }

    @Override
    public Map<String, Object> listPosts(int page, int size, String keyword, String postType, Integer status, AuthenticatedUser user) {
        if (!user.isAdmin()) throw new BusinessException(403, "无权访问");
        var qw = new LambdaQueryWrapper<Post>();
        if (keyword != null && !keyword.isBlank()) qw.like(Post::getTitle, keyword);
        if (postType != null && !postType.isBlank()) qw.eq(Post::getPostType, postType);
        if (status != null) qw.eq(Post::getStatus, status);
        qw.orderByDesc(Post::getCreateTime);
        var result = postMapper.selectPage(new Page<>(page, size), qw);
        List<Map<String, Object>> items = new ArrayList<>();
        for (var p : result.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("title", p.getTitle());
            m.put("authorUserId", p.getAuthorUserId());
            m.put("postType", p.getPostType());
            m.put("status", p.getStatus());
            m.put("likeCount", p.getLikeCount());
            m.put("commentCount", p.getCommentCount());
            m.put("deactivationSource", p.getDeactivationSource());
            m.put("deactivationReason", p.getDeactivationReason());
            m.put("createTime", p.getCreateTime() != null ? p.getCreateTime().toString() : null);
            items.add(m);
        }
        return Map.of("items", items, "total", result.getTotal(), "page", page, "size", size);
    }

    @Override
    @Transactional
    public void deactivatePost(Long id, String reason, AuthenticatedUser user) {
        if (!user.isAdmin()) throw new BusinessException(403, "无权访问");
        if (reason == null || reason.isBlank()) throw new BusinessException(400, "停用原因不能为空");
        var p = postMapper.selectById(id);
        if (p == null) throw new BusinessException(404, "帖子不存在");
        p.setStatus(0);
        p.setDeactivationSource("ADMIN");
        p.setDeactivationReason(reason);
        p.setDeactivatedBy(user.getId());
        p.setDeactivationTime(LocalDateTime.now());
        postMapper.updateById(p);
    }

    @Override
    @Transactional
    public void restorePost(Long id, AuthenticatedUser user) {
        if (!user.isAdmin()) throw new BusinessException(403, "无权访问");
        var p = postMapper.selectById(id);
        if (p == null) throw new BusinessException(404, "帖子不存在");
        p.setStatus(1);
        p.setDeactivationSource(null);
        p.setDeactivationReason(null);
        p.setDeactivatedBy(null);
        p.setDeactivationTime(null);
        postMapper.updateById(p);
    }

    @Override
    public Map<String, Object> listComments(int page, int size, Long postId, String keyword, AuthenticatedUser user) {
        if (!user.isAdmin()) throw new BusinessException(403, "无权访问");
        var qw = new LambdaQueryWrapper<PostComment>();
        if (postId != null) qw.eq(PostComment::getPostId, postId);
        if (keyword != null && !keyword.isBlank()) qw.like(PostComment::getContent, keyword);
        qw.orderByDesc(PostComment::getCreateTime);
        var result = commentMapper.selectPage(new Page<>(page, size), qw);
        List<Map<String, Object>> items = new ArrayList<>();
        for (var c : result.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("postId", c.getPostId());
            m.put("userId", c.getUserId());
            m.put("content", c.getContent());
            m.put("status", c.getStatus());
            m.put("deactivationSource", c.getDeactivationSource());
            m.put("deactivationReason", c.getDeactivationReason());
            m.put("createTime", c.getCreateTime() != null ? c.getCreateTime().toString() : null);
            items.add(m);
        }
        return Map.of("items", items, "total", result.getTotal(), "page", page, "size", size);
    }

    @Override
    @Transactional
    public void deactivateComment(Long id, String reason, AuthenticatedUser user) {
        if (!user.isAdmin()) throw new BusinessException(403, "无权访问");
        if (reason == null || reason.isBlank()) throw new BusinessException(400, "停用原因不能为空");
        var c = commentMapper.selectById(id);
        if (c == null) throw new BusinessException(404, "评论不存在");
        c.setStatus(0);
        c.setDeactivationSource("ADMIN");
        c.setDeactivationReason(reason);
        c.setDeactivatedBy(user.getId());
        c.setDeactivationTime(LocalDateTime.now());
        commentMapper.updateById(c);
    }

    @Override
    @Transactional
    public void restoreComment(Long id, AuthenticatedUser user) {
        if (!user.isAdmin()) throw new BusinessException(403, "无权访问");
        var c = commentMapper.selectById(id);
        if (c == null) throw new BusinessException(404, "评论不存在");
        c.setStatus(1);
        c.setDeactivationSource(null);
        c.setDeactivationReason(null);
        c.setDeactivatedBy(null);
        c.setDeactivationTime(null);
        commentMapper.updateById(c);
    }
}
