package com.smarthire.backend.features.auth.service;

import com.smarthire.backend.features.auth.dto.UpdateProfileRequest;
import com.smarthire.backend.features.auth.dto.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    UserProfileResponse getCurrentProfile();

    UserProfileResponse updateCurrentProfile(UpdateProfileRequest request);

    UserProfileResponse uploadAvatar(MultipartFile file);
}
