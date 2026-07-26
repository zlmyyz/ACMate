package com.itnoduck.acmate.discussion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itnoduck.acmate.auditlog.service.AuditLogService;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.discussion.dto.*;
import com.itnoduck.acmate.discussion.entity.Post;
import com.itnoduck.acmate.discussion.entity.PostComment;
import com.itnoduck.acmate.discussion.entity.PostLike;
import com.itnoduck.acmate.discussion.mapper.PostCommentMapper;
import com.itnoduck.acmate.discussion.mapper.PostLikeMapper;
import com.itnoduck.acmate.discussion.mapper.PostMapper;
import com.itnoduck.acmate.discussion.service.DiscussionService;
import com.itnoduck.acmate.problem.entity.Problem;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DiscussionServiceImpl implements DiscussionService {

    private final PostMapper postMapper;
    private final PostCommentMapper commentMapper;
    private final PostLikeMapper likeMapper;
    private final AppUserMapper userMapper;
    private final ProblemMapper problemMapper;
    private final AuditLogService auditLogService;

    public DiscussionServiceImpl(PostMapper postMapper, PostCommentMapper commentMapper,
                                  PostLikeMapper likeMapper, AppUserMapper userMapper,
                                  ProblemMapper problemMapper, AuditLogService auditLogService) {
        this.postMapper = postMapper;
        this.commentMapper = commentMapper;
        this.likeMapper = likeMapper;
        this.userMapper = userMapper;
        this.problemMapper = problemMapper;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public PostDetailResponse createPost(CreatePostRequest req, Long userId) {
        if ("ANNOUNCEMENT".equals(req.getPostType()) || "NOTICE".equals(req.getPostType())) {
            AppUser u = userMapper.selectById(userId);
            if (u == null || u.getIsAdmin() == null || u.getIsAdmin() != 1) {
                throw new BusinessException(403, "只有管理员才能发布公告");
            }
        }
        if ("SOLUTION".equals(req.getPostType()) && req.getProblemId() == null) {
            throw new BusinessException(400, "题解必须关联题目");
        }

        Post post = new Post();
        post.setTitle(req.getTitle().strip());
        post.setContentMd(req.getContentMd());
        post.setPostType(req.getPostType());
        post.setAuthorUserId(userId);
        post.setProblemId(req.getProblemId());
        post.setTrainingPlanId(req.getTrainingPlanId());
        post.setStatus(1);
        post.setIsPinned(0);
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        postMapper.insert(post);
        return toDetailResponse(post, userId);
    }

    @Override
    @Transactional
    public PostDetailResponse updatePost(Long postId, UpdatePostRequest req, Long userId) {
        Post post = getPost(postId);
        if (!post.getAuthorUserId().equals(userId)) {
            throw new BusinessException(403, "无权编辑该帖子");
        }
        if (req.getTitle() != null) post.setTitle(req.getTitle().strip());
        if (req.getContentMd() != null) post.setContentMd(req.getContentMd());
        postMapper.updateById(post);
        return toDetailResponse(post, userId);
    }

    @Override
    public PostDetailResponse getPostDetail(Long postId, Long userId) {
        Post post = getPost(postId);
        if (post.getStatus() == 0 && !post.getAuthorUserId().equals(userId)) {
            AppUser u = userMapper.selectById(userId);
            if (u == null || u.getIsAdmin() == null || u.getIsAdmin() != 1) {
                throw new BusinessException(404, "帖子不存在");
            }
        }
        post.setViewCount((post.getViewCount() != null ? post.getViewCount() : 0) + 1);
        postMapper.updateById(post);
        return toDetailResponse(post, userId);
    }

    @Override
    public List<PostSummaryResponse> listPosts(String postType, Long problemId, String keyword, int page, int size) {
        LambdaQueryWrapper<Post> qw = new LambdaQueryWrapper<>();
        qw.eq(Post::getStatus, 1);
        if (postType != null && !postType.isBlank()) qw.eq(Post::getPostType, postType);
        if (problemId != null) qw.eq(Post::getProblemId, problemId);
        if (keyword != null && !keyword.isBlank()) qw.like(Post::getTitle, keyword.strip());
        qw.orderByDesc(Post::getIsPinned).orderByDesc(Post::getCreateTime);

        Page<Post> result = postMapper.selectPage(new Page<>(page, size), qw);
        return result.getRecords().stream().map(this::toSummaryResponse).collect(Collectors.toList());
    }

    @Override
    public int countPosts(String postType, Long problemId, String keyword) {
        LambdaQueryWrapper<Post> qw = new LambdaQueryWrapper<>();
        qw.eq(Post::getStatus, 1);
        if (postType != null && !postType.isBlank()) qw.eq(Post::getPostType, postType);
        if (problemId != null) qw.eq(Post::getProblemId, problemId);
        if (keyword != null && !keyword.isBlank()) qw.like(Post::getTitle, keyword.strip());
        Long c = postMapper.selectCount(qw);
        return c != null ? c.intValue() : 0;
    }

    @Override
    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = getPost(postId);
        AppUser u = userMapper.selectById(userId);
        boolean isAdmin = u != null && u.getIsAdmin() != null && u.getIsAdmin() == 1;
        if (!post.getAuthorUserId().equals(userId) && !isAdmin) {
            throw new BusinessException(403, "无权停用该帖子");
        }
        String before = post.getStatus() != null && post.getStatus() == 1 ? "active" : "deactivated";
        post.setStatus(0);
        postMapper.updateById(post);
        if (isAdmin && !post.getAuthorUserId().equals(userId)) {
            auditLogService.log(userId, "ADMIN_DEACTIVATE_POST", "POST", postId, null, before, "deactivated");
        }
    }

    @Override
    @Transactional
    public CommentResponse addComment(Long postId, CreateCommentRequest req, Long userId) {
        Post post = getPost(postId);
        if (post.getStatus() == 0) throw new BusinessException(400, "帖子已停用");

        if (req.getParentId() != null) {
            PostComment parent = commentMapper.selectById(req.getParentId());
            if (parent == null || !parent.getPostId().equals(postId) || parent.getParentId() != null) {
                throw new BusinessException(400, "不能回复该评论");
            }
        }

        PostComment c = new PostComment();
        c.setPostId(postId);
        c.setUserId(userId);
        c.setParentId(req.getParentId());
        c.setReplyToUserId(req.getReplyToUserId());
        c.setContent(req.getContent().strip());
        c.setStatus(1);
        commentMapper.insert(c);

        post.setCommentCount((post.getCommentCount() != null ? post.getCommentCount() : 0) + 1);
        postMapper.updateById(post);

        return toCommentResponse(c);
    }

    @Override
    @Transactional
    public void deleteComment(Long postId, Long commentId, Long userId) {
        PostComment c = commentMapper.selectById(commentId);
        if (c == null || !c.getPostId().equals(postId)) throw new BusinessException(404, "评论不存在");
        AppUser u = userMapper.selectById(userId);
        boolean isAdmin = u != null && u.getIsAdmin() != null && u.getIsAdmin() == 1;
        if (!c.getUserId().equals(userId) && !isAdmin) throw new BusinessException(403, "无权删除该评论");
        String before = c.getStatus() != null && c.getStatus() == 1 ? "active" : "deactivated";
        c.setStatus(0);
        commentMapper.updateById(c);
        if (isAdmin && !c.getUserId().equals(userId)) {
            auditLogService.log(userId, "ADMIN_DEACTIVATE_COMMENT", "COMMENT", commentId, null, before, "deactivated");
        }
    }

    @Override
    @Transactional
    public void toggleLike(Long postId, Long userId) {
        getPost(postId);
        PostLike existing = likeMapper.selectOne(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, postId).eq(PostLike::getUserId, userId));
        Post post = postMapper.selectById(postId);
        if (existing != null) {
            likeMapper.deleteById(existing.getId());
            if (post != null && post.getLikeCount() > 0) {
                post.setLikeCount(post.getLikeCount() - 1);
                postMapper.updateById(post);
            }
        } else {
            PostLike like = new PostLike();
            like.setPostId(postId);
            like.setUserId(userId);
            likeMapper.insert(like);
            if (post != null) {
                post.setLikeCount((post.getLikeCount() != null ? post.getLikeCount() : 0) + 1);
                postMapper.updateById(post);
            }
        }
    }

    private Post getPost(Long id) {
        Post p = postMapper.selectById(id);
        if (p == null) throw new BusinessException(404, "帖子不存在");
        return p;
    }

    private PostSummaryResponse toSummaryResponse(Post p) {
        PostSummaryResponse r = new PostSummaryResponse();
        r.setId(p.getId()); r.setTitle(p.getTitle()); r.setPostType(p.getPostType());
        r.setAuthorUserId(p.getAuthorUserId()); r.setProblemId(p.getProblemId());
        r.setLikeCount(p.getLikeCount() != null ? p.getLikeCount() : 0);
        r.setCommentCount(p.getCommentCount() != null ? p.getCommentCount() : 0);
        r.setViewCount(p.getViewCount() != null ? p.getViewCount() : 0);
        r.setPinned(p.getIsPinned() != null && p.getIsPinned() == 1);
        r.setActive(p.getStatus() != null && p.getStatus() == 1);
        r.setCreateTime(p.getCreateTime());

        AppUser author = userMapper.selectById(p.getAuthorUserId());
        if (author != null) {
            r.setAuthorUsername(author.getUsername());
            r.setAuthorNickname(author.getNickname());
            r.setAuthorAvatarUrl(author.getAvatarUrl());
        }
        if (p.getProblemId() != null) {
            Problem prob = problemMapper.selectById(p.getProblemId());
            if (prob != null) r.setProblemTitle(prob.getTitle());
        }
        return r;
    }

    private PostDetailResponse toDetailResponse(Post p, Long userId) {
        PostDetailResponse r = new PostDetailResponse();
        r.setId(p.getId()); r.setTitle(p.getTitle()); r.setContentMd(p.getContentMd());
        r.setPostType(p.getPostType()); r.setAuthorUserId(p.getAuthorUserId());
        r.setProblemId(p.getProblemId()); r.setTrainingPlanId(p.getTrainingPlanId());
        r.setActive(p.getStatus() != null && p.getStatus() == 1);
        r.setPinned(p.getIsPinned() != null && p.getIsPinned() == 1);
        r.setLikeCount(p.getLikeCount() != null ? p.getLikeCount() : 0);
        r.setCommentCount(p.getCommentCount() != null ? p.getCommentCount() : 0);
        r.setViewCount(p.getViewCount() != null ? p.getViewCount() : 0);
        r.setCreateTime(p.getCreateTime()); r.setUpdateTime(p.getUpdateTime());

        AppUser author = userMapper.selectById(p.getAuthorUserId());
        if (author != null) {
            r.setAuthorUsername(author.getUsername());
            r.setAuthorNickname(author.getNickname());
            r.setAuthorAvatarUrl(author.getAvatarUrl());
        }
        if (p.getProblemId() != null) {
            Problem prob = problemMapper.selectById(p.getProblemId());
            if (prob != null) r.setProblemTitle(prob.getTitle());
        }

        Long liked = likeMapper.selectCount(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, p.getId()).eq(PostLike::getUserId, userId));
        r.setLikedByMe(liked != null && liked > 0);

        List<PostComment> comments = commentMapper.selectList(new LambdaQueryWrapper<PostComment>()
                .eq(PostComment::getPostId, p.getId()).eq(PostComment::getStatus, 1)
                .orderByAsc(PostComment::getCreateTime));

        Map<Long, List<PostComment>> repliesByParent = new LinkedHashMap<>();
        List<CommentResponse> topLevel = new ArrayList<>();

        for (PostComment c : comments) {
            if (c.getParentId() == null) {
                topLevel.add(toCommentResponse(c));
            } else {
                repliesByParent.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c);
            }
        }
        for (CommentResponse cr : topLevel) {
            List<PostComment> reps = repliesByParent.getOrDefault(cr.getId(), Collections.emptyList());
            cr.setReplies(reps.stream().map(this::toCommentResponse).collect(Collectors.toList()));
        }
        r.setComments(topLevel);
        return r;
    }

    private CommentResponse toCommentResponse(PostComment c) {
        CommentResponse r = new CommentResponse();
        r.setId(c.getId()); r.setUserId(c.getUserId()); r.setContent(c.getContent());
        r.setActive(c.getStatus() != null && c.getStatus() == 1);
        r.setCreateTime(c.getCreateTime());
        r.setReplyToUserId(c.getReplyToUserId());

        AppUser u = userMapper.selectById(c.getUserId());
        if (u != null) {
            r.setUsername(u.getUsername()); r.setNickname(u.getNickname()); r.setAvatarUrl(u.getAvatarUrl());
        }
        if (c.getReplyToUserId() != null) {
            AppUser replied = userMapper.selectById(c.getReplyToUserId());
            if (replied != null) r.setReplyToUsername(replied.getUsername());
        }
        return r;
    }
}
