package com.smarthire.backend.features.application.controller;

import com.smarthire.backend.core.security.SecurityUtils;
import com.smarthire.backend.features.application.dto.*;
import com.smarthire.backend.features.application.service.ApplicationService;
import com.smarthire.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import com.smarthire.backend.shared.constants.ApiPaths;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.APPLICATIONS)
@RequiredArgsConstructor
@Tag(name = "Applications", description = "APIs for Job Applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    // ── Candidate APIs (BE020 & BE021) ──

    @PostMapping("/apply")
    @Operation(summary = "Apply to a Job", description = "Candidate submits an application utilizing a specific CV")
    public ResponseEntity<ApplicationResponse> applyToJob(@Valid @RequestBody ApplyJobRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(applicationService.applyToJob(userId, request));
    }

    @GetMapping
    @Operation(summary = "Get Candidate Applications (Paginated)", description = "Retrieves a paginated list of jobs the candidate has applied for")
    public ResponseEntity<Page<ApplicationTrackingResponse>> getCandidateApplications(Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(applicationService.getCandidateApplications(userId, pageable));
    }

    @GetMapping("/me")
    @Operation(summary = "Get My Applications (Flat List)", description = "Retrieves ALL applications for the current candidate as a flat list")
    public ResponseEntity<List<ApplicationTrackingResponse>> getMyApplications() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(applicationService.getCandidateApplicationsList(userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Withdraw Application", description = "Candidate withdraws an application (deletes it)")
    public ResponseEntity<Void> withdrawApplication(@PathVariable("id") Long applicationId) {
        Long userId = SecurityUtils.getCurrentUserId();
        applicationService.withdrawApplication(userId, applicationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Application Detail", description = "Retrieves the application details along with its stage history")
    public ResponseEntity<ApplicationDetailResponse> getApplicationDetail(@PathVariable("id") Long applicationId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(applicationService.getApplicationDetail(userId, applicationId));
    }

    // ── Management APIs (develop) ──

    @GetMapping("/job/{jobId}")
    @Operation(summary = "Get Applications by Job", description = "HR/Admin retrieves all applications for a specific job, optionally filtered by stage")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getApplicationsByJob(
            @PathVariable Long jobId,
            @RequestParam(required = false) String stage) {
        List<ApplicationResponse> response = applicationService.getApplicationsByJob(jobId, stage);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/stage")
    @Operation(summary = "Change Application Stage", description = "HR/Admin updates the stage of an application and records history")
    public ResponseEntity<ApiResponse<ApplicationResponse>> changeStage(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStageRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        ApplicationResponse response = applicationService.changeStage(id, userId, request);
        return ResponseEntity.ok(ApiResponse.success("Application stage updated successfully", response));
    }
}
