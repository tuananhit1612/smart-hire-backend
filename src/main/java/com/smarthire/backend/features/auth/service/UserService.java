package com.smarthire.backend.features.auth.service;

import com.smarthire.backend.features.auth.dto.UpdateProfileRequest;
import com.smarthire.backend.features.auth.dto.UserProfileResponse;

public interface UserService {

    UserProfileResponse getCurrentProfile();

    UserProfileResponse updateCurrentProfile(UpdateProfileRequest request);
}
