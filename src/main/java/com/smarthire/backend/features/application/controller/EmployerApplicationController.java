package com.smarthire.backend.features.application.controller;

import com.smarthire.backend.core.security.SecurityUtils;
import com.smarthire.backend.features.application.dto.ChangeStageRequest;
import com.smarthire.backend.features.application.dto.employer.*;
import com.smarthire.backend.features.application.service.AiFilterService;
import com.smarthire.backend.features.application.service.EmployerApplicationService;
import com.smarthire.backend.shared.constants.ApiPaths;
import com.smarthire.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.BASE + "/employer/jobs")
@RequiredArgsConstructor
@Tag(name = "Employer Applications", description = "APIs for Employer to manage and view applicants with AI Analysis")
public class EmployerApplicationController {

    private final EmployerApplicationService employerApplicationService;
    private final AiFilterService aiFilterService;

    @GetMapping("/applications/all")
    @Operation(summary = "Get all applicants across all jobs", description = "Retrieves a list of all applicants for the current employer")
    public ResponseEntity<ApiResponse<List<EmployerApplicationResponse>>> getAllApplicants() {
        Long employerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(employerApplicationService.getAllApplicantsForEmployer(employerId)));
    }

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

    // ═══════════════════════════════════════════════════════════
    //  SMART CV FILTERING — AI-powered batch filtering
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/{jobId}/smart-filter")
    @Operation(summary = "Run AI Smart Filter", description = "Triggers 2-phase AI filtering (pre-filter + deep evaluation) on all applicants of a job")
    public ResponseEntity<ApiResponse<FilterSessionResponse>> runSmartFilter(
            @PathVariable Long jobId,
            @RequestBody(required = false) RunFilterRequest conditions) {
        Long employerId = SecurityUtils.getCurrentUserId();
        FilterSessionResponse result = aiFilterService.runFilter(jobId, employerId, conditions);
        return ResponseEntity.ok(ApiResponse.success("Smart filter completed", result));
    }

    @GetMapping("/{jobId}/smart-filter/{sessionId}")
    @Operation(summary = "Get filter session result", description = "Retrieves the result of a specific AI filter run")
    public ResponseEntity<ApiResponse<FilterSessionResponse>> getFilterSession(
            @PathVariable Long jobId,
            @PathVariable Long sessionId) {
        Long employerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(aiFilterService.getFilterSession(sessionId, employerId)));
    }

    @GetMapping("/{jobId}/smart-filter/history")
    @Operation(summary = "Get filter history", description = "Retrieves all past AI filter runs for a job")
    public ResponseEntity<ApiResponse<List<FilterSessionResponse>>> getFilterHistory(
            @PathVariable Long jobId) {
        Long employerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(aiFilterService.getFilterHistory(jobId, employerId)));
    }
}
