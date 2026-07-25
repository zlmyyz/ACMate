package com.itnoduck.acmate.user.service;

import com.itnoduck.acmate.user.dto.LoginRequest;
import com.itnoduck.acmate.user.dto.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface UserAuthenticationService {
    LoginResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);
}
