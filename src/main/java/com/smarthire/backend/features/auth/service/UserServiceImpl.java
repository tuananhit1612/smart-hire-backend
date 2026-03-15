package com.smarthire.backend.features.auth.service;

import com.smarthire.backend.core.exception.ResourceNotFoundException;
import com.smarthire.backend.core.security.SecurityUtils;
import com.smarthire.backend.features.auth.dto.UpdateProfileRequest;
import com.smarthire.backend.features.auth.dto.UserProfileResponse;
import com.smarthire.backend.features.auth.entity.User;
import com.smarthire.backend.features.auth.repository.UserRepository;
import com.smarthire.backend.infrastructure.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Override
    public UserProfileResponse getCurrentProfile() {
        User user = SecurityUtils.getCurrentUser();
        return toProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateCurrentProfile(UpdateProfileRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        user = userRepository.save(user);
        return toProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse uploadAvatar(MultipartFile file) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Xóa avatar cũ nếu có
        if (user.getAvatarUrl() != null) {
            fileStorageService.deleteFile(user.getAvatarUrl());
        }

        // Lưu avatar mới
        String relativePath = fileStorageService.storeImage(file, "avatars");
        user.setAvatarUrl(relativePath);
        user = userRepository.save(user);

        return toProfileResponse(user);
    }

    private UserProfileResponse toProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

