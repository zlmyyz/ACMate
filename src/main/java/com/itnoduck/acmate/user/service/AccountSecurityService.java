package com.itnoduck.acmate.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import com.itnoduck.acmate.user.dto.GithubProfile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountSecurityService {
    private final AppUserMapper users;
    private final JdbcTemplate jdbc;
    private final AlternativeLoginService email;
    private final PasswordEncoder encoder;
    private final AccountMergeService merger;
    public record BindingInfo(String email, boolean githubBound, String githubLogin, String githubName) {}
    public record BindingResult(String username, Long userId) {}

    public AccountSecurityService(AppUserMapper users, JdbcTemplate jdbc, AlternativeLoginService email,
                                  PasswordEncoder encoder, AccountMergeService merger) {
        this.users=users; this.jdbc=jdbc; this.email=email; this.encoder=encoder; this.merger=merger;
    }

    public AppUser active(long id) {
        AppUser user = users.selectById(id);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) throw new BusinessException(403,"账号不可用，请重新登录");
        return user;
    }

    public AppUser byEmail(String address) {
        return users.findByLoginEmail(AlternativeLoginService.normalizeEmail(address));
    }

    public Long githubOwner(long githubId) {
        var ids=jdbc.queryForList("SELECT user_id FROM user_login_identity WHERE provider='GITHUB' AND identifier=?",Long.class,githubId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    public BindingInfo bindings(long userId) {
        var user=active(userId);
        var rows=jdbc.queryForList("SELECT login_name AS github_login,display_name AS github_name FROM user_login_identity WHERE provider='GITHUB' AND user_id=?",userId);
        return new BindingInfo(user.getEmail(), !rows.isEmpty(), rows.isEmpty() ? null : (String)rows.get(0).get("github_login"),
                rows.isEmpty() ? null : (String)rows.get(0).get("github_name"));
    }

    @Transactional
    public void resetPassword(String address, String code, String password) {
        email.verifyCode(address,code,"RESET");
        jdbc.queryForList("SELECT user_id FROM user_login_identity WHERE provider='EMAIL' AND identifier=? FOR UPDATE",AlternativeLoginService.normalizeEmail(address));
        AppUser user=byEmail(address);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) throw new BusinessException(400,"无法重置密码，请确认邮箱或联系管理员");
        jdbc.update("UPDATE app_user SET password_hash=? WHERE id=?",encoder.encode(password),user.getId());
        AccountMergeService.afterCommit(() -> merger.expireSessions(user.getId()));
    }

    @Transactional
    public void changePassword(long userId, String currentPassword, String newPassword) {
        lockAccounts(userId, null);
        AppUser user = active(userId);
        if (currentPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72 || !encoder.matches(currentPassword, user.getPasswordHash()))
            throw new BusinessException(400, "原密码不正确，请重新输入；忘记密码可通过邮箱重置");
        if (newPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new BusinessException(400, "新密码过长，请减少字符后重试");
        if (encoder.matches(newPassword, user.getPasswordHash()))
            throw new BusinessException(400, "新密码不能与原密码相同");
        if (jdbc.update("UPDATE app_user SET password_hash=? WHERE id=? AND status=1", encoder.encode(newPassword), userId) != 1)
            throw new BusinessException(409, "账号状态已变化，请重新登录后重试");
        AccountMergeService.afterCommit(() -> merger.expireSessions(userId));
    }

    @Transactional
    public BindingResult bindEmail(long currentId, String address, Long expectedOwner, String expectedCurrentEmail) {
        lockAccounts(currentId, expectedOwner);
        AppUser current=active(currentId);
        if(!java.util.Objects.equals(current.getEmail(),expectedCurrentEmail)) throw new BusinessException(409,"原邮箱已变化，请重新验证");
        String normalized=AlternativeLoginService.normalizeEmail(address);
        AppUser owner=byEmail(normalized);
        Long ownerId=owner == null ? null : owner.getId();
        if (!java.util.Objects.equals(ownerId,expectedOwner)) throw new BusinessException(409,"邮箱账号已变化，请重新验证");
        if (ownerId != null && ownerId != currentId) {
            if (current.getEmail() != null && !current.getEmail().equalsIgnoreCase(normalized))
                throw new BusinessException(409,"两个账号均已绑定邮箱，请联系管理员处理");
            merger.merge(currentId,ownerId);
            return new BindingResult(owner.getUsername(),ownerId);
        }
        users.bindLoginEmail(currentId, normalized);
        jdbc.update("UPDATE app_user SET email=? WHERE id=?",normalized,currentId);
        return new BindingResult(current.getUsername(),currentId);
    }

    @Transactional
    public BindingResult bindGithub(long currentId, GithubProfile profile, Long expectedOwner) {
        lockAccounts(currentId, expectedOwner);
        AppUser current=active(currentId);
        Long ownerId=githubOwner(profile.id());
        if (!java.util.Objects.equals(ownerId,expectedOwner)) throw new BusinessException(409,"GitHub 绑定已变化，请重新授权");
        var existing=jdbc.queryForList("SELECT identifier FROM user_login_identity WHERE provider='GITHUB' AND user_id=?",Long.class,currentId);
        if (!existing.isEmpty() && !existing.get(0).equals(profile.id())) throw new BusinessException(409,"当前账号已绑定另一个 GitHub");
        long target=currentId;
        if (ownerId != null && ownerId != currentId) {
            var owner=active(ownerId);
            if (current.getEmail() == null && owner.getEmail() != null) { merger.merge(currentId,ownerId); target=ownerId; }
            else merger.merge(ownerId,currentId);
        }
        if (ownerId == null) jdbc.update("INSERT INTO user_login_identity(identifier,user_id,login_name,display_name,provider) VALUES(?,?,?,?,'GITHUB')",profile.id(),target,profile.login(),profile.name());
        else jdbc.update("UPDATE user_login_identity SET login_name=?,display_name=? WHERE provider='GITHUB' AND identifier=?",profile.login(),profile.name(),profile.id());
        var retained=active(target);
        return new BindingResult(retained.getUsername(),target);
    }

    private void lockAccounts(long currentId, Long owner) {
        jdbc.queryForList("SELECT id FROM app_user WHERE id IN (?,?) ORDER BY id FOR UPDATE",currentId,owner==null?currentId:owner);
    }

    @Transactional
    public BindingResult unlinkGithub(long userId) {
        lockAccounts(userId, null);
        AppUser current = active(userId);
        if (current.getEmail() == null || current.getEmail().isBlank())
            throw new BusinessException(409, "请先绑定邮箱，再解绑 GitHub，以便继续登录账号");
        if (!email.emailAvailable())
            throw new BusinessException(503, "邮箱登录暂不可用，请稍后再解绑 GitHub");
        if (jdbc.update("DELETE FROM user_login_identity WHERE provider='GITHUB' AND user_id=?", userId) != 1)
            throw new BusinessException(409, "当前账号未绑定 GitHub，请刷新绑定信息");
        AccountMergeService.afterCommit(() -> merger.expireSessions(userId));
        return new BindingResult(current.getUsername(), userId);
    }
}
