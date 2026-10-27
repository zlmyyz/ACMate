package com.itnoduck.acmate.user.service;

import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.config.LoginProperties;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import com.itnoduck.acmate.user.dto.GithubProfile;
import com.itnoduck.acmate.user.entity.AppUser;
import java.util.List;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GithubLoginServiceTest {
    final LoginProperties properties = new LoginProperties();
    final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    final AppUserMapper users = mock(AppUserMapper.class);
    final GithubLoginService service = new GithubLoginService(properties, jdbc,
            users, mock(PasswordEncoder.class), new ObjectMapper());

    void configure() {
        properties.setGithubEnabled(true);
        properties.setGithubClientId("client-id");
        properties.setGithubClientSecret("test-only-secret");
        properties.setGithubCallbackUrl("http://localhost:5173/api/auth/github/callback");
    }

    @Test void unconfiguredLoginIsUnavailable() {
        assertFalse(service.available());
        assertEquals(503, assertThrows(BusinessException.class, () -> service.authorizeUrl("state", "verifier")).getCode());
    }

    @Test void callbackAllowsLocalDevelopmentAndRequiresHttpsForPublicHosts() {
        configure();
        assertTrue(service.available());
        properties.setGithubCallbackUrl("http://example.com/api/auth/github/callback");
        assertFalse(service.available());
        properties.setGithubCallbackUrl("https://example.com/api/auth/github/callback");
        assertTrue(service.available());
        properties.setGithubCallbackUrl("https://example.com/api/auth/github/callback?redirect=bad");
        assertFalse(service.available());
    }

    @Test void authorizeUsesPkceAndNeverExposesSecretOrVerifier() {
        configure();
        String verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        String url = service.authorizeUrl("session-state", verifier);
        assertTrue(url.startsWith("https://github.com/login/oauth/authorize?"));
        assertTrue(url.contains("state=session-state"));
        assertTrue(url.contains("code_challenge_method=S256"));
        assertTrue(url.contains("code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"));
        assertFalse(url.contains("test-only-secret"));
        assertFalse(url.contains(verifier));
    }

    @Test void tokenIsExchangedByPostAndIdentityIsFetchedWithBearerToken() {
        configure();
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(service, "client", builder.build());
        server.expect(requestTo("https://github.com/login/oauth/access_token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("code_verifier=proof")))
                .andRespond(withSuccess("{\"access_token\":\"server-token\",\"token_type\":\"bearer\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.github.com/user"))
                .andExpect(header("Authorization", "Bearer server-token"))
                .andRespond(withSuccess("{\"id\":12345,\"login\":\"test-user\",\"name\":\"GitHub Display Name\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.github.com/user/emails"))
                .andExpect(header("Authorization", "Bearer server-token"))
                .andRespond(withSuccess("[{\"email\":\"unverified@example.com\",\"verified\":false,\"primary\":true},{\"email\":\"verified@example.com\",\"verified\":true,\"primary\":false}]", MediaType.APPLICATION_JSON));
        assertEquals(new GithubProfile(12345L, "test-user", "GitHub Display Name", "verified@example.com"), service.exchangeProfile("code", "proof"));
        server.verify();
    }

    @Test void providerRejectionDoesNotCreateALocalIdentity() {
        configure();
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(service, "client", builder.build());
        server.expect(requestTo("https://github.com/login/oauth/access_token"))
                .andRespond(withSuccess("{\"error\":\"bad_verification_code\"}", MediaType.APPLICATION_JSON));
        assertEquals(401, assertThrows(BusinessException.class, () -> service.exchangeProfile("bad-code", "proof")).getCode());
        server.verify();
    }

    private AppUser createdUser(GithubProfile profile) {
        when(users.selectCount(any())).thenReturn(0L);
        when(users.insert(any(AppUser.class))).thenAnswer(invocation -> {
            ((AppUser) invocation.getArgument(0)).setId(11L);
            return 1;
        });
        service.resolveUsername(profile);
        var created = ArgumentCaptor.forClass(AppUser.class);
        verify(users).insert(created.capture());
        return created.getValue();
    }

    @Test void newAccountUsesGithubDisplayName() {
        AppUser user = createdUser(new GithubProfile(12345L, "test-user", " GitHub 昵称 "));
        assertEquals("GitHub 昵称", user.getNickname());
        assertEquals(0, user.getIsAdmin());
        verify(jdbc).update(startsWith("INSERT INTO user_login_identity"), eq(12345L), eq(11L), eq("test-user"), eq(" GitHub 昵称 "));
    }

    @Test void missingDisplayNameFallsBackToGithubLogin() {
        assertEquals("test-user", createdUser(new GithubProfile(12345L, "test-user", null)).getNickname());
    }

    @Test void nicknameCollisionGetsSuffixInsteadOfLinkingToAnUnrelatedAccount() {
        when(users.selectCount(any())).thenReturn(1L, 0L);
        service.resolveUsername(new GithubProfile(12345L, "test-user", "Same Name"));
        var created = ArgumentCaptor.forClass(AppUser.class);
        verify(users).insert(created.capture());
        assertEquals("Same Name_gh12345", created.getValue().getNickname());
    }

    @Test void existingGithubBindingLogsIntoTheExactLinkedLocalAccount() {
        when(jdbc.queryForList(anyString(), eq(Long.class), eq(12345L))).thenReturn(List.of(7L));
        AppUser linked = new AppUser();
        linked.setId(7L); linked.setUsername("existing-local-user"); linked.setNickname("本站昵称"); linked.setStatus(1);
        when(users.selectById(7L)).thenReturn(linked);
        assertEquals("existing-local-user", service.resolveUsername(new GithubProfile(12345L, "renamed-on-github", "Different Name")));
        verify(users, never()).insert(any(AppUser.class));
        verify(users, never()).updateById(any(AppUser.class));
        verify(jdbc).update(startsWith("UPDATE user_login_identity"), eq("renamed-on-github"), eq("Different Name"), eq(12345L));
    }

    @Test void oldGeneratedNicknameIsReplacedWithoutCreatingAnotherAccount() {
        when(jdbc.queryForList(anyString(), eq(Long.class), eq(12345L))).thenReturn(List.of(3L));
        when(users.selectCount(any())).thenReturn(0L);
        AppUser linked = new AppUser();
        linked.setId(3L); linked.setUsername("gh_aa8a07f84db64afa8defe6aa");
        linked.setNickname("GitHub_aa8a07f84db64afa8defe6aa"); linked.setStatus(1);
        when(users.selectById(3L)).thenReturn(linked);
        assertEquals(linked.getUsername(), service.resolveUsername(new GithubProfile(12345L, "test-user", "真实昵称")));
        var updated = ArgumentCaptor.forClass(AppUser.class);
        verify(users).updateById(updated.capture());
        assertEquals(3L, updated.getValue().getId());
        assertEquals("真实昵称", updated.getValue().getNickname());
        verify(users, never()).insert(any(AppUser.class));
    }
}
