package com.itnoduck.acmate.common.controller;

import com.itnoduck.acmate.user.mapper.AppUserMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    private final AppUserMapper appUserMapper;

    public HealthController(AppUserMapper appUserMapper) {
        this.appUserMapper = appUserMapper;
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        Long userCount = appUserMapper.selectCount(null);
        return Map.of(
            "status", "UP",
            "database", "UP",
            "userCount", userCount
        );
    }
}
