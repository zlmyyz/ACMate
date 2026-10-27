package com.itnoduck.acmate.user.service.impl;

import com.itnoduck.acmate.security.AuthenticatedUser;
import com.itnoduck.acmate.user.dto.LoginRequest;
import com.itnoduck.acmate.user.dto.LoginResponse;
import com.itnoduck.acmate.user.service.UserAuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.stereotype.Service;

@Service
public class UserAuthenticationServiceImpl implements UserAuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final com.itnoduck.acmate.security.PasswordLoginLimiter loginLimiter;
    private final com.itnoduck.acmate.security.ClientIpResolver clientIp;

    public UserAuthenticationServiceImpl(AuthenticationManager authenticationManager,
                                         SecurityContextRepository securityContextRepository,
                                         SessionAuthenticationStrategy sessionAuthenticationStrategy,
                                         com.itnoduck.acmate.security.PasswordLoginLimiter loginLimiter,
                                         com.itnoduck.acmate.security.ClientIpResolver clientIp) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.loginLimiter = loginLimiter;
        this.clientIp = clientIp;
    }

    @Override
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        loginLimiter.check(clientIp.resolve(httpRequest),request.getUsername());
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());

        Authentication authentication = authenticationManager.authenticate(token);

        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);

        sessionAuthenticationStrategy.onAuthentication(authentication, httpRequest, httpResponse);

        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getBio(),
                user.isAdmin()
        );
    }
}
