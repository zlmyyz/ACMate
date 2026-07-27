package com.itnoduck.acmate.security;

import tools.jackson.databind.ObjectMapper;
import com.itnoduck.acmate.common.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        int status;
        ApiError error;

        if (authException instanceof InternalAuthenticationServiceException) {
            status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            error = new ApiError(500, "服务器内部错误，请稍后再试");
        } else {
            status = HttpServletResponse.SC_UNAUTHORIZED;
            error = new ApiError(401, "未登录或登录已失效");
        }

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), error);
    }
}
