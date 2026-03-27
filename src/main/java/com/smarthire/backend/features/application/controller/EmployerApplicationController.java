package com.smarthire.backend.features.application.controller;

import com.smarthire.backend.core.security.SecurityUtils;
import com.smarthire.backend.features.application.dto.ChangeStageRequest;
import com.smarthire.backend.features.application.dto.employer.*;
import com.smarthire.backend.features.application.service.EmployerApplicationService;
import com.smarthire.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/employer/jobs")
@RequiredArgsConstructor
@Tag(name = "Employer Applications", description = "APIs for Employer to manage and view applicants with AI Analysis")
public class EmployerApplicationController {

    private final EmployerApplicationService employerApplicationService;

    @GetMapping("/{jobId}/applicants")
    @Operation(summary = "Get list of applicants for a job", description = "Retrieves filtered and sorted list of applicants")
    public ResponseEntity<ApiResponse<ApplicantListResponse>> getApplicants(
            @PathVariable Long jobId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortBy,
            Pageable pageable) {
        Long employerId = SecurityUtils.getCurrentUserId();
        ApplicantListResponse response = employerApplicationService.getApplicantsByJob(jobId, employerId, search, sortBy, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{jobId}/applicants/{applicantId}")
    @Operation(summary = "Get full applicant details", description = "Retrieves an applicant's detail including AI matching and notes")
    public ResponseEntity<ApiResponse<EmployerApplicationResponse>> getApplicantDetail(
            @PathVariable Long jobId,
            @PathVariable Long applicantId) {
        Long employerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(employerApplicationService.getApplicantDetail(jobId, applicantId, employerId)));
    }

    @PatchMapping("/{jobId}/applicants/{applicantId}/stage")
    @Operation(summary = "Update applicant stage", description = "Changes an applicant's stage in the hiring pipeline")
    public ResponseEntity<ApiResponse<EmployerApplicationResponse>> updateStage(
            @PathVariable Long jobId,
            @PathVariable Long applicantId,
            @Valid @RequestBody ChangeStageRequest request) {
        Long employerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Success", employerApplicationService.updateStage(jobId, applicantId, employerId, request)));
    }

    @PostMapping("/{jobId}/applicants/{applicantId}/notes")
    @Operation(summary = "Add internal note", description = "Employer adds a private internal note for the applicant")
    public ResponseEntity<ApiResponse<EmployerApplicationNoteResponse>> addNote(
            @PathVariable Long jobId,
            @PathVariable Long applicantId,
            @Valid @RequestBody AddApplicationNoteRequest request) {
        Long employerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Note added", employerApplicationService.addNote(jobId, applicantId, employerId, request)));
    }

    @PostMapping("/{jobId}/applicants/{applicantId}/re-analyze")
    @Operation(summary = "Re-analyze applicant CV", description = "Triggers the AI engine to re-analyze the applicant's CV against the job description")
    public ResponseEntity<ApiResponse<AiAnalysisResponse>> reAnalyze(
            @PathVariable Long jobId,
            @PathVariable Long applicantId) {
        Long employerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Analysis complete", employerApplicationService.reAnalyzeApplicant(jobId, applicantId, employerId)));
    }
}
