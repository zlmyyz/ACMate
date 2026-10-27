package com.itnoduck.acmate.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Locale;

@Data
public class RegisterRequest {

    @NotBlank
    @Size(min = 4, max = 32)
    @Pattern(regexp = "^[A-Za-z0-9_]{4,32}$")
    private String username;

    @NotBlank
    @Size(min = 8, max = 64)
    private String password;

    @NotBlank
    @Size(min = 2, max = 32)
    private String nickname;

    @NotBlank(message = "邮箱不能为空")
    @Email
    @Size(max = 128)
    private String email;

    @NotBlank(message = "请输入邮箱验证码")
    @Pattern(regexp = "[0-9]{6}", message = "验证码必须为六位数字")
    private String emailCode;

    public void setUsername(String username) {
        this.username = username == null ? null : username.strip().toLowerCase(Locale.ROOT);
    }

    public void setNickname(String nickname) {
        this.nickname = nickname == null ? null : nickname.strip();
    }

    public void setEmail(String email) {
        if (email == null || email.isBlank()) {
            this.email = null;
        } else {
            this.email = email.strip().toLowerCase(Locale.ROOT);
        }
    }
}
