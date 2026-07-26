package com.itnoduck.acmate.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public final class AuthenticatedUser implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String username;
    private final String passwordHash;
    private final String nickname;
    private final String email;
    private final String avatarUrl;
    private final String bio;
    private final boolean admin;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public AuthenticatedUser(Long id, String username, String passwordHash,
                             String nickname, String email, String avatarUrl, String bio,
                             boolean admin, boolean enabled,
                             Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
        this.admin = admin;
        this.enabled = enabled;
        this.authorities = Collections.unmodifiableCollection(authorities);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public Long getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public String getEmail() {
        return email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getBio() {
        return bio;
    }

    public boolean isAdmin() {
        return admin;
    }

    @Override
    public String toString() {
        return "AuthenticatedUser{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", admin=" + admin +
                ", enabled=" + enabled +
                ", authorities=" + authorities +
                '}';
    }
}
