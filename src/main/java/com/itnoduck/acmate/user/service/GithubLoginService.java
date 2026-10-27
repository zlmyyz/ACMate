package com.itnoduck.acmate.user.service;

import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.config.LoginProperties;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.dto.GithubProfile;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
public class GithubLoginService {
    private final LoginProperties properties;
    private final JdbcTemplate jdbc;
    private final AppUserMapper users;
    private final PasswordEncoder encoder;
    private final RestClient client;
    private final ObjectMapper objectMapper;

    public GithubLoginService(LoginProperties properties, JdbcTemplate jdbc, AppUserMapper users,
                              PasswordEncoder encoder, ObjectMapper objectMapper) {
        this.properties = properties;
        this.jdbc = jdbc;
        this.users = users;
        this.encoder = encoder;
        this.objectMapper = objectMapper;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        client = RestClient.builder().requestFactory(factory).build();
    }

    public boolean available() {
        if (!properties.isGithubEnabled() || properties.getGithubClientId().isBlank()
                || properties.getGithubClientSecret().isBlank() || properties.getGithubCallbackUrl().isBlank()) return false;
        try {
            URI uri = URI.create(properties.getGithubCallbackUrl());
            boolean local = "http".equals(uri.getScheme())
                    && ("localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost()));
            return ("https".equals(uri.getScheme()) || local) && uri.getHost() != null
                    && uri.getUserInfo() == null && uri.getQuery() == null && uri.getFragment() == null
                    && "/api/auth/github/callback".equals(uri.getPath());
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public String authorizeUrl(String state, String verifier) {
        requireAvailable();
        return UriComponentsBuilder.fromUriString("https://github.com/login/oauth/authorize")
                .queryParam("client_id", properties.getGithubClientId())
                .queryParam("redirect_uri", properties.getGithubCallbackUrl())
                .queryParam("scope", "user:email")
                .queryParam("state", state)
                .queryParam("code_challenge", challenge(verifier))
                .queryParam("code_challenge_method", "S256")
                .build().encode().toUriString();
    }

    public GithubProfile exchangeProfile(String code, String verifier) {
        requireAvailable();
        try {
            var form = new LinkedMultiValueMap<String, String>();
            form.add("client_id", properties.getGithubClientId());
            form.add("client_secret", properties.getGithubClientSecret());
            form.add("redirect_uri", properties.getGithubCallbackUrl());
            form.add("code", code);
            form.add("code_verifier", verifier);
            String rawToken = client.post().uri("https://github.com/login/oauth/access_token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.APPLICATION_JSON)
                    .body(form).retrieve().body(String.class);
            Map<?, ?> token = rawToken == null ? null : objectMapper.readValue(rawToken, Map.class);
            if (token == null || token.containsKey("error")
                    || !(token.get("access_token") instanceof String accessToken) || accessToken.isBlank()) {
                throw new BusinessException(401, "GitHub 授权失败，请重新登录");
            }
            // Read the authenticated profile on the server. Never trust browser-supplied GitHub identities.
            String rawUser = client.get().uri("https://api.github.com/user")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .header("Accept", "application/vnd.github+json").header("User-Agent", "ACMate")
                    .retrieve().body(String.class);
            Map<?, ?> profile = rawUser == null ? null : objectMapper.readValue(rawUser, Map.class);
            if (profile == null || !(profile.get("id") instanceof Number id) || id.longValue() <= 0) {
                throw new BusinessException(401, "无法读取 GitHub 用户信息，请重新登录");
            }
            if (!(profile.get("login") instanceof String login) || login.isBlank()) {
                throw new BusinessException(401, "无法读取 GitHub 用户名，请重新登录");
            }
            String name = profile.get("name") instanceof String displayName ? displayName : null;
            String verifiedEmail = null;
            String rawEmails = client.get().uri("https://api.github.com/user/emails")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .header("Accept", "application/vnd.github+json").header("User-Agent", "ACMate")
                    .retrieve().body(String.class);
            if (rawEmails != null) {
                var emails = objectMapper.readValue(rawEmails, java.util.List.class);
                for (Object item : emails) {
                    if (item instanceof Map<?, ?> entry && Boolean.TRUE.equals(entry.get("verified"))
                            && entry.get("email") instanceof String address) {
                        if (verifiedEmail == null || Boolean.TRUE.equals(entry.get("primary"))) verifiedEmail = address;
                        if (Boolean.TRUE.equals(entry.get("primary"))) break;
                    }
                }
            }
            return new GithubProfile(id.longValue(), login, name, verifiedEmail);
        } catch (org.springframework.web.client.RestClientException | tools.jackson.core.JacksonException ex) {
            throw new BusinessException(502, "暂时无法连接 GitHub，请稍后重试");
        }
    }

    @Transactional
    public String resolveUsername(GithubProfile profile) {
        // A permanent numeric GitHub ID remains stable when the user changes their GitHub login name.
        var ids = jdbc.queryForList("SELECT user_id FROM user_login_identity WHERE provider='GITHUB' AND identifier=?", Long.class, profile.id());
        if (!ids.isEmpty()) {
            updateIdentity(profile);
            AppUser user = users.selectById(ids.get(0));
            if (user == null) throw new BusinessException(401, "关联账号不可用，请联系管理员");
            // Upgrade only our old generated placeholder. Linked local profiles and edited nicknames stay intact.
            boolean placeholder = user.getUsername().matches("gh_[a-f0-9]{24}")
                    && ("GitHub_" + user.getUsername().substring(3)).equals(user.getNickname());
            if (placeholder && Integer.valueOf(1).equals(user.getStatus())) {
                String nickname = availableNickname(profile, user.getId());
                if (!nickname.equals(user.getNickname())) {
                    AppUser update = new AppUser();
                    update.setId(user.getId());
                    update.setNickname(nickname);
                    users.updateById(update);
                }
            }
            return user.getUsername();
        }
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        AppUser user = new AppUser();
        user.setUsername("gh_" + suffix);
        user.setNickname(availableNickname(profile, null));
        user.setPasswordHash(encoder.encode(UUID.randomUUID().toString()));
        user.setIsAdmin(0);
        user.setStatus(1);
        users.insert(user);
        jdbc.update("INSERT INTO user_login_identity(identifier,user_id,login_name,display_name,provider) VALUES(?,?,?,?,'GITHUB')", profile.id(), user.getId(), profile.login(), profile.name());
        return user.getUsername();
    }

    public void updateIdentity(GithubProfile profile) {
        jdbc.update("UPDATE user_login_identity SET login_name=?,display_name=? WHERE provider='GITHUB' AND identifier=?", profile.login(), profile.name(), profile.id());
    }

    private String availableNickname(GithubProfile profile, Long userId) {
        String preferred = profile.name() == null ? "" : profile.name().replaceAll("[\\p{Cntrl}]", "").strip();
        if (preferred.isBlank()) preferred = profile.login().strip();
        if (preferred.length() < 2) preferred += "_";
        for (int attempt = 0; attempt < 4; attempt++) {
            String suffix = attempt == 0 ? "" : attempt == 1 ? "_gh" + profile.id()
                    : "_" + UUID.randomUUID().toString().substring(0, 8);
            int end = Math.min(preferred.length(), 32 - suffix.length());
            if (end > 0 && Character.isHighSurrogate(preferred.charAt(end - 1))) end--;
            String candidate = preferred.substring(0, end) + suffix;
            var query = new LambdaQueryWrapper<AppUser>().eq(AppUser::getNickname, candidate);
            if (userId != null) query.ne(AppUser::getId, userId);
            if (users.selectCount(query) == 0) return candidate;
        }
        throw new BusinessException(409, "昵称暂时不可用，请稍后重试");
    }

    private void requireAvailable() {
        if (!available()) throw new BusinessException(503, "GitHub 登录尚未配置，请使用其他方式登录");
    }

    private String challenge(String verifier) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
