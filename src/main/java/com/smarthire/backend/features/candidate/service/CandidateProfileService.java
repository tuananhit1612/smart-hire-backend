package com.smarthire.backend.features.candidate.service;

import com.smarthire.backend.features.candidate.dto.CandidateProfileRequest;
import com.smarthire.backend.features.candidate.dto.CandidateProfileResponse;

public interface CandidateProfileService {

    /**
     * Get the candidate profile of the currently logged-in user.
     * @return CandidateProfileResponse
     */
    CandidateProfileResponse getMyProfile();

    /**
     * Create a new candidate profile for the currently logged-in user.
     * @param request profile data
     * @return CandidateProfileResponse
     */
    CandidateProfileResponse createMyProfile(CandidateProfileRequest request);

    /**
     * Update the candidate profile of the currently logged-in user.
     * @param request profile data
     * @return CandidateProfileResponse
     */
    CandidateProfileResponse updateMyProfile(CandidateProfileRequest request);
}
