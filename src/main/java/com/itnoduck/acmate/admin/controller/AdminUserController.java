package com.itnoduck.acmate.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.security.AuthenticatedUser;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AppUserMapper userMapper;

    public AdminUserController(AppUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String keyword,
            @AuthenticationPrincipal AuthenticatedUser user) {
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

    @PostMapping("/{id}/toggle-status")
    public ResponseEntity<Void> toggleStatus(@PathVariable Long id,
                                              @AuthenticationPrincipal AuthenticatedUser user) {
        if (!user.isAdmin()) throw new BusinessException(403, "无权访问");
        var u = userMapper.selectById(id);
        if (u == null) throw new BusinessException(404, "用户不存在");
        if (u.getId().equals(user.getId())) throw new BusinessException(400, "不能禁用自己");

        int newStatus = u.getStatus() != null && u.getStatus() == 1 ? 0 : 1;
        userMapper.update(null, Wrappers.lambdaUpdate(AppUser.class)
                .eq(AppUser::getId, id).set(AppUser::getStatus, newStatus));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/toggle-admin")
    public ResponseEntity<Void> toggleAdmin(@PathVariable Long id,
                                             @AuthenticationPrincipal AuthenticatedUser user) {
        if (!user.isAdmin()) throw new BusinessException(403, "无权访问");
        var u = userMapper.selectById(id);
        if (u == null) throw new BusinessException(404, "用户不存在");
        if (u.getId().equals(user.getId())) throw new BusinessException(400, "不能取消自己的管理员权限");

        int newAdmin = u.getIsAdmin() != null && u.getIsAdmin() == 1 ? 0 : 1;

        if (newAdmin == 0) {
            long adminCount = userMapper.selectCount(new LambdaQueryWrapper<AppUser>()
                    .eq(AppUser::getIsAdmin, 1).eq(AppUser::getStatus, 1));
            if (adminCount <= 1) throw new BusinessException(400, "系统必须至少保留一个管理员");
        }

        userMapper.update(null, Wrappers.lambdaUpdate(AppUser.class)
                .eq(AppUser::getId, id).set(AppUser::getIsAdmin, newAdmin));
        return ResponseEntity.noContent().build();
    }
}
