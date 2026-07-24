package com.itnoduck.acmate.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.user.dto.RegisterRequest;
import com.itnoduck.acmate.user.dto.RegisterResponse;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import com.itnoduck.acmate.user.service.UserRegistrationService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserRegistrationServiceImpl implements UserRegistrationService {

    private final AppUserMapper appUserMapper;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationServiceImpl(AppUserMapper appUserMapper, PasswordEncoder passwordEncoder) {
        this.appUserMapper = appUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public RegisterResponse register(RegisterRequest request) {
        String username = request.getUsername();
        String nickname = request.getNickname();
        String email = request.getEmail();
        String rawPassword = request.getPassword();

        if (appUserMapper.selectCount(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getUsername, username)) > 0) {
            throw new BusinessException(409, "用户名已被使用");
        }
        if (email != null && appUserMapper.selectCount(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getEmail, email)) > 0) {
            throw new BusinessException(409, "邮箱已被使用");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setNickname(nickname);
        user.setEmail(email);
        user.setIsAdmin(0);
        user.setStatus(1);

        try {
            int rows = appUserMapper.insert(user);
            if (rows != 1) {
                throw new BusinessException(500, "注册失败");
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException(409, "用户名或邮箱已被使用");
        }

        return new RegisterResponse(user.getId(), user.getUsername(), user.getNickname(), user.getEmail());
    }
}
