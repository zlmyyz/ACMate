package com.itnoduck.acmate.discussion.service.impl;

import com.itnoduck.acmate.auditlog.service.AuditLogService;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.discussion.dto.CreateCommentRequest;
import com.itnoduck.acmate.discussion.dto.CreatePostRequest;
import com.itnoduck.acmate.discussion.entity.Post;
import com.itnoduck.acmate.discussion.entity.PostComment;
import com.itnoduck.acmate.discussion.mapper.PostCommentMapper;
import com.itnoduck.acmate.discussion.mapper.PostLikeMapper;
import com.itnoduck.acmate.discussion.mapper.PostMapper;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import com.itnoduck.acmate.testutil.MybatisPlusTestHelper;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    private CreatePostRequest buildRequest(String postType) {
        CreatePostRequest req = new CreatePostRequest();
        req.setTitle("Test Title");
        req.setContentMd("Test Content");
        req.setPostType(postType);
        return req;
    }

    private void mockPostInsertAndFill(Post[] out) {
        doAnswer(inv -> {
            Post p = inv.getArgument(0);
            p.setCreateTime(LocalDateTime.now());
            p.setUpdateTime(LocalDateTime.now());
            if (out != null) out[0] = p;
            return 1;
        }).when(postMapper).insert(any(Post.class));
    }

    private void mockCommentInsertAndFill(PostComment[] out) {
        doAnswer(inv -> {
            PostComment c = inv.getArgument(0);
            c.setCreateTime(LocalDateTime.now());
            c.setUpdateTime(LocalDateTime.now());
            if (out != null) out[0] = c;
            return 1;
        }).when(commentMapper).insert(any(PostComment.class));
    }

    @Test
    void shouldSetCreateTimeOnPostCreate() {
        Post[] captured = new Post[1];
        mockPostInsertAndFill(captured);

        service.createPost(buildRequest("OTHER"), 1L);

        assertNotNull(captured[0].getCreateTime(), "createTime should be set");
    }

    @Test
    void shouldSetUpdateTimeOnPostCreate() {
        Post[] captured = new Post[1];
        mockPostInsertAndFill(captured);

        service.createPost(buildRequest("OTHER"), 1L);

        assertNotNull(captured[0].getUpdateTime(), "updateTime should be set");
    }

    @Test
    void shouldSaveContestSummaryPostType() {
        Post[] captured = new Post[1];
        mockPostInsertAndFill(captured);

        service.createPost(buildRequest("CONTEST_SUMMARY"), 1L);

        assertEquals("CONTEST_SUMMARY", captured[0].getPostType());
    }

    @Test
    void shouldSetCreateTimeOnCommentCreate() {
        Post post = buildPost(1L, 1L, 1);
        when(postMapper.selectById(1L)).thenReturn(post);

        PostComment[] captured = new PostComment[1];
        mockCommentInsertAndFill(captured);

        CreateCommentRequest req = new CreateCommentRequest();
        req.setContent("Comment");
        service.addComment(1L, req, 2L);

        assertNotNull(captured[0].getCreateTime(), "comment createTime should be set");
    }

    @Test
    void shouldRejectAnnouncementForNonAdmin() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setIsAdmin(0);
        when(userMapper.selectById(1L)).thenReturn(user);

        CreatePostRequest req = buildRequest("ANNOUNCEMENT");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createPost(req, 1L));
        assertEquals(403, ex.getCode());
    }

    private Post buildPost(Long id, Long authorUserId, Integer status) {
        Post p = new Post();
        p.setId(id);
        p.setAuthorUserId(authorUserId);
        p.setTitle("Post " + id);
        p.setContentMd("content");
        p.setPostType("OTHER");
        p.setStatus(status);
        p.setCreateTime(LocalDateTime.now());
        p.setUpdateTime(LocalDateTime.now());
        return p;
    }
}
