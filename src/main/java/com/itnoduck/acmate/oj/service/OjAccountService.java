package com.itnoduck.acmate.oj.service;

import com.itnoduck.acmate.oj.dto.SyncResult;
import com.itnoduck.acmate.oj.entity.OjAccount;
import com.itnoduck.acmate.security.AuthenticatedUser;

import java.util.List;
import java.util.Map;

public interface OjAccountService {
    Map<String, Object> getMyAccount(AuthenticatedUser user);
    void bind(String handle, AuthenticatedUser user);
    void unbind(AuthenticatedUser user);
    List<Map<String, Object>> getPendingAccounts(AuthenticatedUser user);
    void verify(Long id, int status, AuthenticatedUser user);
    SyncResult syncMyAccount(AuthenticatedUser user);
    SyncResult syncAccountById(Long accountId, String triggerType);
}
