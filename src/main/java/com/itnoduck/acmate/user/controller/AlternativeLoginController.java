package com.itnoduck.acmate.user.controller;

import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.user.dto.LoginResponse;
import com.itnoduck.acmate.user.dto.GithubProfile;
import com.itnoduck.acmate.user.service.AlternativeLoginService;
import com.itnoduck.acmate.user.service.GithubLoginService;
import com.itnoduck.acmate.user.service.AccountSecurityService;
import com.itnoduck.acmate.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AlternativeLoginController {
    @org.springframework.beans.factory.annotation.Autowired
    private com.itnoduck.acmate.security.ClientIpResolver clientIp;
    private final AlternativeLoginService login;
    private final GithubLoginService github;
    private final AccountSecurityService accounts;
    private record GithubState(String value, String verifier, long expires, Long bindingUserId) implements Serializable {}
    public record PendingGithub(GithubProfile profile, Long bindingUserId, Long expectedOwner, String email, long expires) implements Serializable {}
    public record EmailRequest(@NotBlank @Email @Size(max=128) String email) {}
    public record CodeRequest(@NotBlank @Email @Size(max=128) String email,
                              @NotBlank @Pattern(regexp="[0-9]{6}") String code) {}

    public AlternativeLoginController(AlternativeLoginService login, GithubLoginService github, AccountSecurityService accounts) {
        this.login = login; this.github = github; this.accounts=accounts;
    }

    @GetMapping("/methods")
    public Map<String, Boolean> methods(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return Map.of("email", login.emailAvailable(), "github", github.available(), "registration", login.registrationAvailable());
    }

    @PostMapping("/email/code")
    public Map<String, String> send(@Valid @RequestBody EmailRequest body, HttpServletRequest request) {
        login.sendCode(body.email(), clientIp.resolve(request));
        return Map.of("message", "若验证码邮件成功送达，请查收；首次使用邮箱登录会自动注册账号");
    }

    @PostMapping("/email/login")
    public LoginResponse emailLogin(@Valid @RequestBody CodeRequest body, HttpServletRequest request, HttpServletResponse response) {
        return login.emailLogin(body.email(), body.code(), request, response);
    }

    @PostMapping("/github/authorize")
    public Map<String, String> authorize(HttpServletRequest request) {
        return authorize(request, null);
    }

    public Map<String,String> authorize(HttpServletRequest request, Long bindingUserId) {
        String state = UUID.randomUUID().toString();
        String verifier = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        String url = github.authorizeUrl(state, verifier);
        request.getSession().removeAttribute("pendingGithubBinding");
        request.getSession().setAttribute("githubLoginState", new GithubState(state, verifier, System.currentTimeMillis() + 300_000, bindingUserId));
        return Map.of("url", url);
    }

    @GetMapping("/github/callback")
    public void callback(@RequestParam(required=false) String code, @RequestParam(required=false) String state,
                         HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
        var session = request.getSession(false);
        Object saved = null;
        if (session != null) synchronized (session) {
            saved = session.getAttribute("githubLoginState");
            session.removeAttribute("githubLoginState");
        }
        if (!(saved instanceof GithubState expected) || expected.expires() < System.currentTimeMillis() || state == null
                || !MessageDigest.isEqual(expected.value().getBytes(StandardCharsets.UTF_8), state.getBytes(StandardCharsets.UTF_8))
                || code == null || code.isBlank() || code.length() > 512) {
            redirectToFrontend(response, "/login?github=failed"); return;
        }
        try {
            GithubProfile profile = github.exchangeProfile(code, expected.verifier());
            Long owner=accounts.githubOwner(profile.id());
            if (expected.bindingUserId() != null) {
                var authentication=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)
                        || !expected.bindingUserId().equals(user.getId())) throw new BusinessException(401,"请重新登录后绑定");
                session.setAttribute("pendingGithubBinding",new PendingGithub(profile,user.getId(),owner,null,System.currentTimeMillis()+300_000));
                redirectToFrontend(response,"/settings/profile?github=pending"); return;
            }
            var candidate=profile.verifiedEmail() == null ? null : accounts.byEmail(profile.verifiedEmail());
            if (candidate != null && !candidate.getId().equals(owner)) {
                accounts.active(candidate.getId());
                session.setAttribute("pendingGithubBinding",new PendingGithub(profile,null,owner,candidate.getEmail(),System.currentTimeMillis()+300_000));
                redirectToFrontend(response,"/account-link"); return;
            }
            String username;
            try { username = github.resolveUsername(profile); }
            catch (org.springframework.dao.DuplicateKeyException ex) { username = github.resolveUsername(profile); }
            login.signIn(username, request, response);
            redirectToFrontend(response, "/");
        } catch (BusinessException | org.springframework.security.core.AuthenticationException ex) {
            redirectToFrontend(response, expected.bindingUserId() == null ? "/login?github=failed" : "/settings/profile?github=failed");
        }
    }

    public static PendingGithub pending(HttpServletRequest request) {
        var session=request.getSession(false);
        Object saved=session == null ? null : session.getAttribute("pendingGithubBinding");
        if (!(saved instanceof PendingGithub pending) || pending.expires()<System.currentTimeMillis())
            throw new BusinessException(410,"GitHub 授权已过期，请重新授权");
        return pending;
    }

    private void redirectToFrontend(HttpServletResponse response, String path) {
        // Servlet sendRedirect can expand relative paths using the proxy's internal Host (8080).
        // Keep Location relative so the browser resolves it on the public frontend origin (5173).
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", path);
    }
}
