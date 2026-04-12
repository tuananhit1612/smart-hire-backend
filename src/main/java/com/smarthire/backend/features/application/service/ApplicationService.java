package com.smarthire.backend.features.application.service;

import com.smarthire.backend.features.application.dto.ApplyJobRequest;
import com.smarthire.backend.features.application.dto.ApplicationResponse;
import com.smarthire.backend.features.application.dto.ChangeStageRequest;

import com.smarthire.backend.features.application.dto.ApplicationTrackingResponse;

import java.util.List;

public interface ApplicationService {

    ApplicationResponse apply(ApplyJobRequest request);

    ApplicationResponse getApplicationById(Long id);

    List<ApplicationResponse> getApplicationsByJob(Long jobId, String stage);

    ApplicationResponse changeStage(Long applicationId, ChangeStageRequest request);

    List<ApplicationTrackingResponse> getMyApplications();

    void withdrawApplication(Long applicationId);
}
