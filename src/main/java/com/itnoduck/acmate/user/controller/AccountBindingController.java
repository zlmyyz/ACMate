package com.itnoduck.acmate.user.controller;

import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.security.AuthenticatedUser;
import com.itnoduck.acmate.user.dto.LoginResponse;
import com.itnoduck.acmate.user.service.AccountSecurityService;
import com.itnoduck.acmate.user.service.AlternativeLoginService;
import com.itnoduck.acmate.user.service.GithubLoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

@RestController
public class AccountBindingController {
    @org.springframework.beans.factory.annotation.Autowired
    private com.itnoduck.acmate.security.ClientIpResolver clientIp;
    private final AlternativeLoginService email;
    private final GithubLoginService github;
    private final AlternativeLoginController oauth;
    private final AccountSecurityService accounts;
    public record SendCode(@NotBlank @Email @Size(max=128) String email, boolean current) {}
    public record VerifyEmail(@NotBlank @Email @Size(max=128) String email,
            @NotBlank @Pattern(regexp="[0-9]{6}") String code, String currentCode) {}
    public record Confirmation(@AssertTrue(message="请确认绑定账号") boolean confirm) {}
    public record LinkCode(@NotBlank @Pattern(regexp="[0-9]{6}") String code, @AssertTrue boolean confirm) {}
    public record ResetPassword(@NotBlank @Email @Size(max=128) String email,
            @NotBlank @Pattern(regexp="[0-9]{6}") String code, @NotBlank @Size(min=8,max=64) String password) {}
    public record PendingEmail(long userId,String previousEmail,String email,Long owner,long expires) implements Serializable {}
    public record ChangePassword(@NotBlank @Size(max=64) String currentPassword,
                                 @NotBlank @Size(min=8,max=64) String newPassword,
                                 @NotBlank @Size(min=8,max=64) String confirmPassword) {}

    public AccountBindingController(AlternativeLoginService email,GithubLoginService github,
                                     AlternativeLoginController oauth,AccountSecurityService accounts) {
        this.email=email; this.github=github; this.oauth=oauth; this.accounts=accounts;
    }

    @PostMapping("/api/auth/register/code")
    public Map<String,String> registrationCode(@Valid @RequestBody AlternativeLoginController.EmailRequest body,HttpServletRequest request) {
        email.sendCode(body.email(),clientIp.resolve(request),"REGISTER");
        return Map.of("message","注册验证码已发送，请查收邮箱");
    }

    @PostMapping("/api/auth/password/code")
    public Map<String,String> passwordCode(@Valid @RequestBody AlternativeLoginController.EmailRequest body,HttpServletRequest request) {
        // Same outward response for registered and unregistered addresses.
        email.sendCode(body.email(),clientIp.resolve(request),"RESET");
        return Map.of("message","验证码已发送；请使用已绑定账号的邮箱重置密码");
    }

    @PostMapping("/api/auth/password/reset")
    public Map<String,String> reset(@Valid @RequestBody ResetPassword body,HttpServletRequest request,HttpServletResponse response,
                                    org.springframework.security.core.Authentication authentication) {
        accounts.resetPassword(body.email(),body.code(),body.password());
        new org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler().logout(request,response,authentication);
        return Map.of("message","密码已重置，请使用新密码登录，其他会话已失效");
    }

    @GetMapping("/api/users/me/bindings")
    public AccountSecurityService.BindingInfo bindings(@AuthenticationPrincipal AuthenticatedUser user,HttpServletResponse response) {
        response.setHeader("Cache-Control","no-store");
        return accounts.bindings(user.getId());
    }

    @PostMapping("/api/users/me/password")
    public Map<String,String> changePassword(@AuthenticationPrincipal AuthenticatedUser user,@Valid @RequestBody ChangePassword body,
                                             HttpServletRequest request,HttpServletResponse response,
                                             org.springframework.security.core.Authentication authentication) {
        if (!body.newPassword().equals(body.confirmPassword())) throw new BusinessException(400,"两次输入的新密码不一致");
        accounts.changePassword(user.getId(),body.currentPassword(),body.newPassword());
        new org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler().logout(request,response,authentication);
        return Map.of("message","密码已修改，请使用新密码重新登录");
    }

    @PostMapping("/api/users/me/email/code")
    public Map<String,String> bindingCode(@AuthenticationPrincipal AuthenticatedUser user,@Valid @RequestBody SendCode body,HttpServletRequest request) {
        var current=accounts.active(user.getId());
        String address=body.current() ? current.getEmail() : body.email();
        if(address == null) throw new BusinessException(400,"尚未绑定邮箱");
        email.sendCode(address,clientIp.resolve(request),(body.current()?"CURRENT:":"BIND:")+user.getId());
        return Map.of("message","验证码已发送，请查收邮箱");
    }

    @PostMapping("/api/users/me/email/prepare")
    public Map<String,Object> prepareEmail(@AuthenticationPrincipal AuthenticatedUser user,@Valid @RequestBody VerifyEmail body,HttpServletRequest request) {
        var current=accounts.active(user.getId());
        String address=body.email().strip().toLowerCase(java.util.Locale.ROOT);
        if (current.getEmail()!=null && !current.getEmail().equalsIgnoreCase(address))
            email.verifyCode(current.getEmail(),body.currentCode(),"CURRENT:"+user.getId());
        email.verifyCode(address,body.code(),"BIND:"+user.getId());
        var owner=accounts.byEmail(address);
        if(owner!=null) accounts.active(owner.getId());
        Long ownerId=owner==null?null:owner.getId();
        request.getSession().setAttribute("pendingEmailBinding",new PendingEmail(user.getId(),current.getEmail(),address,ownerId,System.currentTimeMillis()+300_000));
        return Map.of("mergeRequired",ownerId!=null&&!ownerId.equals(user.getId()),"accountName",owner==null?"":owner.getNickname());
    }

