package com.smarthire.backend.features.candidate.controller;

import com.smarthire.backend.features.candidate.dto.ProjectRequest;
import com.smarthire.backend.features.candidate.dto.ProjectResponse;
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
@RequestMapping(ApiPaths.CANDIDATE_PROFILE + "/projects")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CANDIDATE')")
@Tag(name = "Candidate Project", description = "CRUD for candidate projects")
public class CandidateProjectController {

    private final CandidateResumeService resumeService;

    @GetMapping
    @Operation(summary = "Get all projects", description = "Returns the project list of the current candidate")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getProjects() {
        return ResponseEntity.ok(ApiResponse.success("Projects retrieved successfully", resumeService.getProjects()));
    }

    @PostMapping
    @Operation(summary = "Add project", description = "Adds a new project")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(@Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Project created successfully", resumeService.createProject(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update project", description = "Updates an existing project")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(@PathVariable Long id, @Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Project updated successfully", resumeService.updateProject(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete project", description = "Deletes a project")
    public ResponseEntity<ApiResponse<Void>> deleteProject(@PathVariable Long id) {
        resumeService.deleteProject(id);
        return ResponseEntity.ok(ApiResponse.success("Project deleted successfully", null));
    }
}
