package com.itnoduck.acmate.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.user.dto.RegisterRequest;
import com.itnoduck.acmate.user.dto.RegisterResponse;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceImplTest {

    @Mock
    private AppUserMapper appUserMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private UserRegistrationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserRegistrationServiceImpl(appUserMapper, passwordEncoder);
    }

    @Test
    void shouldRegisterSuccessfully() {
        RegisterRequest req = buildRequest("testuser", "password123", "Test User", "test@example.com");
        when(appUserMapper.selectCount(ArgumentMatchers.<LambdaQueryWrapper<AppUser>>any())).thenReturn(0L);
        when(appUserMapper.insert(ArgumentMatchers.<AppUser>any())).thenAnswer(inv -> {
            AppUser u = inv.getArgument(0);
            u.setId(1L);
            return 1;
        });

        RegisterResponse resp = service.register(req);

        assertThat(resp.id()).isEqualTo(1L);
        assertThat(resp.username()).isEqualTo("testuser");
        assertThat(resp.nickname()).isEqualTo("Test User");
        assertThat(resp.email()).isEqualTo("test@example.com");

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserMapper).insert(captor.capture());
        AppUser saved = captor.getValue();
        assertThat(saved.getIsAdmin()).isEqualTo(0);
        assertThat(saved.getStatus()).isEqualTo(1);
        assertThat(saved.getPasswordHash()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", saved.getPasswordHash())).isTrue();
    }

    @Test
    void shouldThrow409WhenUsernameExists() {
        RegisterRequest req = buildRequest("testuser", "password123", "Test User", "test@example.com");
        when(appUserMapper.selectCount(ArgumentMatchers.<LambdaQueryWrapper<AppUser>>any())).thenReturn(1L);

        assertThatThrownBy(() -> service.register(req))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getCode() == 409 && e.getMessage().contains("用户名"));

        verify(appUserMapper, never()).insert(ArgumentMatchers.<AppUser>any());
    }

    @Test
    void shouldThrow409WhenNicknameExists() {
        RegisterRequest req = buildRequest("testuser", "password123", "Taken", "test@example.com");
        when(appUserMapper.selectCount(ArgumentMatchers.<LambdaQueryWrapper<AppUser>>any()))
                .thenReturn(0L).thenReturn(1L);

        assertThatThrownBy(() -> service.register(req))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getCode() == 409 && e.getMessage().contains("昵称"));

        verify(appUserMapper, never()).insert(ArgumentMatchers.<AppUser>any());
    }

    @Test
    void shouldThrow409WhenEmailExists() {
        RegisterRequest req = buildRequest("testuser", "password123", "Test User", "test@example.com");
        when(appUserMapper.selectCount(ArgumentMatchers.<LambdaQueryWrapper<AppUser>>any()))
                .thenReturn(0L).thenReturn(0L).thenReturn(1L);

        assertThatThrownBy(() -> service.register(req))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getCode() == 409 && e.getMessage().contains("邮箱"));

        verify(appUserMapper, never()).insert(ArgumentMatchers.<AppUser>any());
    }

    @Test
    void shouldNormalizeEmptyEmailToNull() {
        RegisterRequest req = buildRequest("testuser", "password123", "Test User", "   ");
        assertThat(req.getEmail()).isNull();

        when(appUserMapper.selectCount(ArgumentMatchers.<LambdaQueryWrapper<AppUser>>any())).thenReturn(0L);
        when(appUserMapper.insert(ArgumentMatchers.<AppUser>any())).thenAnswer(inv -> {
            AppUser u = inv.getArgument(0);
            u.setId(1L);
            return 1;
        });

        RegisterResponse resp = service.register(req);
        assertThat(resp.email()).isNull();

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserMapper).insert(captor.capture());
        assertThat(captor.getValue().getEmail()).isNull();
    }

    @Test
    void shouldNormalizeEmailCaseAndTrimInDto() {
        RegisterRequest req = buildRequest("testuser", "password123", "Test User", "  TEST@EXAMPLE.COM  ");
        assertThat(req.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void shouldNormalizeUsernameCaseAndTrimInDto() {
        RegisterRequest req = buildRequest(" TestUser ", "password123", "Test User", "test@example.com");
        assertThat(req.getUsername()).isEqualTo("testuser");
    }

    @Test
    void shouldNormalizeNicknameTrimInDto() {
        RegisterRequest req = buildRequest("testuser", "password123", " Hello World ", "test@example.com");
        assertThat(req.getNickname()).isEqualTo("Hello World");
    }

    @Test
    void shouldNormalizeUsernameWithUnderscoreToLowercase() {
        RegisterRequest req = buildRequest("Test_User_01", "password123", "Test", "test@example.com");
        assertThat(req.getUsername()).isEqualTo("test_user_01");

        when(appUserMapper.selectCount(ArgumentMatchers.<LambdaQueryWrapper<AppUser>>any())).thenReturn(0L);
        when(appUserMapper.insert(ArgumentMatchers.<AppUser>any())).thenAnswer(inv -> {
            AppUser u = inv.getArgument(0);
            u.setId(2L);
            return 1;
        });

        RegisterResponse resp = service.register(req);
        assertThat(resp.username()).isEqualTo("test_user_01");
    }

    @Test
    void shouldThrow500WhenInsertReturnsZero() {
        RegisterRequest req = buildRequest("testuser", "password123", "Test User", "test@example.com");
        when(appUserMapper.selectCount(ArgumentMatchers.<LambdaQueryWrapper<AppUser>>any())).thenReturn(0L);
        when(appUserMapper.insert(ArgumentMatchers.<AppUser>any())).thenReturn(0);

        assertThatThrownBy(() -> service.register(req))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getCode() == 500);
    }

    @Test
    void shouldThrow409OnDuplicateKey() {
        RegisterRequest req = buildRequest("testuser", "password123", "Test User", "test@example.com");
        when(appUserMapper.selectCount(ArgumentMatchers.<LambdaQueryWrapper<AppUser>>any()))
                .thenReturn(0L).thenReturn(0L).thenReturn(0L);
        when(appUserMapper.insert(ArgumentMatchers.<AppUser>any())).thenThrow(new DuplicateKeyException("dup"));

        assertThatThrownBy(() -> service.register(req))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getCode() == 409
                        && e.getMessage().equals("用户名或邮箱已被使用"));
    }

    @Test
    void shouldThrow409OnNicknameDuplicateKeyViaConcurrentInsert() {
        RegisterRequest req = buildRequest("testuser", "password123", "Test User", "test@example.com");
        when(appUserMapper.selectCount(ArgumentMatchers.<LambdaQueryWrapper<AppUser>>any())).thenReturn(0L);
        var sqlEx = new java.sql.SQLIntegrityConstraintViolationException("Duplicate entry 'Test User' for key 'app_user.uk_app_user_nickname'");
        when(appUserMapper.insert(ArgumentMatchers.<AppUser>any()))
                .thenThrow(new DuplicateKeyException("dup", sqlEx));

        assertThatThrownBy(() -> service.register(req))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getCode() == 409
                        && e.getMessage().contains("昵称"));
    }

    private RegisterRequest buildRequest(String username, String password, String nickname, String email) {
        RegisterRequest req = new RegisterRequest();
        req.setUsername(username);
        req.setPassword(password);
        req.setNickname(nickname);
        req.setEmail(email);
        return req;
    }
}
