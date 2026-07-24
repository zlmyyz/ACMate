package com.itnoduck.acmate.user.service;

import com.itnoduck.acmate.user.dto.RegisterRequest;
import com.itnoduck.acmate.user.dto.RegisterResponse;

public interface UserRegistrationService {
    RegisterResponse register(RegisterRequest request);
}