    @PostMapping("/api/users/me/email/confirm")
    public LoginResponse confirmEmail(@AuthenticationPrincipal AuthenticatedUser user,@Valid @RequestBody Confirmation body,HttpServletRequest request,HttpServletResponse response) {
        var session=request.getSession();
        synchronized(session) {
            Object value=session.getAttribute("pendingEmailBinding");
            if(!(value instanceof PendingEmail pending)||pending.userId()!=user.getId()||pending.expires()<System.currentTimeMillis())
                throw new BusinessException(410,"邮箱验证已过期，请重新验证");
            if(!Objects.equals(accounts.active(user.getId()).getEmail(),pending.previousEmail())) throw new BusinessException(409,"绑定信息已改变，请重新验证");
            var result=accounts.bindEmail(user.getId(),pending.email(),pending.owner(),pending.previousEmail());
            session.removeAttribute("pendingEmailBinding");
            return email.signIn(result.username(),request,response);
        }
    }

    @PostMapping("/api/users/me/github/authorize")
    public Map<String,String> authorize(@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest request) {
        accounts.active(user.getId());
        return oauth.authorize(request,user.getId());
    }

    @PostMapping("/api/users/me/bindings/cancel")
    public Map<String,String> cancel(HttpServletRequest request) {
        var session=request.getSession(false);
        if(session!=null) { session.removeAttribute("pendingGithubBinding"); session.removeAttribute("pendingEmailBinding"); }
        return Map.of("message","已取消绑定");
    }

    @PostMapping("/api/users/me/github/unlink")
    public LoginResponse unlinkGithub(@AuthenticationPrincipal AuthenticatedUser user,@Valid @RequestBody Confirmation body,
                                      HttpServletRequest request,HttpServletResponse response) {
        var result=accounts.unlinkGithub(user.getId());
        var session=request.getSession();
        session.removeAttribute("githubLoginState");
        session.removeAttribute("pendingGithubBinding");
        return email.signIn(result.username(),request,response);
    }

    @GetMapping("/api/users/me/github/pending")
    public Map<String,Object> pendingBinding(@AuthenticationPrincipal AuthenticatedUser user,HttpServletRequest request,HttpServletResponse response) {
        response.setHeader("Cache-Control","no-store");
        var pending=AlternativeLoginController.pending(request);
        if(!user.getId().equals(pending.bindingUserId())) throw new BusinessException(403,"绑定会话不匹配");
        var owner=pending.expectedOwner()==null?null:accounts.active(pending.expectedOwner());
        return Map.of("githubLogin",pending.profile().login(),"githubName",pending.profile().name()==null?"":pending.profile().name(),
                "mergeRequired",owner!=null&&!owner.getId().equals(user.getId()),"accountName",owner==null?"":owner.getNickname());
    }

    @PostMapping("/api/users/me/github/confirm")
    public LoginResponse confirmGithub(@AuthenticationPrincipal AuthenticatedUser user,@Valid @RequestBody Confirmation body,HttpServletRequest request,HttpServletResponse response) {
        var session=request.getSession();
        synchronized(session) {
            var pending=AlternativeLoginController.pending(request);
            if(!user.getId().equals(pending.bindingUserId())) throw new BusinessException(403,"绑定会话不匹配");
            var result=accounts.bindGithub(user.getId(),pending.profile(),pending.expectedOwner());
            session.removeAttribute("pendingGithubBinding");
            return email.signIn(result.username(),request,response);
        }
    }

    @GetMapping("/api/auth/github/link")
    public Map<String,String> loginLink(HttpServletRequest request,HttpServletResponse response) {
        response.setHeader("Cache-Control","no-store");
        var pending=loginPending(request);
        String address=pending.email(); int at=address.indexOf('@');
        return Map.of("githubLogin",pending.profile().login(),"email",address.substring(0,1)+"***"+address.substring(at));
    }

    @PostMapping("/api/auth/github/link/code")
    public Map<String,String> loginLinkCode(HttpServletRequest request) {
        var pending=loginPending(request);
        email.sendCode(pending.email(),clientIp.resolve(request),"LINK:"+pending.profile().id());
        return Map.of("message","验证码已发送到已有邮箱账号");
    }

    @PostMapping("/api/auth/github/link/confirm")
    public LoginResponse loginLinkConfirm(@Valid @RequestBody LinkCode body,HttpServletRequest request,HttpServletResponse response) {
        var session=request.getSession();
        synchronized(session) {
            var pending=loginPending(request);
            email.verifyCode(pending.email(),body.code(),"LINK:"+pending.profile().id());
            var owner=accounts.byEmail(pending.email());
            if(owner==null) throw new BusinessException(409,"邮箱账号已变化，请重新登录");
            var result=accounts.bindGithub(owner.getId(),pending.profile(),pending.expectedOwner());
            session.removeAttribute("pendingGithubBinding");
            return email.signIn(result.username(),request,response);
        }
    }

    @PostMapping("/api/auth/github/link/skip")
    public LoginResponse skipLink(HttpServletRequest request,HttpServletResponse response) {
        var session=request.getSession();
        synchronized(session) {
            var pending=loginPending(request);
            String username=github.resolveUsername(pending.profile());
            session.removeAttribute("pendingGithubBinding");
            return email.signIn(username,request,response);
        }
    }

    private AlternativeLoginController.PendingGithub loginPending(HttpServletRequest request) {
        var pending=AlternativeLoginController.pending(request);
        if(pending.bindingUserId()!=null||pending.email()==null) throw new BusinessException(403,"授权用途不匹配");
        return pending;
    }
}
