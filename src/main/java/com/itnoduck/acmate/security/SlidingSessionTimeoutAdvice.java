package com.itnoduck.acmate.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

/** Renews authenticated servlet sessions and their browser cookie after controller activity. */
@Component
public class SlidingSessionTimeoutAdvice implements MethodInterceptor {

    private static final Duration SESSION_TTL = Duration.ofHours(1);
    private static final int SESSION_TIMEOUT_SECONDS = (int) SESSION_TTL.toSeconds();

    private final String cookieName;
    private final String cookiePath;
    private final String sameSite;
    private final boolean httpOnly;
    private final boolean secure;

    public SlidingSessionTimeoutAdvice(
            @Value("${server.servlet.session.cookie.name:JSESSIONID}") String cookieName,
            @Value("${server.servlet.session.cookie.path:/}") String cookiePath,
            @Value("${server.servlet.session.cookie.same-site:Lax}") String sameSite,
            @Value("${server.servlet.session.cookie.http-only:true}") boolean httpOnly,
            @Value("${server.servlet.session.cookie.secure:false}") boolean secure) {
        this.cookieName = cookieName;
        this.cookiePath = cookiePath == null || cookiePath.isBlank() ? "/" : cookiePath;
        this.sameSite = sameSite;
        this.httpOnly = httpOnly;
        this.secure = secure;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        try {
            return invocation.proceed();
        } finally {
            renewCurrentSession();
        }
    }

    private void renewCurrentSession() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return;
        }

        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) return;

        HttpServletRequest request = servletAttributes.getRequest();
        HttpServletResponse response = servletAttributes.getResponse();
        if (response == null) return;

        HttpSession session = request.getSession(false);
        if (session == null) return;

        try {
            session.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);
            if (response.isCommitted()) return;

            ResponseCookie renewedCookie = ResponseCookie.from(cookieName, session.getId())
                    .httpOnly(httpOnly)
                    .secure(secure || request.isSecure())
                    .path(cookiePath)
                    .sameSite(sameSite)
                    .maxAge(SESSION_TTL)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, renewedCookie.toString());
        } catch (IllegalStateException ignored) {
            // Logout may invalidate the session while the controller is executing.
        }
    }
}
