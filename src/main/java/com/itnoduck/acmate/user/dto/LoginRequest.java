package com.itnoduck.acmate.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public class LoginRequest {

    @NotBlank
    @Size(min = 4, max = 32)
    @Pattern(regexp = "^[A-Za-z0-9_]{4,32}$")
    private String username;

    @NotBlank
    @Size(min = 8, max = 64)
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username == null ? null : username.strip().toLowerCase(Locale.ROOT);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "LoginRequest{username='" + username + "'}";
    }
}
