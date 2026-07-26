package com.itnoduck.acmate.oj.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.oj.entity.OjAccount;
import com.itnoduck.acmate.oj.mapper.OjAccountMapper;
import com.itnoduck.acmate.security.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/oj-accounts")
public class OjAccountController {

    private final OjAccountMapper accountMapper;

    public OjAccountController(OjAccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    @GetMapping("/me")
    public Map<String, Object> myAccount(@AuthenticationPrincipal AuthenticatedUser user) {
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

    @PostMapping
    public ResponseEntity<Void> bind(@RequestBody Map<String, String> body,
                                      @AuthenticationPrincipal AuthenticatedUser user) {
        String handle = body.get("handle");
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
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> unbind(@AuthenticationPrincipal AuthenticatedUser user) {
        accountMapper.delete(new LambdaQueryWrapper<OjAccount>().eq(OjAccount::getUserId, user.getId()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin")
    public List<Map<String, Object>> pendingAccounts(@AuthenticationPrincipal AuthenticatedUser user) {
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

    @PostMapping("/admin/{id}/verify")
    public ResponseEntity<Void> verify(@PathVariable Long id, @RequestParam(defaultValue = "1") int status,
                                        @AuthenticationPrincipal AuthenticatedUser user) {
        if (!user.isAdmin()) throw new BusinessException(403, "无权操作");
        var acc = accountMapper.selectById(id);
        if (acc == null) throw new BusinessException(404, "账号不存在");
        acc.setVerifyStatus(status);
        accountMapper.updateById(acc);
        return ResponseEntity.noContent().build();
    }
}
