package com.itnoduck.acmate.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.problem.entity.Problem;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import com.itnoduck.acmate.user.dto.UpdateProfileRequest;
import com.itnoduck.acmate.user.dto.UserProfileResponse;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import com.itnoduck.acmate.user.service.UserProfileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    private final AppUserMapper appUserMapper;
    private final ProblemMapper problemMapper;
    private final Path uploadDir;

    public UserProfileServiceImpl(AppUserMapper appUserMapper,
                                  ProblemMapper problemMapper,
                                  @Value("${acmate.upload-dir:./uploads}") String uploadPath) {
        this.appUserMapper = appUserMapper;
        this.problemMapper = problemMapper;
        this.uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
    }

    @Override
    public UserProfileResponse getProfile(long userId) {
        AppUser user = appUserMapper.selectById(userId);
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException(404, "用户不存在");
        }

        long problemCount = problemMapper.selectCount(
                new LambdaQueryWrapper<Problem>()
                        .eq(Problem::getCreatorUserId, userId)
                        .eq(Problem::getStatus, 1));

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getIsAdmin() != null && user.getIsAdmin() == 1,
                problemCount,
                user.getCreateTime());
    }

    @Override
    @Transactional
    public void updateProfile(long userId, UpdateProfileRequest request) {
        AppUser user = appUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        LambdaUpdateWrapper<AppUser> wrapper = Wrappers.lambdaUpdate(AppUser.class)
                .eq(AppUser::getId, userId);

        if (request.getNickname() != null) {
            wrapper.set(AppUser::getNickname, request.getNickname().strip());
        }
        if (request.getBio() != null) {
            wrapper.set(AppUser::getBio, request.getBio().isBlank() ? null : request.getBio().strip());
        }

        appUserMapper.update(null, wrapper);
    }

    @Override
    public String updateAvatar(long userId, String originalFilename, byte[] content) {
        AppUser user = appUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        String ext = "";
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        if (!ext.matches("\\.(png|jpg|jpeg|gif|webp)")) {
            throw new BusinessException(400, "不支持的文件类型，仅支持 PNG、JPG、GIF、WebP");
        }
        if (content.length > 2 * 1024 * 1024) {
            throw new BusinessException(400, "文件大小不能超过 2MB");
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Files.createDirectories(uploadDir);
            Path filePath = uploadDir.resolve(filename).normalize();
            if (!filePath.startsWith(uploadDir)) {
                throw new BusinessException(400, "非法的文件路径");
            }
            Files.write(filePath, content);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败");
        }

        String avatarUrl = "/uploads/" + filename;

        appUserMapper.update(null,
                Wrappers.lambdaUpdate(AppUser.class)
                        .eq(AppUser::getId, userId)
                        .set(AppUser::getAvatarUrl, avatarUrl));

        return avatarUrl;
    }
}
