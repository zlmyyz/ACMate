package com.itnoduck.acmate.user.controller;

import com.itnoduck.acmate.user.dto.LoginRequest;
import com.itnoduck.acmate.user.dto.LoginResponse;
import com.itnoduck.acmate.user.dto.RegisterRequest;
import com.itnoduck.acmate.user.dto.RegisterResponse;
import com.itnoduck.acmate.user.service.UserAuthenticationService;
import com.itnoduck.acmate.user.service.UserRegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRegistrationService userRegistrationService;
    private final UserAuthenticationService userAuthenticationService;

    public AuthController(UserRegistrationService userRegistrationService,
                          UserAuthenticationService userAuthenticationService) {
        this.userRegistrationService = userRegistrationService;
        this.userAuthenticationService = userAuthenticationService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = userRegistrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest,
                                               HttpServletResponse httpResponse) {
        LoginResponse response = userAuthenticationService.login(request, httpRequest, httpResponse);
        return ResponseEntity.ok(response);
    }
}
