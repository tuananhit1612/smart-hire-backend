package com.smarthire.backend.features.candidate.controller;

import com.smarthire.backend.features.candidate.dto.EducationRequest;
import com.smarthire.backend.features.candidate.dto.EducationResponse;
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
@RequestMapping(ApiPaths.CANDIDATE_PROFILE + "/educations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CANDIDATE')")
@Tag(name = "Candidate Education", description = "CRUD for candidate education records")
public class CandidateEducationController {

    private final CandidateResumeService resumeService;

    @GetMapping
    @Operation(summary = "Get all educations", description = "Returns the education list of the current candidate")
    public ResponseEntity<ApiResponse<List<EducationResponse>>> getEducations() {
        return ResponseEntity.ok(ApiResponse.success("Educations retrieved successfully", resumeService.getEducations()));
    }

    @PostMapping
    @Operation(summary = "Add education", description = "Adds a new education record")
    public ResponseEntity<ApiResponse<EducationResponse>> createEducation(@Valid @RequestBody EducationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Education created successfully", resumeService.createEducation(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update education", description = "Updates an existing education record")
    public ResponseEntity<ApiResponse<EducationResponse>> updateEducation(@PathVariable Long id, @Valid @RequestBody EducationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Education updated successfully", resumeService.updateEducation(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete education", description = "Deletes an education record")
    public ResponseEntity<ApiResponse<Void>> deleteEducation(@PathVariable Long id) {
        resumeService.deleteEducation(id);
        return ResponseEntity.ok(ApiResponse.success("Education deleted successfully", null));
    }
}
