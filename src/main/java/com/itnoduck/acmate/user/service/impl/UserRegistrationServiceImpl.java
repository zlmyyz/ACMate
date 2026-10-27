package com.itnoduck.acmate.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.user.dto.RegisterRequest;
import com.itnoduck.acmate.user.dto.RegisterResponse;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import com.itnoduck.acmate.user.service.UserRegistrationService;
import com.itnoduck.acmate.user.service.AlternativeLoginService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserRegistrationServiceImpl implements UserRegistrationService {

    private final AppUserMapper appUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final AlternativeLoginService emailLogin;

    public UserRegistrationServiceImpl(AppUserMapper appUserMapper, PasswordEncoder passwordEncoder, AlternativeLoginService emailLogin) {
        this.appUserMapper = appUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.emailLogin = emailLogin;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (!emailLogin.emailAvailable()) throw new BusinessException(503, "注册暂未开放：邮箱验证服务未启用");
        String username = request.getUsername();
        String nickname = request.getNickname();
        String email = request.getEmail() == null ? null : request.getEmail().strip().toLowerCase(java.util.Locale.ROOT);
        String rawPassword = request.getPassword();
        if (email == null || email.isBlank()) throw new BusinessException(400, "邮箱不能为空");

        if (appUserMapper.selectCount(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getUsername, username)) > 0) {
            throw new BusinessException(409, "用户名已被使用");
        }
        if (appUserMapper.selectCount(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getNickname, nickname)) > 0) {
            throw new BusinessException(409, "该昵称已被使用，请更换其他昵称。");
        }
        if (appUserMapper.findByLoginEmail(email) != null) {
            throw new BusinessException(409, "邮箱已被使用");
        }

        emailLogin.verifyCode(email, request.getEmailCode(), "REGISTER");
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setNickname(nickname);
        user.setEmail(email);
        user.setIsAdmin(0);
        user.setStatus(1);

        try {
            int rows = appUserMapper.insert(user);
            if (rows != 1 || user.getId() == null) {
                throw new BusinessException(500, "注册失败");
            }
        } catch (DuplicateKeyException e) {
            String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            if (msg != null && msg.contains("uk_app_user_nickname")) {
                throw new BusinessException(409, "该昵称已被使用，请更换其他昵称。");
            }
            throw new BusinessException(409, "用户名或邮箱已被使用");
        }

        appUserMapper.insertLoginEmail(user.getId(), email);
        return new RegisterResponse(user.getId(), user.getUsername(), user.getNickname(), user.getEmail());
    }
}
