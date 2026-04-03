package com.smarthire.backend.features.application.controller;

import com.smarthire.backend.features.application.dto.ApplyRequest;
import com.smarthire.backend.features.application.dto.ApplicationResponse;
import com.smarthire.backend.features.application.dto.ChangeStageRequest;
import com.smarthire.backend.features.application.service.ApplicationService;
import com.smarthire.backend.shared.constants.ApiPaths;
import com.smarthire.backend.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.APPLICATIONS)
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    // ── Candidate: apply to a job ──

    @PostMapping
    public ResponseEntity<ApiResponse<ApplicationResponse>> apply(
            @Valid @RequestBody ApplyRequest request) {
        ApplicationResponse response = applicationService.apply(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Application submitted successfully", response));
    }

    // ── Candidate: list my applications ──

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getMyApplications() {
        List<ApplicationResponse> responses = applicationService.getMyApplications();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    // ── Candidate: withdraw an application ──

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> withdrawApplication(@PathVariable Long id) {
        applicationService.withdrawApplication(id);
        return ResponseEntity.noContent().build();
    }

    // ── Existing endpoints ──

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApplicationResponse>> getApplicationById(@PathVariable Long id) {
        ApplicationResponse response = applicationService.getApplicationById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getApplicationsByJob(
            @PathVariable Long jobId,
            @RequestParam(required = false) String stage) {
        List<ApplicationResponse> responses = applicationService.getApplicationsByJob(jobId, stage);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PatchMapping("/{id}/stage")
    public ResponseEntity<ApiResponse<ApplicationResponse>> changeStage(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStageRequest request) {
        ApplicationResponse response = applicationService.changeStage(id, request);
        return ResponseEntity.ok(ApiResponse.success("Stage updated successfully", response));
    }
}
