package com.itnoduck.acmate.oj.controller;

import com.itnoduck.acmate.oj.service.OjAccountService;
import com.itnoduck.acmate.security.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/oj-accounts")
public class OjAccountController {

    private final OjAccountService ojAccountService;

    public OjAccountController(OjAccountService ojAccountService) {
        this.ojAccountService = ojAccountService;
    }

    @GetMapping("/me")
    public Map<String, Object> myAccount(@AuthenticationPrincipal AuthenticatedUser user) {
        return ojAccountService.getMyAccount(user);
    }

    @PostMapping
    public ResponseEntity<Void> bind(@RequestBody Map<String, String> body,
                                      @AuthenticationPrincipal AuthenticatedUser user) {
        ojAccountService.bind(body.get("handle"), user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> unbind(@AuthenticationPrincipal AuthenticatedUser user) {
        ojAccountService.unbind(user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin")
    public List<Map<String, Object>> pendingAccounts(@AuthenticationPrincipal AuthenticatedUser user) {
        return ojAccountService.getPendingAccounts(user);
    }

    @PostMapping("/admin/{id}/verify")
    public ResponseEntity<Void> verify(@PathVariable Long id, @RequestParam(defaultValue = "1") int status,
                                        @AuthenticationPrincipal AuthenticatedUser user) {
        ojAccountService.verify(id, status, user);
        return ResponseEntity.noContent().build();
    }
}
