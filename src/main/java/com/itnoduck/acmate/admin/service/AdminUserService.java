package com.itnoduck.acmate.admin.service;

import com.itnoduck.acmate.security.AuthenticatedUser;

import java.util.Map;

public interface AdminUserService {
    Map<String, Object> listUsers(int page, int size, String keyword, String status, String admin, AuthenticatedUser user);
    void deactivate(Long id, String reason, AuthenticatedUser user);
    void restore(Long id, AuthenticatedUser user);
    void grantAdmin(Long id, AuthenticatedUser user);
    void revokeAdmin(Long id, AuthenticatedUser user);
}
