package com.itnoduck.acmate.user.service;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.mockito.Mockito.*;
class OfflineIpRegionServiceTest {
    final OfflineIpRegionService regions=new OfflineIpRegionService();
    @Test void localIpv4AndIpv6NeverNeedAnExternalLookup() {
        for(String ip:java.util.List.of("127.0.0.1","::1","192.168.1.5","10.0.0.1","172.16.5.3","fc00::1")) assertEquals("本地网络",regions.resolve(ip));
    }
    @Test void nonIpInputsAreRejectedWithoutDnsResolution() {
        assertEquals("未知地区",regions.resolve("example.com"));assertEquals("未知地区",regions.resolve("not an ip"));assertEquals("未知地区",regions.resolve(null));
    }
    @Test void bundledDatabasesResolvePublicIpv4AndIpv6Locally() {
        assertNotEquals("未知地区",regions.resolve("114.114.114.114"));
        assertNotEquals("未知地区",regions.resolve("2606:4700:4700::1111"));
    }
    @Test void loginRecordingStoresRegionButPublicApisDoNotNeedRawIp() {
        var jdbc=mock(JdbcTemplate.class);var service=new LoginActivityService(jdbc,regions);
        service.record(5,"127.0.0.1");
        verify(jdbc).update(startsWith("UPDATE app_user"),eq("127.0.0.1"),eq("本地网络"),eq(5L));
    }
}
