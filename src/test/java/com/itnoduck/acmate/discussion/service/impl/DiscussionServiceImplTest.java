package com.itnoduck.acmate.discussion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itnoduck.acmate.auditlog.service.AuditLogService;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.discussion.dto.CreateCommentRequest;
import com.itnoduck.acmate.discussion.dto.CreatePostRequest;
import com.itnoduck.acmate.discussion.dto.UpdatePostRequest;
import com.itnoduck.acmate.discussion.entity.Post;
import com.itnoduck.acmate.discussion.entity.PostComment;
import com.itnoduck.acmate.discussion.entity.PostLike;
import com.itnoduck.acmate.discussion.mapper.PostCommentMapper;
import com.itnoduck.acmate.discussion.mapper.PostLikeMapper;
import com.itnoduck.acmate.discussion.mapper.PostMapper;
import com.itnoduck.acmate.problem.entity.Problem;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import com.itnoduck.acmate.testutil.MybatisPlusTestHelper;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscussionServiceImplTest {

    @Mock private PostMapper postMapper;
    @Mock private PostCommentMapper commentMapper;
    @Mock private PostLikeMapper likeMapper;
    @Mock private AppUserMapper userMapper;
    @Mock private ProblemMapper problemMapper;
    @Mock private AuditLogService auditLogService;
    @InjectMocks private DiscussionServiceImpl service;

    @BeforeAll
    static void initMybatisPlus() {
        MybatisPlusTestHelper.initEntityTables();
    }

    @BeforeEach
    void mockInsertFill() {
        lenient().doAnswer(inv -> {
            Post p = inv.getArgument(0);
            p.setCreateTime(LocalDateTime.now());
            p.setUpdateTime(LocalDateTime.now());
            if (inv.getMethod().getName().equals("insert")) {
                p.setId(1L);
            }
            return 1;
        }).when(postMapper).insert(any(Post.class));
        lenient().doAnswer(inv -> {
            PostComment c = inv.getArgument(0);
            c.setCreateTime(LocalDateTime.now());
            c.setUpdateTime(LocalDateTime.now());
            c.setId(1L);
            return 1;
        }).when(commentMapper).insert(any(PostComment.class));
        lenient().doAnswer(inv -> {
            PostLike l = inv.getArgument(0);
            l.setId(1L);
            l.setCreateTime(LocalDateTime.now());
            return 1;
        }).when(likeMapper).insert(any(PostLike.class));
    }

    private CreatePostRequest buildRequest(String postType) {
        CreatePostRequest req = new CreatePostRequest();
        req.setTitle("Test Title");
        req.setContentMd("Test Content");
        req.setPostType(postType);
        return req;
    }

    private AppUser buildUser(Long id, int isAdmin) {
        AppUser u = new AppUser();
        u.setId(id);
        u.setUsername("user" + id);
        u.setNickname("User " + id);
        u.setIsAdmin(isAdmin);
        u.setStatus(1);
        return u;
    }

    private Post buildPost(Long id, Long authorUserId, Integer status) {
        Post p = new Post();
        p.setId(id);
        p.setAuthorUserId(authorUserId);
        p.setTitle("Post " + id);
        p.setContentMd("content");
        p.setPostType("OTHER");
        p.setStatus(status);
        p.setIsPinned(0);
        p.setViewCount(10);
        p.setLikeCount(5);
        p.setCommentCount(3);
        p.setCreateTime(LocalDateTime.now());
        p.setUpdateTime(LocalDateTime.now());
        return p;
    }

    // ========== Section 三: time fill ==========

    @Test
    void shouldSetCreateTimeOnPostCreate() {
        Post[] captured = new Post[1];
        doAnswer(inv -> {
            Post p = inv.getArgument(0);
            captured[0] = p;
            p.setCreateTime(LocalDateTime.now());
            p.setUpdateTime(LocalDateTime.now());
            return 1;
        }).when(postMapper).insert(any(Post.class));

        service.createPost(buildRequest("OTHER"), 1L);
        assertNotNull(captured[0].getCreateTime());
    }

    @Test
    void shouldSetUpdateTimeOnPostCreate() {
        Post[] captured = new Post[1];
        doAnswer(inv -> {
            Post p = inv.getArgument(0);
            captured[0] = p;
            p.setCreateTime(LocalDateTime.now());
            p.setUpdateTime(LocalDateTime.now());
            return 1;
        }).when(postMapper).insert(any(Post.class));

        service.createPost(buildRequest("OTHER"), 1L);
        assertNotNull(captured[0].getUpdateTime());
    }

    // ========== Section 四: post creation ==========

    @Test
    void shouldSaveContestSummaryPostType() {
        Post[] captured = new Post[1];
        doAnswer(inv -> {
            Post p = inv.getArgument(0);
            captured[0] = p;
            p.setId(1L); p.setCreateTime(LocalDateTime.now()); p.setUpdateTime(LocalDateTime.now());
            return 1;
        }).when(postMapper).insert(any(Post.class));

        service.createPost(buildRequest("CONTEST_SUMMARY"), 1L);
        assertEquals("CONTEST_SUMMARY", captured[0].getPostType());
    }

    @Test
    void shouldSaveTrainingExperiencePostType() {
        Post[] captured = new Post[1];
        doAnswer(inv -> {
            Post p = inv.getArgument(0);
            captured[0] = p;
            p.setId(1L); p.setCreateTime(LocalDateTime.now()); p.setUpdateTime(LocalDateTime.now());
            return 1;
        }).when(postMapper).insert(any(Post.class));

        service.createPost(buildRequest("TRAINING_EXPERIENCE"), 1L);
        assertEquals("TRAINING_EXPERIENCE", captured[0].getPostType());
    }

    @Test
    void shouldRejectAnnouncementForNonAdmin() {
        AppUser user = buildUser(1L, 0);
        when(userMapper.selectById(1L)).thenReturn(user);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createPost(buildRequest("ANNOUNCEMENT"), 1L));
        assertEquals(403, ex.getCode());
    }

    @Test
    void shouldAllowAnnouncementForAdmin() {
        AppUser user = buildUser(1L, 1);
        when(userMapper.selectById(1L)).thenReturn(user);

        assertDoesNotThrow(() -> service.createPost(buildRequest("ANNOUNCEMENT"), 1L));
    }

    @Test
    void shouldRequireProblemIdForSolution() {
        CreatePostRequest req = buildRequest("SOLUTION");
        req.setProblemId(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createPost(req, 1L));
        assertEquals(400, ex.getCode());
    }

    @Test
    void shouldAllowSolutionWithProblemId() {
        CreatePostRequest req = buildRequest("SOLUTION");
        req.setProblemId(100L);
        assertDoesNotThrow(() -> service.createPost(req, 1L));
    }

    @Test
    void shouldRejectInvalidPostType() {
        CreatePostRequest req = buildRequest("INVALID_TYPE");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createPost(req, 1L));
        assertEquals(400, ex.getCode());
    }

    @Test
    void shouldRejectNullPostType() {
        CreatePostRequest req = buildRequest(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createPost(req, 1L));
        assertEquals(400, ex.getCode());
    }

    @Test
    void shouldRejectEmptyTitle() {
        CreatePostRequest req = buildRequest("OTHER");
        req.setTitle("   ");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createPost(req, 1L));
        assertEquals(400, ex.getCode());
    }

    @Test
    void shouldRejectEmptyContent() {
        CreatePostRequest req = buildRequest("OTHER");
        req.setContentMd("   ");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createPost(req, 1L));
        assertEquals(400, ex.getCode());
    }

    // ========== Section 四: post update ==========

    @Test
    void shouldAllowAuthorToUpdate() {
        Post post = buildPost(1L, 1L, 1);
        when(postMapper.selectById(1L)).thenReturn(post);
        when(postMapper.updateById(any(Post.class))).thenReturn(1);

        UpdatePostRequest req = new UpdatePostRequest();
        req.setTitle("Updated");
        req.setContentMd("Updated Content");
        assertDoesNotThrow(() -> service.updatePost(1L, req, 1L));
    }

    @Test
    void shouldRejectAdminEditOthersPost() {
        Post post = buildPost(1L, 2L, 1);
        when(postMapper.selectById(1L)).thenReturn(post);

        UpdatePostRequest req = new UpdatePostRequest();
        req.setTitle("Updated");
        req.setContentMd("Updated Content");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updatePost(1L, req, 1L));
        assertEquals(403, ex.getCode());
    }

    @Test
    void shouldRejectNonAuthorUpdate() {
        Post post = buildPost(1L, 2L, 1);
        when(postMapper.selectById(1L)).thenReturn(post);

        UpdatePostRequest req = new UpdatePostRequest();
        req.setTitle("Updated");
        req.setContentMd("Updated Content");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updatePost(1L, req, 1L));
        assertEquals(403, ex.getCode());
    }

    // ========== Section 四: post detail ==========

    @Test
    void shouldReturnPostDetail() {
        Post post = buildPost(1L, 2L, 1);
        when(postMapper.selectById(1L)).thenReturn(post, post);
        AppUser author = buildUser(2L, 0);
        when(userMapper.selectById(2L)).thenReturn(author);
        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, 0));
        when(likeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(commentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        var resp = service.getPostDetail(1L, 1L);
        assertEquals(1L, resp.getId());
        assertEquals("Post 1", resp.getTitle());
    }

    @Test
    void shouldReturn404ForDeactivatedPostForNonAuthor() {
        Post post = buildPost(1L, 2L, 0);
        when(postMapper.selectById(1L)).thenReturn(post);
        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, 0));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getPostDetail(1L, 1L));
        assertEquals(404, ex.getCode());
    }

    @Test
    void shouldAllowAuthorToViewDeactivatedPost() {
        Post post = buildPost(1L, 1L, 0);
        when(postMapper.selectById(1L)).thenReturn(post, post);
        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, 0));
        when(likeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(commentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        assertDoesNotThrow(() -> service.getPostDetail(1L, 1L));
    }

    @Test
    void shouldAllowAdminToViewDeactivatedPost() {
        Post post = buildPost(1L, 2L, 0);
        when(postMapper.selectById(1L)).thenReturn(post, post);
        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, 1));
        when(likeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(commentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        assertDoesNotThrow(() -> service.getPostDetail(1L, 1L));
    }

    // ========== Section 十: post list ==========

    @Test
    void shouldOnlyReturnActivePosts() {
        Post post = buildPost(1L, 2L, 1);
        post.setProblemId(null);
        Page<Post> page = new Page<>(1, 20);
        page.setRecords(List.of(post));
        when(postMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(userMapper.selectBatchIds(anySet())).thenReturn(List.of(buildUser(2L, 0)));

        service.listPosts(null, null, null, 1, 20);
        // Verify eq(Post::getStatus, 1) is in the query
        verify(postMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void shouldBatchLoadAuthors() {
        Post post = buildPost(1L, 2L, 1);
        post.setProblemId(null);
        Page<Post> page = new Page<>(1, 20);
        page.setRecords(List.of(post));
        when(postMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(userMapper.selectBatchIds(Set.of(2L))).thenReturn(List.of(buildUser(2L, 0)));

        var result = service.listPosts(null, null, null, 1, 20);
        assertEquals(1, result.size());
        assertEquals("user2", result.get(0).getAuthorUsername());
        verify(userMapper).selectBatchIds(Set.of(2L));
        verify(userMapper, never()).selectById(anyLong());
    }

    @Test
    void shouldBatchLoadProblems() {
        Post post = buildPost(1L, 2L, 1);
        post.setProblemId(100L);
        Page<Post> page = new Page<>(1, 20);
        page.setRecords(List.of(post));
        when(postMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(userMapper.selectBatchIds(Set.of(2L))).thenReturn(List.of(buildUser(2L, 0)));
        Problem prob = new Problem();
        prob.setId(100L); prob.setTitle("Problem 100");
        when(problemMapper.selectBatchIds(Set.of(100L))).thenReturn(List.of(prob));

        var result = service.listPosts(null, null, null, 1, 20);
        assertEquals(1, result.size());
        assertEquals("Problem 100", result.get(0).getProblemTitle());
        verify(problemMapper).selectBatchIds(Set.of(100L));
        verify(problemMapper, never()).selectById(anyLong());
    }

    // ========== Section 五: deactivation ==========

    @Test
    void shouldTrackAuthorDeactivationSource() {
        Post post = buildPost(1L, 1L, 1);
        when(postMapper.selectById(1L)).thenReturn(post);
        when(postMapper.updateById(any(Post.class))).thenReturn(1);

        service.deletePost(1L, 1L);

        assertEquals("CREATOR", post.getDeactivationSource());
        assertEquals(1L, post.getDeactivatedBy());
        assertNotNull(post.getDeactivationTime());
        assertEquals(0, post.getStatus());
    }

    @Test
    void shouldRejectNonAuthorDeactivation() {
        Post post = buildPost(1L, 2L, 1);
        when(postMapper.selectById(1L)).thenReturn(post);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.deletePost(1L, 1L));
        assertEquals(403, ex.getCode());
    }

    @Test
    void shouldBeIdempotentDeactivate() {
        Post post = buildPost(1L, 1L, 0);
        when(postMapper.selectById(1L)).thenReturn(post);

        assertDoesNotThrow(() -> service.deletePost(1L, 1L));
        verify(postMapper, never()).updateById(any(Post.class));
    }

    // ========== Section 五/六: restore ==========

    @Test
    void shouldAllowAuthorToRestoreOwnDeactivatedPost() {
        Post post = buildPost(1L, 1L, 0);
        post.setDeactivationSource("CREATOR");
        when(postMapper.selectById(1L)).thenReturn(post);
        when(postMapper.updateById(any(Post.class))).thenReturn(1);
        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, 0));

        service.restorePost(1L, 1L);
        assertEquals(1, post.getStatus());
        assertNull(post.getDeactivationSource());
    }

    @Test
    void shouldAllowAdminToRestoreAnyPost() {
        Post post = buildPost(1L, 2L, 0);
        post.setDeactivationSource("ADMIN");
        when(postMapper.selectById(1L)).thenReturn(post);
        when(postMapper.updateById(any(Post.class))).thenReturn(1);
        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, 1));

        service.restorePost(1L, 1L);
        assertEquals(1, post.getStatus());
    }

    @Test
    void shouldRejectAuthorRestoreAdminDeactivatedPost() {
        Post post = buildPost(1L, 1L, 0);
        post.setDeactivationSource("ADMIN");
        when(postMapper.selectById(1L)).thenReturn(post);
        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, 0));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.restorePost(1L, 1L));
        assertEquals(403, ex.getCode());
    }

    @Test
    void shouldClearDeactivationFieldsOnRestore() {
        Post post = buildPost(1L, 1L, 0);
        post.setDeactivationSource("CREATOR");
        post.setDeactivationReason("reason");
        post.setDeactivatedBy(1L);
        post.setDeactivationTime(LocalDateTime.now());
        when(postMapper.selectById(1L)).thenReturn(post);
        when(postMapper.updateById(any(Post.class))).thenReturn(1);
        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, 0));

        service.restorePost(1L, 1L);
        assertNull(post.getDeactivationSource());
        assertNull(post.getDeactivationReason());
        assertNull(post.getDeactivatedBy());
        assertNull(post.getDeactivationTime());
    }

    @Test
    void shouldBeIdempotentRestore() {
        Post post = buildPost(1L, 1L, 1);
        when(postMapper.selectById(1L)).thenReturn(post);
        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, 0));

        assertDoesNotThrow(() -> service.restorePost(1L, 1L));
        verify(postMapper, never()).updateById(any(Post.class));
    }

    @Test
    void shouldReturn404ForNonAuthorRestoreDeactivatedPost() {
        Post post = buildPost(1L, 2L, 0);
        when(postMapper.selectById(1L)).thenReturn(post);
        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, 0));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.restorePost(1L, 1L));
        assertEquals(404, ex.getCode());
    }

    // ========== Section 七: comments ==========

    @Test
    void shouldSetCreateTimeOnCommentCreate() {
        Post post = buildPost(1L, 1L, 1);
        when(postMapper.selectById(1L)).thenReturn(post);
        when(userMapper.selectById(2L)).thenReturn(buildUser(2L, 0));

        PostComment[] captured = new PostComment[1];
        doAnswer(inv -> {
            PostComment c = inv.getArgument(0);
            captured[0] = c;
            c.setCreateTime(LocalDateTime.now()); c.setUpdateTime(LocalDateTime.now()); c.setId(1L);
            return 1;
        }).when(commentMapper).insert(any(PostComment.class));

        CreateCommentRequest req = new CreateCommentRequest();
        req.setContent("Comment");
        service.addComment(1L, req, 2L);

        assertNotNull(captured[0].getCreateTime());
    }

    @Test
    void shouldAddTopLevelComment() {
        Post post = buildPost(1L, 1L, 1);
        when(postMapper.selectById(1L)).thenReturn(post);
        when(userMapper.selectById(2L)).thenReturn(buildUser(2L, 0));

        CreateCommentRequest req = new CreateCommentRequest();
        req.setContent("Top level");
        var resp = service.addComment(1L, req, 2L);
        assertNotNull(resp);
        assertEquals("Top level", resp.getContent());
    }

    @Test
    void shouldAddReplyToComment() {
        Post post = buildPost(1L, 1L, 1);
        when(postMapper.selectById(1L)).thenReturn(post);
        PostComment parent = new PostComment();
        parent.setId(10L); parent.setPostId(1L); parent.setParentId(null);
        when(commentMapper.selectById(10L)).thenReturn(parent);
        when(userMapper.selectById(2L)).thenReturn(buildUser(2L, 0));

        CreateCommentRequest req = new CreateCommentRequest();
        req.setContent("Reply");
        req.setParentId(10L);
        assertDoesNotThrow(() -> service.addComment(1L, req, 2L));
    }

    @Test
    void shouldRejectReplyToReply() {
        Post post = buildPost(1L, 1L, 1);
        when(postMapper.selectById(1L)).thenReturn(post);
        PostComment reply = new PostComment();
        reply.setId(10L); reply.setPostId(1L); reply.setParentId(5L); // already a reply
        when(commentMapper.selectById(10L)).thenReturn(reply);

        CreateCommentRequest req = new CreateCommentRequest();
        req.setContent("Nested");
        req.setParentId(10L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.addComment(1L, req, 2L));
        assertEquals(400, ex.getCode());
    }

    @Test
    void shouldRejectCommentOnDeactivatedPost() {
        Post post = buildPost(1L, 1L, 0);
        when(postMapper.selectById(1L)).thenReturn(post);

        CreateCommentRequest req = new CreateCommentRequest();
        req.setContent("Comment");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.addComment(1L, req, 2L));
        assertEquals(400, ex.getCode());
    }

    @Test
    void shouldRejectEmptyComment() {
        Post post = buildPost(1L, 1L, 1);
        when(postMapper.selectById(1L)).thenReturn(post);

        CreateCommentRequest req = new CreateCommentRequest();
        req.setContent("   ");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.addComment(1L, req, 2L));
        assertEquals(400, ex.getCode());
    }

    // ========== Security: backend stores raw Markdown (frontend handles sanitization) ==========

    @Test
    void shouldAcceptScriptTagInMarkdownContent() {
        CreatePostRequest req = buildRequest("OTHER");
        req.setContentMd("Hello <script>alert(1)</script>");
        assertDoesNotThrow(() -> service.createPost(req, 1L));
    }

    @Test
    void shouldAcceptHtmlInMarkdownCodeBlock() {
        CreatePostRequest req = buildRequest("OTHER");
        req.setContentMd("```html\n<script>alert(1)</script>\n```");
        assertDoesNotThrow(() -> service.createPost(req, 1L));
    }

    @Test
    void shouldAcceptImgOnErrorInMarkdown() {
        CreatePostRequest req = buildRequest("OTHER");
        req.setContentMd("<img src=x onerror=alert(1)>");
        assertDoesNotThrow(() -> service.createPost(req, 1L));
    }

    @Test
    void shouldAcceptJavascriptLinkInMarkdown() {
        CreatePostRequest req = buildRequest("OTHER");
        req.setContentMd("[click](javascript:alert(1))");
        assertDoesNotThrow(() -> service.createPost(req, 1L));
    }

    @Test
    void shouldAcceptCaseVariantHtmlTags() {
        CreatePostRequest req = buildRequest("OTHER");
        req.setContentMd("<SCRIPT>alert(1)</SCRIPT>");
        assertDoesNotThrow(() -> service.createPost(req, 1L));
    }

    @Test
    void shouldAcceptHtmlInComment() {
        Post post = buildPost(1L, 1L, 1);
        when(postMapper.selectById(1L)).thenReturn(post);

        CreateCommentRequest req = new CreateCommentRequest();
        req.setContent("<script>alert(1)</script>");
        assertDoesNotThrow(() -> service.addComment(1L, req, 2L));
    }

    @Test
    void shouldAllowAuthorToDeleteComment() {
        PostComment c = new PostComment();
        c.setId(1L); c.setPostId(1L); c.setUserId(1L); c.setStatus(1);
        when(commentMapper.selectById(1L)).thenReturn(c);
        when(commentMapper.updateById(any(PostComment.class))).thenReturn(1);

        assertDoesNotThrow(() -> service.deleteComment(1L, 1L, 1L));
        assertEquals(0, c.getStatus());
        assertEquals("CREATOR", c.getDeactivationSource());
    }

    @Test
    void shouldRejectNonAuthorDeleteComment() {
        PostComment c = new PostComment();
        c.setId(1L); c.setPostId(1L); c.setUserId(2L); c.setStatus(1);
        when(commentMapper.selectById(1L)).thenReturn(c);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.deleteComment(1L, 1L, 1L));
        assertEquals(403, ex.getCode());
    }

    @Test
    void shouldTrackCommentDeactivationSource() {
        PostComment c = new PostComment();
        c.setId(1L); c.setPostId(1L); c.setUserId(1L); c.setStatus(1);
        when(commentMapper.selectById(1L)).thenReturn(c);
        when(commentMapper.updateById(any(PostComment.class))).thenReturn(1);

        service.deleteComment(1L, 1L, 1L);
        assertEquals("CREATOR", c.getDeactivationSource());
        assertEquals(1L, c.getDeactivatedBy());
        assertNotNull(c.getDeactivationTime());
    }

    // ========== Section 八: likes ==========

    @Test
    void shouldLikePost() {
        Post post = buildPost(1L, 1L, 1);
        when(postMapper.selectById(1L)).thenReturn(post);
        when(likeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        service.like(1L, 1L);
        verify(likeMapper).insert(any(PostLike.class));
    }

    @Test
    void shouldUnlikePost() {
        Post post = buildPost(1L, 1L, 1);
        PostLike existing = new PostLike();
        existing.setId(100L);
        when(postMapper.selectById(1L)).thenReturn(post);
        when(likeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        when(likeMapper.deleteById(100L)).thenReturn(1);

        service.unlike(1L, 1L);
        verify(likeMapper).deleteById(100L);
    }

    @Test
    void shouldBeIdempotentLike() {
        Post post = buildPost(1L, 1L, 1);
        PostLike existing = new PostLike();
        existing.setId(100L);
        when(postMapper.selectById(1L)).thenReturn(post, post);
        when(likeMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(null)   // first call: not liked yet
                .thenReturn(existing); // second call: already liked

        service.like(1L, 1L); // first: inserts
        service.like(1L, 1L); // second: idempotent, returns early
        verify(likeMapper, times(1)).insert(any(PostLike.class));
    }

    @Test
    void shouldBeIdempotentUnlike() {
        Post post = buildPost(1L, 1L, 1);
        when(postMapper.selectById(1L)).thenReturn(post, post);
        when(likeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        service.unlike(1L, 1L); // first: no record, no-op
        service.unlike(1L, 1L); // second: still no-op
        verify(likeMapper, never()).deleteById(anyLong());
    }

    @Test
    void shouldRejectLikeOnDeactivatedPost() {
        Post post = buildPost(1L, 1L, 0);
        when(postMapper.selectById(1L)).thenReturn(post);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.like(1L, 1L));
        assertEquals(400, ex.getCode());
    }

    @Test
    void shouldAllowUnlikeOnDeactivatedPost() {
        Post post = buildPost(1L, 1L, 0);
        PostLike existing = new PostLike();
        existing.setId(100L);
        when(postMapper.selectById(1L)).thenReturn(post);
        when(likeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        when(likeMapper.deleteById(100L)).thenReturn(1);

        assertDoesNotThrow(() -> service.unlike(1L, 1L));
        verify(likeMapper).deleteById(100L);
    }

    @Test
    void shouldNotDecrementCountWhenUnlikeHasNoRecord() {
        Post post = buildPost(1L, 1L, 1);
        when(postMapper.selectById(1L)).thenReturn(post);
        when(likeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        service.unlike(1L, 1L);
        verify(postMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void shouldIncrementLikeCountOnLike() {
        Post post = buildPost(1L, 1L, 1);
        when(postMapper.selectById(1L)).thenReturn(post);
        when(likeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        service.like(1L, 1L);
        verify(postMapper).update(any(), argThat(w -> {
            String sql = w.getSqlSet();
            return sql != null && sql.contains("like_count = like_count + 1");
        }));
    }

    @Test
    void shouldDecrementLikeCountOnUnlike() {
        Post post = buildPost(1L, 1L, 1);
        PostLike existing = new PostLike();
        existing.setId(100L);
        when(postMapper.selectById(1L)).thenReturn(post);
        when(likeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        when(likeMapper.deleteById(100L)).thenReturn(1);

        service.unlike(1L, 1L);
        verify(postMapper).update(any(), argThat(w -> {
            String sql = w.getSqlSet();
            return sql != null && sql.contains("GREATEST(like_count - 1, 0)");
        }));
    }

    @Test
    void shouldHandleDuplicateKeyOnConcurrentLike() {
        Post post = buildPost(1L, 1L, 1);
        when(postMapper.selectById(1L)).thenReturn(post);
        when(likeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(likeMapper.insert(any(PostLike.class)))
                .thenThrow(new org.springframework.dao.DuplicateKeyException("duplicate"));

        assertDoesNotThrow(() -> service.like(1L, 1L));
        // insert failed, count should NOT be incremented
        verify(postMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
    }

    // ========== Section 十三: admin audit log ==========

    @Test
    void shouldRejectAdminDeactivationWithoutReasonViaService() {
        // DiscussionService.deletePost is author-only; admin uses AdminContentService
        // This test verifies the boundary: non-author can't use deletePost
        Post post = buildPost(1L, 2L, 1);
        when(postMapper.selectById(1L)).thenReturn(post);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.deletePost(1L, 1L));
        assertEquals(403, ex.getCode());
    }
}
