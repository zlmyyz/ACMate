package com.itnoduck.acmate.admin.controller;

import com.itnoduck.acmate.admin.service.AdminUserService;
import com.itnoduck.acmate.security.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String keyword,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return adminUserService.listUsers(page, size, keyword, user);
    }

    @PostMapping("/{id}/toggle-status")
    public ResponseEntity<Void> toggleStatus(@PathVariable Long id,
                                              @AuthenticationPrincipal AuthenticatedUser user) {
        adminUserService.toggleStatus(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/toggle-admin")
    public ResponseEntity<Void> toggleAdmin(@PathVariable Long id,
                                             @AuthenticationPrincipal AuthenticatedUser user) {
        adminUserService.toggleAdmin(id, user);
        return ResponseEntity.noContent().build();
    }
}
