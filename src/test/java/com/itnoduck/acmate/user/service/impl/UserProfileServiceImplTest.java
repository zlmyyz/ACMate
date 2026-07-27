package com.itnoduck.acmate.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import com.itnoduck.acmate.user.dto.UpdateProfileRequest;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.sql.SQLIntegrityConstraintViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock
    private AppUserMapper appUserMapper;

    @Mock
    private ProblemMapper problemMapper;

    private UserProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserProfileServiceImpl(appUserMapper, problemMapper, "./uploads");
    }

    private AppUser buildUser(Long id, String nickname) {
        AppUser u = new AppUser();
        u.setId(id);
        u.setUsername("user" + id);
        u.setNickname(nickname);
        u.setStatus(1);
        return u;
    }

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
