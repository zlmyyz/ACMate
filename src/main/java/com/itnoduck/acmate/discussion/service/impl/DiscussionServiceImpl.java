package com.itnoduck.acmate.discussion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import com.itnoduck.acmate.notification.event.NotificationEvent;
import com.itnoduck.acmate.problem.entity.Problem;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DiscussionServiceImpl implements DiscussionService {

    private static final Set<String> VALID_POST_TYPES = Set.of(
            "SOLUTION", "QUESTION", "CONTEST_SUMMARY", "TRAINING_EXPERIENCE", "ANNOUNCEMENT", "OTHER");

    private final PostMapper postMapper;
    private final PostCommentMapper commentMapper;
    private final PostLikeMapper likeMapper;
    private final AppUserMapper userMapper;
    private final ProblemMapper problemMapper;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    public DiscussionServiceImpl(PostMapper postMapper, PostCommentMapper commentMapper,
                                  PostLikeMapper likeMapper, AppUserMapper userMapper,
                                  ProblemMapper problemMapper, AuditLogService auditLogService,
                                  ApplicationEventPublisher eventPublisher) {
        this.postMapper = postMapper;
        this.commentMapper = commentMapper;
        this.likeMapper = likeMapper;
        this.userMapper = userMapper;
        this.problemMapper = problemMapper;
        this.auditLogService = auditLogService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public PostDetailResponse createPost(CreatePostRequest req, Long userId) {
        String type = req.getPostType();
        if (type == null || !VALID_POST_TYPES.contains(type)) {
            throw new BusinessException(400, "无效的帖子类型");
        }
        if ("ANNOUNCEMENT".equals(type)) {
            AppUser u = userMapper.selectById(userId);
            if (u == null || u.getIsAdmin() == null || u.getIsAdmin() != 1) {
                throw new BusinessException(403, "只有管理员才能发布公告");
            }
        }
        if ("SOLUTION".equals(type) && req.getProblemId() == null) {
            throw new BusinessException(400, "题解必须关联题目");
        }

        String title = req.getTitle() != null ? req.getTitle().strip() : "";
        if (title.isEmpty()) throw new BusinessException(400, "标题不能为空");
        String contentMd = req.getContentMd();
        if (contentMd == null || contentMd.isBlank()) throw new BusinessException(400, "内容不能为空");

        Post post = new Post();
        post.setTitle(title);
        post.setContentMd(contentMd);
        post.setPostType(type);
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
        if (req.getTitle() != null) {
            String t = req.getTitle().strip();
            if (t.isEmpty()) throw new BusinessException(400, "标题不能为空");
            post.setTitle(t);
        }
        if (req.getContentMd() != null) {
            if (req.getContentMd().isBlank()) throw new BusinessException(400, "内容不能为空");
            post.setContentMd(req.getContentMd());
        }
        postMapper.updateById(post);
        return toDetailResponse(post, userId);
    }

    @Override
    @Transactional
    public PostDetailResponse getPostDetail(Long postId, Long userId) {
        Post post = getPost(postId);
        boolean isAdmin = isAdminUser(userId);
        if (post.getStatus() == 0 && !post.getAuthorUserId().equals(userId) && !isAdmin) {
            throw new BusinessException(404, "帖子不存在");
        }
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .setSql("view_count = view_count + 1")
                .eq(Post::getId, postId));
        post = postMapper.selectById(postId);
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
        List<Post> posts = result.getRecords();
        if (posts.isEmpty()) return List.of();

        Map<Long, AppUser> userMap = batchLoadUsers(posts.stream().map(Post::getAuthorUserId).collect(Collectors.toSet()));
        Set<Long> pids = posts.stream().map(Post::getProblemId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Problem> probMap = batchLoadProblems(pids);

        return posts.stream().map(p -> toSummaryResponse(p, userMap, probMap)).collect(Collectors.toList());
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
        if (!post.getAuthorUserId().equals(userId)) {
            throw new BusinessException(403, "无权停用该帖子");
        }
        if (post.getStatus() != null && post.getStatus() == 0) return;
        post.setStatus(0);
        post.setDeactivationSource("CREATOR");
        post.setDeactivatedBy(userId);
        post.setDeactivationTime(LocalDateTime.now());
        postMapper.updateById(post);
    }

    @Override
    @Transactional
    public void restorePost(Long postId, Long userId) {
        Post post = getPost(postId);
        boolean isAdmin = isAdminUser(userId);
        if (!post.getAuthorUserId().equals(userId) && !isAdmin) {
            if (post.getStatus() == null || post.getStatus() == 0) throw new BusinessException(404, "帖子不存在");
            throw new BusinessException(403, "无权恢复该帖子");
        }
        if (post.getStatus() != null && post.getStatus() == 1) return;
        if (!isAdmin && !"CREATOR".equals(post.getDeactivationSource())) {
            throw new BusinessException(403, "该帖子由管理员停用，无法自行恢复");
        }
        post.setStatus(1);
        post.setDeactivationSource(null);
        post.setDeactivationReason(null);
        post.setDeactivatedBy(null);
        post.setDeactivationTime(null);
        postMapper.updateById(post);
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

        String content = req.getContent() != null ? req.getContent().strip() : "";
        if (content.isEmpty()) throw new BusinessException(400, "评论内容不能为空");

        PostComment c = new PostComment();
        c.setPostId(postId);
        c.setUserId(userId);
        c.setParentId(req.getParentId());
        c.setReplyToUserId(req.getReplyToUserId());
        c.setContent(content);
        c.setStatus(1);
        commentMapper.insert(c);

        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .setSql("comment_count = comment_count + 1")
                .eq(Post::getId, postId));

        AppUser u = userMapper.selectById(userId);
        CommentResponse resp = toCommentResponse(c, u);
        if (c.getReplyToUserId() != null) {
            AppUser replied = userMapper.selectById(c.getReplyToUserId());
            if (replied != null) resp.setReplyToUsername(replied.getUsername());
        }

        // Notifications
        if (c.getParentId() == null) {
            var recipients = new HashSet<Long>();
            recipients.add(post.getAuthorUserId());
            var payload = new LinkedHashMap<String, Object>();
            payload.put("postTitle", post.getTitle());
            payload.put("actorNickname", u != null ? u.getNickname() : null);
            eventPublisher.publishEvent(new NotificationEvent(
                    recipients, userId, "POST_COMMENTED", "POST", postId, payload));
        } else {
            PostComment parent = commentMapper.selectById(c.getParentId());
            if (parent != null) {
                var recipients = new HashSet<Long>();
                recipients.add(parent.getUserId());
                var payload = new LinkedHashMap<String, Object>();
                payload.put("postId", postId);
                payload.put("postTitle", post.getTitle());
                payload.put("actorNickname", u != null ? u.getNickname() : null);
                eventPublisher.publishEvent(new NotificationEvent(
                        recipients, userId, "COMMENT_REPLIED", "COMMENT", c.getParentId(), payload));
            }
        }

        return resp;
    }

    @Override
    @Transactional
    public void deleteComment(Long postId, Long commentId, Long userId) {
        PostComment c = commentMapper.selectById(commentId);
        if (c == null || !c.getPostId().equals(postId)) throw new BusinessException(404, "评论不存在");
        if (!c.getUserId().equals(userId)) throw new BusinessException(403, "无权删除该评论");
        if (c.getStatus() != null && c.getStatus() == 0) return;
        c.setStatus(0);
        c.setDeactivationSource("CREATOR");
        c.setDeactivatedBy(userId);
        c.setDeactivationTime(LocalDateTime.now());
        commentMapper.updateById(c);
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .setSql("comment_count = GREATEST(comment_count - 1, 0)")
                .eq(Post::getId, postId));
    }

    @Override
    @Transactional
    public void like(Long postId, Long userId) {
        Post post = getPost(postId);
        if (post.getStatus() == 0) throw new BusinessException(400, "帖子已停用");

        PostLike existing = likeMapper.selectOne(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, postId).eq(PostLike::getUserId, userId));
        if (existing != null) return;

        try {
            PostLike like = new PostLike();
            like.setPostId(postId);
            like.setUserId(userId);
            likeMapper.insert(like);
        } catch (DuplicateKeyException e) {
            return; // concurrent duplicate, idempotent
        }
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .setSql("like_count = like_count + 1")
                .eq(Post::getId, postId));
    }

    @Override
    @Transactional
    public void unlike(Long postId, Long userId) {
        getPost(postId);

        PostLike existing = likeMapper.selectOne(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, postId).eq(PostLike::getUserId, userId));
        if (existing == null) return;

        int deleted = likeMapper.deleteById(existing.getId());
        if (deleted > 0) {
            postMapper.update(null, new LambdaUpdateWrapper<Post>()
                    .setSql("like_count = GREATEST(like_count - 1, 0)")
                    .eq(Post::getId, postId));
        }
    }

    // ---------- private helpers ----------

    private Post getPost(Long id) {
        Post p = postMapper.selectById(id);
        if (p == null) throw new BusinessException(404, "帖子不存在");
        return p;
    }

    private boolean isAdminUser(Long userId) {
        AppUser u = userMapper.selectById(userId);
        return u != null && u.getIsAdmin() != null && u.getIsAdmin() == 1;
    }

    private Map<Long, AppUser> batchLoadUsers(Set<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return userMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(AppUser::getId, u -> u, (a, b) -> a));
    }

    private Map<Long, Problem> batchLoadProblems(Set<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return problemMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(Problem::getId, p -> p, (a, b) -> a));
    }

    private PostSummaryResponse toSummaryResponse(Post p, Map<Long, AppUser> userMap, Map<Long, Problem> probMap) {
        PostSummaryResponse r = new PostSummaryResponse();
        r.setId(p.getId()); r.setTitle(p.getTitle()); r.setPostType(p.getPostType());
        r.setAuthorUserId(p.getAuthorUserId()); r.setProblemId(p.getProblemId());
        r.setLikeCount(p.getLikeCount() != null ? p.getLikeCount() : 0);
        r.setCommentCount(p.getCommentCount() != null ? p.getCommentCount() : 0);
        r.setViewCount(p.getViewCount() != null ? p.getViewCount() : 0);
        r.setPinned(p.getIsPinned() != null && p.getIsPinned() == 1);
        r.setActive(p.getStatus() != null && p.getStatus() == 1);
        r.setCreateTime(p.getCreateTime());

        AppUser author = userMap.get(p.getAuthorUserId());
        if (author != null) {
            r.setAuthorUsername(author.getUsername());
            r.setAuthorNickname(author.getNickname());
            r.setAuthorAvatarUrl(author.getAvatarUrl());
        }
        if (p.getProblemId() != null) {
            Problem prob = probMap.get(p.getProblemId());
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

        boolean isAdmin = isAdminUser(userId);
        boolean isAuthor = p.getAuthorUserId().equals(userId);

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
        r.setDeactivationSource(p.getDeactivationSource());
        r.setDeactivationReason(p.getDeactivationReason());

        // Load all comments (including deactivated for author/admin visibility)
        LambdaQueryWrapper<PostComment> cqw = new LambdaQueryWrapper<PostComment>()
                .eq(PostComment::getPostId, p.getId())
                .orderByAsc(PostComment::getCreateTime);
        if (!isAuthor && !isAdmin) {
            cqw.eq(PostComment::getStatus, 1);
        }
        List<PostComment> comments = commentMapper.selectList(cqw);

        // Batch load comment users
        Set<Long> userIds = new HashSet<>();
        for (PostComment c : comments) {
            userIds.add(c.getUserId());
            if (c.getReplyToUserId() != null) userIds.add(c.getReplyToUserId());
        }
        Map<Long, AppUser> userMap = batchLoadUsers(userIds);

        Map<Long, List<PostComment>> repliesByParent = new LinkedHashMap<>();
        List<CommentResponse> topLevel = new ArrayList<>();

        for (PostComment c : comments) {
            if (c.getParentId() == null) {
                topLevel.add(toCommentResponse(c, userMap.get(c.getUserId())));
            } else {
                repliesByParent.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c);
            }
        }
        for (CommentResponse cr : topLevel) {
            List<PostComment> reps = repliesByParent.getOrDefault(cr.getId(), Collections.emptyList());
            cr.setReplies(reps.stream().map(rc -> {
                CommentResponse reply = toCommentResponse(rc, userMap.get(rc.getUserId()));
                if (rc.getReplyToUserId() != null) {
                    AppUser replied = userMap.get(rc.getReplyToUserId());
                    if (replied != null) reply.setReplyToUsername(replied.getUsername());
                }
                return reply;
            }).collect(Collectors.toList()));
        }
        r.setComments(topLevel);
        return r;
    }

    private CommentResponse toCommentResponse(PostComment c, AppUser u) {
        CommentResponse r = new CommentResponse();
        r.setId(c.getId()); r.setUserId(c.getUserId()); r.setContent(c.getContent());
        r.setActive(c.getStatus() != null && c.getStatus() == 1);
        r.setCreateTime(c.getCreateTime());
        r.setReplyToUserId(c.getReplyToUserId());
        if (u != null) {
            r.setUsername(u.getUsername()); r.setNickname(u.getNickname()); r.setAvatarUrl(u.getAvatarUrl());
        }
        return r;
    }
}
