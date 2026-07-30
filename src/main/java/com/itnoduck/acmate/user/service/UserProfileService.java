package com.itnoduck.acmate.user.service;

import com.itnoduck.acmate.user.dto.PublicUserProfileResponse;
import com.itnoduck.acmate.user.dto.UpdateProfileRequest;

public interface UserProfileService {
    PublicUserProfileResponse getProfile(long userId);
    void updateProfile(long userId, UpdateProfileRequest request);
    String updateAvatar(long userId, String filename, byte[] content);
}
