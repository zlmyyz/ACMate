package com.itnoduck.acmate.admin.controller;

import com.itnoduck.acmate.admin.dto.DeactivateUserRequest;
import com.itnoduck.acmate.admin.service.AdminUserService;
import com.itnoduck.acmate.security.AuthenticatedUser;
import jakarta.validation.Valid;
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
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String admin,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return adminUserService.listUsers(page, size, keyword, status, admin, user);
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long id,
                                           @Valid @RequestBody DeactivateUserRequest request,
                                           @AuthenticationPrincipal AuthenticatedUser user) {
        adminUserService.deactivate(id, request.getReason(), user);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable Long id,
                                         @AuthenticationPrincipal AuthenticatedUser user) {
        adminUserService.restore(id, user);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/grant-admin")
    public ResponseEntity<Void> grantAdmin(@PathVariable Long id,
                                            @AuthenticationPrincipal AuthenticatedUser user) {
        adminUserService.grantAdmin(id, user);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/revoke-admin")
    public ResponseEntity<Void> revokeAdmin(@PathVariable Long id,
                                             @AuthenticationPrincipal AuthenticatedUser user) {
        adminUserService.revokeAdmin(id, user);
        return ResponseEntity.noContent().build();
    }
}
