package com.itnoduck.acmate.user.service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
@Service
public class LoginActivityService {
    private final JdbcTemplate jdbc;
    private final OfflineIpRegionService regions;
    public LoginActivityService(JdbcTemplate jdbc,OfflineIpRegionService regions) {this.jdbc=jdbc;this.regions=regions;}
    public void record(long userId,String ip) {
        // Do not trust caller-supplied X-Forwarded-For or expose raw IP addresses in public profile APIs.
        jdbc.update("UPDATE app_user SET last_login_time=CURRENT_TIMESTAMP,last_login_ip=?,last_login_region=? WHERE id=? AND status=1",ip,regions.resolve(ip),userId);
    }
}
