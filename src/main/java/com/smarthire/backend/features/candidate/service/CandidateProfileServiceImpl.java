package com.smarthire.backend.features.candidate.service;

import com.smarthire.backend.core.exception.BadRequestException;
import com.smarthire.backend.core.exception.ResourceNotFoundException;
import com.smarthire.backend.core.security.CustomUserDetails;
import com.smarthire.backend.features.auth.entity.User;
import com.smarthire.backend.features.candidate.dto.CandidateProfileRequest;
import com.smarthire.backend.features.candidate.dto.CandidateProfileResponse;
import com.smarthire.backend.features.candidate.entity.CandidateProfile;
import com.smarthire.backend.features.candidate.repository.CandidateProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateProfileServiceImpl implements CandidateProfileService {

    private final CandidateProfileRepository profileRepository;

    private User getCurrentUser() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userDetails.getUser();
    }

    @Override
    @Transactional(readOnly = true)
    public CandidateProfileResponse getMyProfile() {
        User currentUser = getCurrentUser();
        CandidateProfile profile = profileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate Profile", currentUser.getId()));
        
        return mapToResponse(profile, currentUser);
    }

    @Override
    @Transactional
    public CandidateProfileResponse createMyProfile(CandidateProfileRequest request) {
        User currentUser = getCurrentUser();
        
        if (profileRepository.existsByUserId(currentUser.getId())) {
            throw new BadRequestException("Profile already exists for this user. Use PUT to update.");
        }

        CandidateProfile newProfile = CandidateProfile.builder()
                .user(currentUser)
                .headline(request.getHeadline())
                .summary(request.getSummary())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .address(request.getAddress())
                .city(request.getCity())
                .yearsOfExperience(request.getYearsOfExperience())
                .jobLevel(request.getJobLevel())
                .build();

        newProfile = profileRepository.save(newProfile);
        log.info("Created profile for user {}", currentUser.getEmail());
        
        return mapToResponse(newProfile, currentUser);
    }

    @Override
    @Transactional
    public CandidateProfileResponse updateMyProfile(CandidateProfileRequest request) {
        User currentUser = getCurrentUser();
        
        CandidateProfile existingProfile = profileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate Profile", currentUser.getId()));

        existingProfile.setHeadline(request.getHeadline());
        existingProfile.setSummary(request.getSummary());
        // Only update if not null, or accept nulls if that's the intent. Here we overwrite with request data directly.
        existingProfile.setDateOfBirth(request.getDateOfBirth());
        existingProfile.setGender(request.getGender());
        existingProfile.setAddress(request.getAddress());
        existingProfile.setCity(request.getCity());
        existingProfile.setYearsOfExperience(request.getYearsOfExperience());
        existingProfile.setJobLevel(request.getJobLevel());

        existingProfile = profileRepository.save(existingProfile);
        log.info("Updated profile for user {}", currentUser.getEmail());
        
        return mapToResponse(existingProfile, currentUser);
    }

    private CandidateProfileResponse mapToResponse(CandidateProfile profile, User user) {
        return CandidateProfileResponse.builder()
                .id(profile.getId())
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .headline(profile.getHeadline())
                .summary(profile.getSummary())
                .dateOfBirth(profile.getDateOfBirth())
                .gender(profile.getGender())
                .address(profile.getAddress())
                .city(profile.getCity())
                .yearsOfExperience(profile.getYearsOfExperience())
                .jobLevel(profile.getJobLevel())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
