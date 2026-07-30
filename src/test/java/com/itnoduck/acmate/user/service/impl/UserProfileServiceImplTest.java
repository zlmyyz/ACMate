package com.itnoduck.acmate.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.oj.entity.OjAccount;
import com.itnoduck.acmate.oj.mapper.OjAccountMapper;
import com.itnoduck.acmate.oj.mapper.OjSubmissionMapper;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import com.itnoduck.acmate.user.dto.PublicUserProfileResponse;
import com.itnoduck.acmate.user.dto.UpdateProfileRequest;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.testutil.MybatisPlusTestHelper;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock
    private AppUserMapper appUserMapper;
    @Mock
    private ProblemMapper problemMapper;
    @Mock
    private OjAccountMapper ojAccountMapper;
    @Mock
    private OjSubmissionMapper ojSubmissionMapper;

    private UserProfileServiceImpl service;

    @BeforeAll
    static void initMybatisPlus() {
        MybatisPlusTestHelper.initEntityTables();
    }

    @BeforeEach
    void setUp() {
        service = new UserProfileServiceImpl(appUserMapper, problemMapper,
                ojAccountMapper, ojSubmissionMapper, "./uploads");
    }

    private AppUser buildUser(Long id, String nickname) {
        AppUser u = new AppUser();
        u.setId(id);
        u.setUsername("user" + id);
        u.setNickname(nickname);
        u.setStatus(1);
        return u;
    }

    // ---------- getProfile ----------

    @Test
    void shouldReturnActiveUserProfile() {
        AppUser user = buildUser(1L, "TestUser");
        user.setBio("Hello");
        user.setAvatarUrl("/uploads/abc.png");
        user.setIsAdmin(0);
        user.setCreateTime(LocalDateTime.of(2026, 1, 1, 0, 0));
        when(appUserMapper.selectById(1L)).thenReturn(user);
        when(problemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);
        when(ojAccountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("solved_count", 10);
        stats.put("solved_30d", 3);
        stats.put("solved_7d", 1);
        when(ojSubmissionMapper.getUserOjStats(eq(1L), any(), any())).thenReturn(stats);

        PublicUserProfileResponse r = service.getProfile(1L);

        assertThat(r.getId()).isEqualTo(1L);
        assertThat(r.getUsername()).isEqualTo("user1");
        assertThat(r.getNickname()).isEqualTo("TestUser");
        assertThat(r.getAvatarUrl()).isEqualTo("/uploads/abc.png");
        assertThat(r.getBio()).isEqualTo("Hello");
        assertThat(r.isAdmin()).isFalse();
        assertThat(r.getAccountStatus()).isEqualTo("ACTIVE");
        assertThat(r.getCreatedProblemCount()).isEqualTo(5L);
        assertThat(r.getCreateTime()).isNotNull();
        assertThat(r.getCodeforcesHandle()).isNull();
        assertThat(r.getOjStats()).isNotNull();
        assertThat(r.getOjStats().getSolvedCount()).isEqualTo(10);
        assertThat(r.getOjStats().getSolvedCount30d()).isEqualTo(3);
        assertThat(r.getOjStats().getSolvedCount7d()).isEqualTo(1);
    }

    @Test
    void shouldReturn404WhenUserNotFound() {
        when(appUserMapper.selectById(999L)).thenReturn(null);
        assertThatThrownBy(() -> service.getProfile(999L))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getCode() == 404);
    }

    @Test
    void shouldReturnDisabledStatusForDisabledUser() {
        AppUser user = buildUser(2L, "Disabled");
        user.setStatus(0);
        when(appUserMapper.selectById(2L)).thenReturn(user);
        when(problemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);
        when(ojAccountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(ojSubmissionMapper.getUserOjStats(eq(2L), any(), any())).thenReturn(null);

        PublicUserProfileResponse r = service.getProfile(2L);

        assertThat(r.getAccountStatus()).isEqualTo("DISABLED");
        assertThat(r.getCreatedProblemCount()).isEqualTo(3L);
        assertThat(r.getOjStats()).isNotNull();
        assertThat(r.getOjStats().getSolvedCount()).isEqualTo(0);
    }

    @Test
    void shouldNotReturnEmail() {
        AppUser user = buildUser(1L, "Test");
        user.setEmail("test@test.com");
        when(appUserMapper.selectById(1L)).thenReturn(user);
        when(problemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(ojAccountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(ojSubmissionMapper.getUserOjStats(eq(1L), any(), any())).thenReturn(null);

        PublicUserProfileResponse r = service.getProfile(1L);
        // email is not a field on PublicUserProfileResponse, verified by compilation
        assertThat(r.getUsername()).isEqualTo("user1");
    }

    @Test
    void shouldShowCodeforcesHandleWhenVerified() {
        AppUser user = buildUser(1L, "Test");
        when(appUserMapper.selectById(1L)).thenReturn(user);
        when(problemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        OjAccount cf = new OjAccount();
        cf.setPlatform("CODEFORCES");
        cf.setExternalUserId("tourist");
        cf.setVerifyStatus(1);
        when(ojAccountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(cf);
        when(ojSubmissionMapper.getUserOjStats(eq(1L), any(), any())).thenReturn(null);

        PublicUserProfileResponse r = service.getProfile(1L);
        assertThat(r.getCodeforcesHandle()).isEqualTo("tourist");
    }

    @Test
    void shouldNotShowCodeforcesHandleWhenPending() {
        AppUser user = buildUser(1L, "Test");
        when(appUserMapper.selectById(1L)).thenReturn(user);
        when(problemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(ojAccountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(ojSubmissionMapper.getUserOjStats(eq(1L), any(), any())).thenReturn(null);

        PublicUserProfileResponse r = service.getProfile(1L);
        assertThat(r.getCodeforcesHandle()).isNull();
    }

    @Test
    void shouldReturnEmptyStatsWhenNoAcData() {
        AppUser user = buildUser(1L, "Test");
        when(appUserMapper.selectById(1L)).thenReturn(user);
        when(problemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(ojAccountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(ojSubmissionMapper.getUserOjStats(eq(1L), any(), any())).thenReturn(null);

        PublicUserProfileResponse r = service.getProfile(1L);
        assertThat(r.getOjStats()).isNotNull();
        assertThat(r.getOjStats().getSolvedCount()).isEqualTo(0);
        assertThat(r.getOjStats().getLastAcceptedTime()).isNull();
    }

    @Test
    void shouldStillReturnOjStatsForDisabledUser() {
        AppUser user = buildUser(1L, "Test");
        user.setStatus(0);
        when(appUserMapper.selectById(1L)).thenReturn(user);
        when(problemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(ojAccountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("solved_count", 42);
        stats.put("solved_30d", 5);
        stats.put("solved_7d", 0);
        when(ojSubmissionMapper.getUserOjStats(eq(1L), any(), any())).thenReturn(stats);

        PublicUserProfileResponse r = service.getProfile(1L);
        assertThat(r.getAccountStatus()).isEqualTo("DISABLED");
        assertThat(r.getOjStats().getSolvedCount()).isEqualTo(42);
    }

    // ---------- updateProfile ----------

    @Test
    void shouldAllowKeepingOwnNickname() {
        AppUser current = buildUser(1L, "MyNick");
        when(appUserMapper.selectById(1L)).thenReturn(current);
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setNickname("MyNick");
        when(appUserMapper.update(any(), any())).thenReturn(1);
        service.updateProfile(1L, req);
        verify(appUserMapper, never()).selectCount(ArgumentMatchers.<LambdaQueryWrapper<AppUser>>any());
    }

    @Test
    void shouldAllowChangingToUniqueNickname() {
        AppUser current = buildUser(1L, "OldNick");
        when(appUserMapper.selectById(1L)).thenReturn(current);
        when(appUserMapper.selectCount(ArgumentMatchers.<LambdaQueryWrapper<AppUser>>any())).thenReturn(0L);
        when(appUserMapper.update(any(), any())).thenReturn(1);
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setNickname("NewNick");
        service.updateProfile(1L, req);
    }

    @Test
    void shouldRejectNicknameAlreadyInUse() {
        AppUser current = buildUser(1L, "OldNick");
        when(appUserMapper.selectById(1L)).thenReturn(current);
        when(appUserMapper.selectCount(ArgumentMatchers.<LambdaQueryWrapper<AppUser>>any())).thenReturn(1L);
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setNickname("Taken");
        assertThatThrownBy(() -> service.updateProfile(1L, req))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getCode() == 409 && e.getMessage().contains("昵称"));
    }

    @Test
    void shouldRejectEmptyNicknameAfterTrim() {
        AppUser current = buildUser(1L, "OldNick");
        when(appUserMapper.selectById(1L)).thenReturn(current);
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setNickname("   ");
        assertThatThrownBy(() -> service.updateProfile(1L, req))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getCode() == 400 && e.getMessage().contains("不能为空"));
    }

    @Test
    void shouldHandleConcurrentNicknameDupViaUniqueConstraint() {
        AppUser current = buildUser(1L, "OldNick");
        when(appUserMapper.selectById(1L)).thenReturn(current);
        when(appUserMapper.selectCount(ArgumentMatchers.<LambdaQueryWrapper<AppUser>>any())).thenReturn(0L);
        var sqlEx = new SQLIntegrityConstraintViolationException(
                "Duplicate entry 'NewNick' for key 'app_user.uk_app_user_nickname'");
        when(appUserMapper.update(any(), any())).thenThrow(new DuplicateKeyException("dup", sqlEx));
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setNickname("NewNick");
        assertThatThrownBy(() -> service.updateProfile(1L, req))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getCode() == 409 && e.getMessage().contains("昵称"));
    }
}
