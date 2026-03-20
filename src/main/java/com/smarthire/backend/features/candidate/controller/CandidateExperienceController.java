package com.smarthire.backend.features.candidate.controller;

import com.smarthire.backend.features.candidate.dto.ExperienceRequest;
import com.smarthire.backend.features.candidate.dto.ExperienceResponse;
import com.smarthire.backend.features.candidate.service.CandidateResumeService;
import com.smarthire.backend.shared.constants.ApiPaths;
import com.smarthire.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.CANDIDATE_PROFILE + "/experiences")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CANDIDATE')")
@Tag(name = "Candidate Experience", description = "CRUD for candidate work experience records")
public class CandidateExperienceController {

    private final CandidateResumeService resumeService;

    @GetMapping
    @Operation(summary = "Get all experiences", description = "Returns the experience list of the current candidate")
    public ResponseEntity<ApiResponse<List<ExperienceResponse>>> getExperiences() {
        return ResponseEntity.ok(ApiResponse.success("Experiences retrieved successfully", resumeService.getExperiences()));
    }

    @PostMapping
    @Operation(summary = "Add experience", description = "Adds a new work experience record")
    public ResponseEntity<ApiResponse<ExperienceResponse>> createExperience(@Valid @RequestBody ExperienceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Experience created successfully", resumeService.createExperience(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update experience", description = "Updates an existing experience record")
    public ResponseEntity<ApiResponse<ExperienceResponse>> updateExperience(@PathVariable Long id, @Valid @RequestBody ExperienceRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Experience updated successfully", resumeService.updateExperience(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete experience", description = "Deletes an experience record")
    public ResponseEntity<ApiResponse<Void>> deleteExperience(@PathVariable Long id) {
        resumeService.deleteExperience(id);
        return ResponseEntity.ok(ApiResponse.success("Experience deleted successfully", null));
    }
}
