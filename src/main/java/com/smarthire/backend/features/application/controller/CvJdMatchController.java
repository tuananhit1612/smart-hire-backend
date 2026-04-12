package com.smarthire.backend.features.application.controller;

import com.smarthire.backend.features.application.dto.CvJdMatchResponse;
import com.smarthire.backend.features.application.dto.TriggerMatchRequest;
import com.smarthire.backend.features.application.service.CvJdMatchService;
import com.smarthire.backend.shared.constants.ApiPaths;
import com.smarthire.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CV-JD Matching Controller (AI003).
 * Cung cấp endpoint trigger matching, xem kết quả, liệt kê theo job.
 *
 * Endpoints:
 *   POST   /api/ai/cv-jd-match              → Trigger matching
 *   GET    /api/ai/cv-jd-match/{appId}       → Xem kết quả
 *   GET    /api/ai/cv-jd-match/job/{jobId}   → Liệt kê theo job
 *   POST   /api/ai/cv-jd-match/{appId}/re-analyze → Force re-analyze
 */
@RestController
@RequestMapping(ApiPaths.AI + "/cv-jd-match")
@RequiredArgsConstructor
@Tag(name = "CV-JD Matching", description = "AI-powered CV vs Job Description matching & scoring APIs")
public class CvJdMatchController {

    private final CvJdMatchService cvJdMatchService;

    /**
     * Trigger AI matching cho một application.
     * Nếu đã có result và forceReAnalyze=false → trả result cũ.
     */
    @PostMapping
    @Operation(summary = "Trigger CV-JD matching",
            description = "Triggers AI engine to analyze CV against Job Description. Returns cached result if already analyzed and forceReAnalyze is false.")
    public ResponseEntity<ApiResponse<CvJdMatchResponse>> triggerMatch(
            @Valid @RequestBody TriggerMatchRequest request) {
        boolean force = request.getForceReAnalyze() != null && request.getForceReAnalyze();
        CvJdMatchResponse response = cvJdMatchService.triggerMatch(request.getApplicationId(), force);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("CV-JD matching completed", response));
    }

    /**
     * Lấy kết quả matching cho một application.
     */
    @GetMapping("/{applicationId}")
    @Operation(summary = "Get match result",
            description = "Retrieves the AI matching result for a specific application. Returns 404 if not yet analyzed.")
    public ResponseEntity<ApiResponse<CvJdMatchResponse>> getMatchResult(
            @PathVariable Long applicationId) {
        CvJdMatchResponse response = cvJdMatchService.getMatchResult(applicationId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Lấy tất cả kết quả matching theo job.
     */
    @GetMapping("/job/{jobId}")
    @Operation(summary = "Get all match results for a job",
            description = "Retrieves all AI matching results for all applications under a specific job posting.")
    public ResponseEntity<ApiResponse<List<CvJdMatchResponse>>> getMatchResultsByJob(
            @PathVariable Long jobId) {
        List<CvJdMatchResponse> responses = cvJdMatchService.getMatchResultsByJob(jobId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Force re-analyze matching cho một application.
     * Xóa result cũ rồi chạy lại AI.
     */
    @PostMapping("/{applicationId}/re-analyze")
    @Operation(summary = "Re-analyze CV-JD matching",
            description = "Deletes existing AI result and triggers a fresh analysis. Use when CV or JD has been updated.")
    public ResponseEntity<ApiResponse<CvJdMatchResponse>> reAnalyze(
            @PathVariable Long applicationId) {
        CvJdMatchResponse response = cvJdMatchService.triggerMatch(applicationId, true);
        return ResponseEntity.ok(ApiResponse.success("Re-analysis completed", response));
    }
}
