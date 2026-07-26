package com.itnoduck.acmate.user.service;

import com.itnoduck.acmate.user.dto.UpdateProfileRequest;
import com.itnoduck.acmate.user.dto.UserProfileResponse;

public interface UserProfileService {
    UserProfileResponse getProfile(long userId);
    void updateProfile(long userId, UpdateProfileRequest request);
    String updateAvatar(long userId, String filename, byte[] content);
}
