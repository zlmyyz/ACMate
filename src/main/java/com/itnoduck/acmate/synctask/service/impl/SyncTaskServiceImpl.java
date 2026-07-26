package com.itnoduck.acmate.synctask.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.security.AuthenticatedUser;
import com.itnoduck.acmate.synctask.entity.SyncTaskLog;
import com.itnoduck.acmate.synctask.mapper.SyncTaskLogMapper;
import com.itnoduck.acmate.synctask.service.SyncTaskService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SyncTaskServiceImpl implements SyncTaskService {

    private final SyncTaskLogMapper taskLogMapper;

    public SyncTaskServiceImpl(SyncTaskLogMapper taskLogMapper) {
        this.taskLogMapper = taskLogMapper;
    }

    @Override
    public Map<String, Object> listTasks(int page, int size, String taskStatus, AuthenticatedUser user) {
        if (!user.isAdmin()) throw new BusinessException(403, "无权访问");
        var qw = new LambdaQueryWrapper<SyncTaskLog>();
        if (taskStatus != null && !taskStatus.isBlank()) qw.eq(SyncTaskLog::getTaskStatus, taskStatus);
        qw.orderByDesc(SyncTaskLog::getCreateTime);
        var result = taskLogMapper.selectPage(new Page<>(page, size), qw);
        List<Map<String, Object>> items = new ArrayList<>();
        for (var t : result.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("ojAccountId", t.getOjAccountId());
            m.put("platform", t.getPlatform());
            m.put("triggerType", t.getTriggerType());
            m.put("taskStatus", t.getTaskStatus());
            m.put("fetchedCount", t.getFetchedCount());
            m.put("insertedCount", t.getInsertedCount());
            m.put("firstAcCount", t.getFirstAcCount());
            m.put("errorMessage", t.getErrorMessage());
            m.put("startTime", t.getStartTime() != null ? t.getStartTime().toString() : null);
            m.put("endTime", t.getEndTime() != null ? t.getEndTime().toString() : null);
            items.add(m);
        }
        return Map.of("items", items, "total", result.getTotal(), "page", page, "size", size);
    }
}
