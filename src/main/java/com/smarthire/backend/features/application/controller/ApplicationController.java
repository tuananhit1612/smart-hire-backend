package com.smarthire.backend.features.application.controller;

import com.smarthire.backend.core.security.SecurityUtils;
import com.smarthire.backend.features.application.dto.ApplicationDetailResponse;
import com.smarthire.backend.features.application.dto.ApplicationResponse;
import com.smarthire.backend.features.application.dto.ApplicationTrackingResponse;
import com.smarthire.backend.features.application.dto.ApplyJobRequest;
import com.smarthire.backend.features.application.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Tag(name = "Applications", description = "APIs for Job Applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping("/apply")
    @Operation(summary = "Apply to a Job", description = "Candidate submits an application utilizing a specific CV")
    public ResponseEntity<ApplicationResponse> applyToJob(@Valid @RequestBody ApplyJobRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(applicationService.applyToJob(userId, request));
    }

    @GetMapping
    @Operation(summary = "Get Candidate Applications", description = "Retrieves a paginated list of jobs the candidate has applied for")
    public ResponseEntity<Page<ApplicationTrackingResponse>> getCandidateApplications(Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(applicationService.getCandidateApplications(userId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Application Detail", description = "Retrieves the application details along with its stage history")
    public ResponseEntity<ApplicationDetailResponse> getApplicationDetail(@PathVariable("id") Long applicationId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(applicationService.getApplicationDetail(userId, applicationId));
    }
}
