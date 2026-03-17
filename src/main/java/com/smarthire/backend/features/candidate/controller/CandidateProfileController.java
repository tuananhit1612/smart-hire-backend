package com.smarthire.backend.features.candidate.controller;

import com.smarthire.backend.features.candidate.dto.CandidateProfileRequest;
import com.smarthire.backend.features.candidate.dto.CandidateProfileResponse;
import com.smarthire.backend.features.candidate.service.CandidateProfileService;
import com.smarthire.backend.shared.constants.ApiPaths;
import com.smarthire.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.CANDIDATE_PROFILE)
@RequiredArgsConstructor
@PreAuthorize("hasRole('CANDIDATE')")
@Tag(name = "Candidate Profile", description = "Endpoints for managing the Candidate profile (My Profile)")
public class CandidateProfileController {

    private final CandidateProfileService profileService;

    @GetMapping
    @Operation(summary = "Get my candidate profile", description = "Retrieves the profile of the currently logged-in candidate")
    public ResponseEntity<ApiResponse<CandidateProfileResponse>> getMyProfile() {
        CandidateProfileResponse response = profileService.getMyProfile();
        return ResponseEntity.ok(ApiResponse.success("Candidate Profile retrieved successfully", response));
    }

    @PostMapping
    @Operation(summary = "Create candidate profile", description = "Creates a profile for the currently logged-in candidate")
    public ResponseEntity<ApiResponse<CandidateProfileResponse>> createMyProfile(
            @Valid @RequestBody CandidateProfileRequest request) {
        CandidateProfileResponse response = profileService.createMyProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Candidate Profile created successfully", response));
    }

    @PutMapping
    @Operation(summary = "Update candidate profile", description = "Updates the profile of the currently logged-in candidate")
    public ResponseEntity<ApiResponse<CandidateProfileResponse>> updateMyProfile(
            @Valid @RequestBody CandidateProfileRequest request) {
        CandidateProfileResponse response = profileService.updateMyProfile(request);
        return ResponseEntity.ok(ApiResponse.success("Candidate Profile updated successfully", response));
    }
}
