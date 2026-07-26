package com.itnoduck.acmate.synctask.controller;

import com.itnoduck.acmate.security.AuthenticatedUser;
import com.itnoduck.acmate.synctask.service.SyncTaskService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/sync-tasks")
public class SyncTaskController {

    private final SyncTaskService syncTaskService;

    public SyncTaskController(SyncTaskService syncTaskService) {
        this.syncTaskService = syncTaskService;
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @RequestParam(required = false) String taskStatus,
                                     @AuthenticationPrincipal AuthenticatedUser user) {
        return syncTaskService.listTasks(page, size, taskStatus, user);
    }

    @PostMapping("/trigger")
    public Map<String, Object> trigger(@AuthenticationPrincipal AuthenticatedUser user) {
        return syncTaskService.triggerSync(user);
    }
}
