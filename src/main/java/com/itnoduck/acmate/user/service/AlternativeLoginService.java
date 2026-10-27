package com.itnoduck.acmate.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.config.LoginProperties;
import com.itnoduck.acmate.security.DatabaseUserDetailsService;
import com.itnoduck.acmate.user.dto.LoginResponse;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class AlternativeLoginService {
    private final LoginProperties properties;
    private final ObjectProvider<JavaMailSender> mail;
    private final StringRedisTemplate redis;
    private final AppUserMapper users;
    private final PasswordEncoder encoder;
    private final DatabaseUserDetailsService details;
    private final SecurityContextRepository contexts;
    private final SessionAuthenticationStrategy sessions;
    private final SecureRandom random = new SecureRandom();
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final DefaultRedisScript<Long> STORE_CODE_SCRIPT = new DefaultRedisScript<>(
            "redis.call('HMSET', KEYS[1], 'codeHash', ARGV[1], 'attempts', 0); " +
                    "redis.call('EXPIRE', KEYS[1], ARGV[2]); return 1", Long.class);
    private static final DefaultRedisScript<Long> DELETE_IF_MATCHES_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('HGET', KEYS[1], 'codeHash') == ARGV[1] then " +
                    "return redis.call('DEL', KEYS[1]); else return 0; end", Long.class);
    private static final DefaultRedisScript<Long> CONSUME_CODE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('HGET', KEYS[1], 'codeHash') == ARGV[1] then " +
                    "return redis.call('DEL', KEYS[1]); else return 0; end", Long.class);
    private static final DefaultRedisScript<Long> INCREMENT_ATTEMPTS_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('EXISTS', KEYS[1]) == 0 then return 0 end; " +
                    "local attempts = tonumber(redis.call('HGET', KEYS[1], 'attempts') or '0'); " +
                    "if attempts >= tonumber(ARGV[1]) then return 0 end; " +
                    "redis.call('HINCRBY', KEYS[1], 'attempts', 1); return 1", Long.class);
    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            "local hits = redis.call('INCR', KEYS[1]); " +
                    "if hits == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end; " +
                    "if hits <= tonumber(ARGV[2]) then return 1 else return 0 end", Long.class);

    public AlternativeLoginService(LoginProperties properties, ObjectProvider<JavaMailSender> mail,
            StringRedisTemplate redis, AppUserMapper users, PasswordEncoder encoder, DatabaseUserDetailsService details,
            SecurityContextRepository contexts, SessionAuthenticationStrategy sessions) {
        this.properties = properties; this.mail = mail; this.redis = redis; this.users = users;
        this.encoder = encoder; this.details = details; this.contexts = contexts; this.sessions = sessions;
    }

    public boolean emailAvailable() {
        return properties.isEmailEnabled() && !properties.getMailFrom().isBlank() && mail.getIfAvailable() != null;
    }

    public boolean registrationAvailable() {
        if (!emailAvailable()) return false;
        try (var connection = redis.getConnectionFactory().getConnection()) { return "PONG".equalsIgnoreCase(connection.ping()); }
        catch (Exception ignored) { return false; }
    }

    public void sendCode(String address, String ip) {
        sendCode(address, ip, "LOGIN");
    }

    public void sendCode(String address, String ip, String purpose) {
        if (!emailAvailable()) throw new BusinessException(503, "邮箱登录尚未配置，请使用账号密码登录");
        String email = normalizeEmail(address);
        // Redis counters are shared across application instances. Never trust X-Forwarded-For here.
        limit("ip:" + ip, 3600, 10);
        limit("email:" + email, 60, 1);
        // Send to new addresses too; a valid code will verify mailbox ownership before account creation.
        // Send the same response and email flow regardless of account state to avoid account enumeration.
        String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        String hash = encoder.encode(code);
        String challengeKey = challengeKey(email, purpose);
        executeScript(STORE_CODE_SCRIPT, List.of(challengeKey), hash, Long.toString(CODE_TTL.toSeconds()));
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getMailFrom()); message.setTo(email);
        String action = purpose.startsWith("BIND") ? "绑定邮箱" : purpose.startsWith("CURRENT") ? "更换邮箱" : purpose.equals("RESET") ? "重置密码" : purpose.equals("REGISTER") ? "注册账号" : "登录";
        message.setSubject("图灵算法社团 · " + action + "验证码");
        message.setText("你正在" + action + "，验证码为：" + code + "\n有效期为 5 分钟，请勿转发给他人。如非本人操作，请忽略此邮件。");
        try { mail.getObject().send(message); }
        catch (org.springframework.mail.MailException ex) {
            try { executeScript(DELETE_IF_MATCHES_SCRIPT, List.of(challengeKey), hash); }
            catch (BusinessException ignored) { /* The key expires automatically if Redis is temporarily unavailable. */ }
            throw new BusinessException(503, "邮件发送失败，请稍后重试或使用账号密码登录");
        }
    }

    private void limit(String key, int seconds, int maximum) {
        String bucket = "acmate:login:rate:" + seconds + ":" + digest(key);
        Long allowed = executeScript(RATE_LIMIT_SCRIPT, List.of(bucket), Integer.toString(seconds), Integer.toString(maximum));
        if (!Long.valueOf(1).equals(allowed))
            throw new BusinessException(429, "验证码请求过于频繁，请稍后重试");
    }

    @Transactional
    public LoginResponse emailLogin(String address, String code, HttpServletRequest request, HttpServletResponse response) {
        verifyCode(address, code, "LOGIN");
        String email = normalizeEmail(address);
        AppUser user = users.findByLoginEmail(email);
        if (user == null) user = createEmailAccount(email);
        if (!Integer.valueOf(1).equals(user.getStatus())) throw new DisabledException("账号已被禁用");
        return signIn(user.getUsername(), request, response);
    }

    public void verifyCode(String address, String code, String purpose) {
        if (code == null || !code.matches("[0-9]{6}")) throw invalidCode();
        if (!emailAvailable()) throw new BusinessException(503, "邮箱登录尚未配置，请使用账号密码登录");
        String email = normalizeEmail(address);
        String challengeKey = challengeKey(email, purpose);
        if (!Long.valueOf(1).equals(executeScript(INCREMENT_ATTEMPTS_SCRIPT, List.of(challengeKey), "5")))
            throw invalidCode();
        Object storedHash;
        try { storedHash = redis.opsForHash().get(challengeKey, "codeHash"); }
        catch (DataAccessException ex) { throw redisUnavailable(); }
        if (!(storedHash instanceof String hash) || !encoder.matches(code, hash)) throw invalidCode();
        if (!Long.valueOf(1).equals(executeScript(CONSUME_CODE_SCRIPT, List.of(challengeKey), hash)))
            throw invalidCode();
    }

    private AppUser createEmailAccount(String email) {
        for (int attempt = 0; attempt < 3; attempt++) {
            AppUser user = new AppUser();
            user.setUsername("mail_" + randomHex(12));
            user.setPasswordHash(encoder.encode(randomHex(32)));
            user.setNickname("邮箱用户_" + randomHex(6));
            user.setEmail(email);
            user.setIsAdmin(0);
            user.setStatus(1);
            try {
                if (users.insert(user) != 1 || user.getId() == null) {
                    throw new BusinessException(500, "自动注册失败，请稍后重试");
                }
                users.insertLoginEmail(user.getId(), email);
                return user;
            } catch (org.springframework.dao.DuplicateKeyException ex) {
                // A normal registration may have claimed this address after the code was sent.
                AppUser existing = users.findByLoginEmail(email);
                if (existing != null) return existing;
                if (attempt == 2) throw new BusinessException(409, "账号创建冲突，请重新获取验证码");
            }
        }
        throw new BusinessException(500, "自动注册失败，请稍后重试");
    }

    private String randomHex(int byteCount) {
        byte[] bytes = new byte[byteCount];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public LoginResponse signIn(String username, HttpServletRequest request, HttpServletResponse response) {
        var principal = details.loadUserByUsername(username);
        if (!principal.isEnabled()) throw new DisabledException("账号已被禁用");
        var authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
        sessions.onAuthentication(authentication, request, response);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        return new LoginResponse(principal.getId(), principal.getUsername(), principal.getNickname(), principal.getEmail(),
                principal.getAvatarUrl(), principal.getBio(), principal.isAdmin());
    }

    static String normalizeEmail(String email) { return email.strip().toLowerCase(Locale.ROOT); }
    private BusinessException invalidCode() { return new BusinessException(401, "验证码错误、已过期或尝试次数过多，请重新获取"); }
    private Long executeScript(DefaultRedisScript<Long> script, List<String> keys, String... arguments) {
        try { return redis.execute(script, keys, (Object[]) arguments); }
        catch (DataAccessException ex) { throw redisUnavailable(); }
    }
    private BusinessException redisUnavailable() { return new BusinessException(503, "邮箱验证码服务暂不可用，请稍后重试"); }
    private String challengeKey(String email, String purpose) {
        return "acmate:login:email-code:" + ("LOGIN".equals(purpose) ? "" : purpose + ":") + digest(email);
    }
    private String digest(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
