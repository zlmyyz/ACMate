package com.itnoduck.acmate.user.controller;
import com.itnoduck.acmate.common.exception.BusinessException;
import com.itnoduck.acmate.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me/editor-settings")
public class EditorSettingsController {
    private final JdbcTemplate jdbc;
    public EditorSettingsController(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public record Settings(boolean autocomplete, String language){}
    public record Update(@NotNull Boolean autocomplete,@NotNull @Positive Long accountId,
                         @Pattern(regexp="CPP17|C17|JAVA17|PYTHON3|CSHARP|TYPESCRIPT|JAVASCRIPT|GO|RUST") String language){
        public Update(Boolean autocomplete, Long accountId){this(autocomplete,accountId,null);}
    }
    private void requireUser(long id){if(jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE id=? AND status=1",Long.class,id)==0)throw new BusinessException(401,"登录已失效，请重新登录");}
    @GetMapping public Settings get(@AuthenticationPrincipal AuthenticatedUser user){
        requireUser(user.getId());
        var values=jdbc.queryForList("SELECT autocomplete FROM user_editor_setting WHERE user_id=?",Integer.class,user.getId());
        var languages=jdbc.queryForList("SELECT preferred_language FROM user_editor_setting WHERE user_id=?",String.class,user.getId());
        return new Settings(values.isEmpty()||values.get(0)==1,languages.isEmpty()?"CPP17":languages.get(0));
    }
    @PutMapping public Settings put(@AuthenticationPrincipal AuthenticatedUser user,@Valid @RequestBody Update body){
        requireUser(user.getId());
        if(body.accountId().longValue()!=user.getId().longValue())throw new BusinessException(409,"当前账号已变化，请重新加载编辑器设置");
        if(body.language()==null){
            jdbc.update("INSERT INTO user_editor_setting(user_id,autocomplete) VALUES(?,?) ON DUPLICATE KEY UPDATE autocomplete=VALUES(autocomplete)",user.getId(),body.autocomplete());
        }else{
            jdbc.update("INSERT INTO user_editor_setting(user_id,autocomplete,preferred_language) VALUES(?,?,?) ON DUPLICATE KEY UPDATE autocomplete=VALUES(autocomplete),preferred_language=VALUES(preferred_language)",user.getId(),body.autocomplete(),body.language());
        }
        var languages=jdbc.queryForList("SELECT preferred_language FROM user_editor_setting WHERE user_id=?",String.class,user.getId());
        return new Settings(body.autocomplete(),languages.isEmpty()?"CPP17":languages.get(0));
    }
}
