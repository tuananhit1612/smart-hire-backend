package com.smarthire.backend.features.application.service;

import com.smarthire.backend.features.application.dto.ApplicationResponse;
import com.smarthire.backend.features.application.dto.ChangeStageRequest;

import java.util.List;

public interface ApplicationService {

    ApplicationResponse getApplicationById(Long id);

    List<ApplicationResponse> getApplicationsByJob(Long jobId, String stage);

    ApplicationResponse changeStage(Long applicationId, ChangeStageRequest request);
}
