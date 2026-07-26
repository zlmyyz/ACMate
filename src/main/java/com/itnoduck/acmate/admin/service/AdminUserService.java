package com.itnoduck.acmate.admin.service;

import com.itnoduck.acmate.security.AuthenticatedUser;

import java.util.Map;

public interface AdminUserService {
    Map<String, Object> listUsers(int page, int size, String keyword, AuthenticatedUser user);
    void toggleStatus(Long id, AuthenticatedUser user);
    void toggleAdmin(Long id, AuthenticatedUser user);
}
