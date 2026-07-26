package com.itnoduck.acmate.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itnoduck.acmate.admin.service.AdminUserService;
import com.itnoduck.acmate.auditlog.service.AuditLogService;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.security.AuthenticatedUser;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final AppUserMapper userMapper;
    private final SessionRegistry sessionRegistry;
    private final AuditLogService auditLogService;

    public AdminUserServiceImpl(AppUserMapper userMapper, SessionRegistry sessionRegistry, AuditLogService auditLogService) {
        this.userMapper = userMapper;
        this.sessionRegistry = sessionRegistry;
        this.auditLogService = auditLogService;
    }

    @Override
    public Map<String, Object> listUsers(int page, int size, String keyword, AuthenticatedUser user) {
        if (!user.isAdmin()) throw new BusinessException(403, "无权访问");

        var qw = new LambdaQueryWrapper<AppUser>();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(AppUser::getUsername, keyword).or().like(AppUser::getNickname, keyword));
        }
        qw.orderByDesc(AppUser::getCreateTime);

        var result = userMapper.selectPage(new Page<>(page, size), qw);
        List<Map<String, Object>> users = new ArrayList<>();
        for (var u : result.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("nickname", u.getNickname());
            m.put("email", u.getEmail());
            m.put("avatarUrl", u.getAvatarUrl());
            m.put("bio", u.getBio());
            m.put("admin", u.getIsAdmin() != null && u.getIsAdmin() == 1);
            m.put("status", u.getStatus());
            m.put("createTime", u.getCreateTime() != null ? u.getCreateTime().toString() : null);
            m.put("lastLoginTime", u.getLastLoginTime() != null ? u.getLastLoginTime().toString() : null);
            users.add(m);
        }
        return Map.of("users", users, "total", result.getTotal(), "page", page, "size", size);
    }

    @Override
    @Transactional
    public void toggleStatus(Long id, AuthenticatedUser user) {
        if (!user.isAdmin()) throw new BusinessException(403, "无权访问");
        if (id.equals(user.getId())) throw new BusinessException(400, "不能禁用自己");
        var u = userMapper.selectById(id);
        if (u == null) throw new BusinessException(404, "用户不存在");

        String before = u.getStatus() != null && u.getStatus() == 1 ? "enabled" : "disabled";
        int newStatus = u.getStatus() != null && u.getStatus() == 1 ? 0 : 1;
        userMapper.update(null, Wrappers.lambdaUpdate(AppUser.class)
                .eq(AppUser::getId, id).set(AppUser::getStatus, newStatus));

        String after = newStatus == 1 ? "enabled" : "disabled";
        auditLogService.log(user.getId(), "TOGGLE_USER_STATUS", "USER", id, null, before, after);

        if (newStatus == 0) {
            expireUserSessions(id);
        }
    }

    private void expireUserSessions(Long userId) {
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof AuthenticatedUser au && au.getId().equals(userId)) {
                for (SessionInformation si : sessionRegistry.getAllSessions(principal, false)) {
                    si.expireNow();
                }
            }
        }
    }

    @Override
    @Transactional
    public void toggleAdmin(Long id, AuthenticatedUser user) {
        if (!user.isAdmin()) throw new BusinessException(403, "无权访问");
        if (id.equals(user.getId())) throw new BusinessException(400, "不能取消自己的管理员权限");
        var u = userMapper.selectById(id);
        if (u == null) throw new BusinessException(404, "用户不存在");

        int newAdmin = u.getIsAdmin() != null && u.getIsAdmin() == 1 ? 0 : 1;

        if (newAdmin == 0) {
            long adminCount = userMapper.selectCount(new LambdaQueryWrapper<AppUser>()
                    .eq(AppUser::getIsAdmin, 1).eq(AppUser::getStatus, 1));
            if (adminCount <= 1) throw new BusinessException(400, "系统必须至少保留一个管理员");
        }

        userMapper.update(null, Wrappers.lambdaUpdate(AppUser.class)
                .eq(AppUser::getId, id).set(AppUser::getIsAdmin, newAdmin));

        String before = u.getIsAdmin() != null && u.getIsAdmin() == 1 ? "admin" : "user";
        String after = newAdmin == 1 ? "admin" : "user";
        auditLogService.log(user.getId(), "TOGGLE_ADMIN", "USER", id, null, before, after);
    }
}
