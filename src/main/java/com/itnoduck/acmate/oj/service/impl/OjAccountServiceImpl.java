package com.itnoduck.acmate.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.oj.entity.OjAccount;
import com.itnoduck.acmate.oj.mapper.OjAccountMapper;
import com.itnoduck.acmate.oj.service.OjAccountService;
import com.itnoduck.acmate.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class OjAccountServiceImpl implements OjAccountService {

    private final OjAccountMapper accountMapper;

    public OjAccountServiceImpl(OjAccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    @Override
    public Map<String, Object> getMyAccount(AuthenticatedUser user) {
        var acc = accountMapper.selectOne(new LambdaQueryWrapper<OjAccount>()
                .eq(OjAccount::getUserId, user.getId()));
        if (acc == null) return Map.of("hasAccount", false);
        return Map.of(
            "hasAccount", true,
            "id", acc.getId(),
            "platform", acc.getPlatform(),
            "externalUserId", acc.getExternalUserId(),
            "displayName", acc.getDisplayName(),
            "verifyStatus", acc.getVerifyStatus(),
            "syncEnabled", acc.getSyncEnabled(),
            "lastSyncTime", acc.getLastSyncTime() != null ? acc.getLastSyncTime().toString() : null,
            "lastSyncSuccess", acc.getLastSyncSuccess()
        );
    }

    @Override
    @Transactional
    public void bind(String handle, AuthenticatedUser user) {
        if (handle == null || handle.isBlank()) throw new BusinessException(400, "Codeforces handle 不能为空");

        var existing = accountMapper.selectOne(new LambdaQueryWrapper<OjAccount>()
                .eq(OjAccount::getUserId, user.getId()));
        if (existing != null) throw new BusinessException(409, "已绑定过 Codeforces 账号，请先解绑");

        var conflict = accountMapper.selectOne(new LambdaQueryWrapper<OjAccount>()
                .eq(OjAccount::getExternalUserId, handle.trim()));
        if (conflict != null) throw new BusinessException(409, "该 Codeforces 账号已被其他用户绑定");

        OjAccount acc = new OjAccount();
        acc.setUserId(user.getId());
        acc.setPlatform("CODEFORCES");
        acc.setExternalUserId(handle.trim());
        acc.setDisplayName(handle.trim());
        acc.setVerifyStatus(0);
        acc.setSyncEnabled(1);
        accountMapper.insert(acc);
    }

    @Override
    @Transactional
    public void unbind(AuthenticatedUser user) {
        accountMapper.delete(new LambdaQueryWrapper<OjAccount>().eq(OjAccount::getUserId, user.getId()));
    }

    @Override
    public List<Map<String, Object>> getPendingAccounts(AuthenticatedUser user) {
        if (!user.isAdmin()) throw new BusinessException(403, "无权访问");
        var accounts = accountMapper.selectList(new LambdaQueryWrapper<OjAccount>()
                .orderByAsc(OjAccount::getCreateTime));
        List<Map<String, Object>> result = new ArrayList<>();
        for (var a : accounts) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("userId", a.getUserId());
            m.put("platform", a.getPlatform());
            m.put("externalUserId", a.getExternalUserId());
            m.put("displayName", a.getDisplayName());
            m.put("verifyStatus", a.getVerifyStatus());
            m.put("syncEnabled", a.getSyncEnabled());
            m.put("lastSyncTime", a.getLastSyncTime() != null ? a.getLastSyncTime().toString() : null);
            result.add(m);
        }
        return result;
    }

    @Override
    @Transactional
    public void verify(Long id, int status, AuthenticatedUser user) {
        if (!user.isAdmin()) throw new BusinessException(403, "无权操作");
        var acc = accountMapper.selectById(id);
        if (acc == null) throw new BusinessException(404, "账号不存在");
        acc.setVerifyStatus(status);
        accountMapper.updateById(acc);
    }
}
