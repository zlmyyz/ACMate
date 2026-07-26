package com.itnoduck.acmate.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.notification.entity.Notification;
import com.itnoduck.acmate.notification.mapper.NotificationMapper;
import com.itnoduck.acmate.notification.service.NotificationService;
import com.itnoduck.acmate.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    public NotificationServiceImpl(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    public Map<String, Object> listNotifications(AuthenticatedUser user, int page, int size) {
        var qw = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, user.getId())
                .orderByDesc(Notification::getCreateTime);
        var result = notificationMapper.selectPage(new Page<>(page, size), qw);
        List<Map<String, Object>> items = new ArrayList<>();
        for (var n : result.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", n.getId());
            m.put("type", n.getType());
            m.put("title", n.getTitle());
            m.put("content", n.getContent());
            m.put("resourceType", n.getResourceType());
            m.put("resourceId", n.getResourceId());
            m.put("isRead", n.getIsRead() != null && n.getIsRead() == 1);
            m.put("createTime", n.getCreateTime() != null ? n.getCreateTime().toString() : null);
            items.add(m);
        }
        return Map.of("items", items, "total", result.getTotal(), "page", page, "size", size);
    }

    @Override
    public long countUnread(AuthenticatedUser user) {
        return notificationMapper.countUnread(user.getId());
    }

    @Override
    @Transactional
    public void markRead(Long id, AuthenticatedUser user) {
        var n = notificationMapper.selectById(id);
        if (n == null) throw new BusinessException(404, "通知不存在");
        if (!n.getUserId().equals(user.getId())) throw new BusinessException(403, "无权操作");
        n.setIsRead(1);
        notificationMapper.updateById(n);
    }

    @Override
    @Transactional
    public void markAllRead(AuthenticatedUser user) {
        notificationMapper.markAllRead(user.getId());
    }
}
