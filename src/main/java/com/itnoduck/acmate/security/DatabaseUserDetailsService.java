package com.itnoduck.acmate.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itnoduck.acmate.user.entity.AppUser;
import com.itnoduck.acmate.user.mapper.AppUserMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final AppUserMapper appUserMapper;

    public DatabaseUserDetailsService(AppUserMapper appUserMapper) {
        this.appUserMapper = appUserMapper;
    }

    @Override
    public AuthenticatedUser loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("Invalid username");
        }

        String normalized = username.strip().toLowerCase(Locale.ROOT);

        AppUser user = appUserMapper.selectOne(
                new LambdaQueryWrapper<AppUser>().eq(AppUser::getUsername, normalized));

        if (user == null) {
            throw new UsernameNotFoundException("Invalid username or password");
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (user.getIsAdmin() != null && user.getIsAdmin() == 1) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        boolean enabled = user.getStatus() != null && user.getStatus() == 1;

        return new AuthenticatedUser(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getNickname(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getIsAdmin() != null && user.getIsAdmin() == 1,
                enabled,
                Collections.unmodifiableList(authorities)
        );
    }
}
