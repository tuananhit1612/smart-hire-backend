package com.smarthire.backend.features.application.service;

import com.smarthire.backend.features.application.dto.ApplicationDetailResponse;
import com.smarthire.backend.features.application.dto.ApplicationResponse;
import com.smarthire.backend.features.application.dto.ApplicationTrackingResponse;
import com.smarthire.backend.features.application.dto.ApplyJobRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ApplicationService {
    ApplicationResponse applyToJob(Long userId, ApplyJobRequest request);
    Page<ApplicationTrackingResponse> getCandidateApplications(Long userId, Pageable pageable);
    ApplicationDetailResponse getApplicationDetail(Long userId, Long applicationId);
}
