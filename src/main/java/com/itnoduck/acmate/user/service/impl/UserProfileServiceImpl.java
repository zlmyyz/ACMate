package com.itnoduck.acmate.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.oj.entity.OjAccount;
import com.itnoduck.acmate.oj.mapper.FirstAcMapper;
import com.itnoduck.acmate.oj.mapper.OjAccountMapper;
import com.itnoduck.acmate.oj.mapper.OjSubmissionMapper;
import com.itnoduck.acmate.problem.entity.Problem;
import com.itnoduck.acmate.problem.mapper.ProblemMapper;
import com.itnoduck.acmate.user.dto.OjStatsResponse;
import com.itnoduck.acmate.user.dto.PublicUserProfileResponse;
import com.itnoduck.acmate.user.dto.UpdateProfileRequest;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import com.itnoduck.acmate.user.service.UserProfileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    private final AppUserMapper appUserMapper;
    private final ProblemMapper problemMapper;
    private final OjAccountMapper ojAccountMapper;
    private final OjSubmissionMapper ojSubmissionMapper;
    private final Path uploadDir;

    public UserProfileServiceImpl(AppUserMapper appUserMapper,
                                  ProblemMapper problemMapper,
                                  OjAccountMapper ojAccountMapper,
                                  OjSubmissionMapper ojSubmissionMapper,
                                  @Value("${acmate.upload-dir:./uploads}") String uploadPath) {
        this.appUserMapper = appUserMapper;
        this.problemMapper = problemMapper;
        this.ojAccountMapper = ojAccountMapper;
        this.ojSubmissionMapper = ojSubmissionMapper;
        this.uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
    }

    @Override
    public PublicUserProfileResponse getProfile(long userId) {
        AppUser user = appUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        boolean isDisabled = user.getStatus() != null && user.getStatus() == 0;

        long problemCount = problemMapper.selectCount(
                new LambdaQueryWrapper<Problem>()
                        .eq(Problem::getCreatorUserId, userId)
                        .eq(Problem::getStatus, 1));

        PublicUserProfileResponse r = new PublicUserProfileResponse();
        r.setId(user.getId());
        r.setUsername(user.getUsername());
        r.setNickname(user.getNickname());
        r.setAvatarUrl(user.getAvatarUrl());
        r.setBio(user.getBio());
        r.setAdmin(user.getIsAdmin() != null && user.getIsAdmin() == 1);
        r.setAccountStatus(isDisabled ? "DISABLED" : "ACTIVE");
        r.setCreatedProblemCount(problemCount);
        r.setCreateTime(user.getCreateTime());

        // Codeforces account: only VERIFIED accounts are shown on public profile
        OjAccount cfAccount = ojAccountMapper.selectOne(new LambdaQueryWrapper<OjAccount>()
                .eq(OjAccount::getUserId, userId)
                .eq(OjAccount::getPlatform, "CODEFORCES")
                .eq(OjAccount::getVerifyStatus, 1));
        if (cfAccount != null) {
            r.setCodeforcesHandle(cfAccount.getExternalUserId());
        }

        // OJ stats: from oj_first_ac, same data source as leaderboard
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> stats = ojSubmissionMapper.getUserOjStats(userId,
                now.minusDays(30), now.minusDays(7));
        if (stats != null && stats.get("solved_count") != null) {
            OjStatsResponse ojStats = new OjStatsResponse();
            ojStats.setSolvedCount(((Number) stats.get("solved_count")).intValue());
            ojStats.setSolvedCount30d(stats.get("solved_30d") != null
                    ? ((Number) stats.get("solved_30d")).intValue() : 0);
            ojStats.setSolvedCount7d(stats.get("solved_7d") != null
                    ? ((Number) stats.get("solved_7d")).intValue() : 0);
            if (stats.get("last_accepted_time") != null) {
                ojStats.setLastAcceptedTime((LocalDateTime) stats.get("last_accepted_time"));
            }
            r.setOjStats(ojStats);
        } else {
            OjStatsResponse empty = new OjStatsResponse();
            r.setOjStats(empty);
        }

        return r;
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
            String trimmed = request.getNickname().strip();
            if (trimmed.isEmpty()) {
                throw new BusinessException(400, "昵称不能为空");
            }
            if (!trimmed.equalsIgnoreCase(user.getNickname())) {
                if (appUserMapper.selectCount(new LambdaQueryWrapper<AppUser>()
                        .eq(AppUser::getNickname, trimmed)) > 0) {
                    throw new BusinessException(409, "该昵称已被使用，请更换其他昵称。");
                }
            }
            wrapper.set(AppUser::getNickname, trimmed);
        }
        if (request.getBio() != null) {
            wrapper.set(AppUser::getBio, request.getBio().isBlank() ? null : request.getBio().strip());
        }

        try {
            appUserMapper.update(null, wrapper);
        } catch (DuplicateKeyException e) {
            String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            if (msg != null && msg.contains("uk_app_user_nickname")) {
                throw new BusinessException(409, "该昵称已被使用，请更换其他昵称。");
            }
            throw new BusinessException(409, "用户名或邮箱已被使用");
        }
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
