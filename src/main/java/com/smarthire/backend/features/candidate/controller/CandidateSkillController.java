package com.smarthire.backend.features.candidate.controller;

import com.smarthire.backend.features.candidate.dto.SkillRequest;
import com.smarthire.backend.features.candidate.dto.SkillResponse;
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
@RequestMapping(ApiPaths.CANDIDATE_PROFILE + "/skills")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CANDIDATE')")
@Tag(name = "Candidate Skill", description = "CRUD for candidate skills")
public class CandidateSkillController {

    private final CandidateResumeService resumeService;

    @GetMapping
    @Operation(summary = "Get all skills", description = "Returns the skill list of the current candidate")
    public ResponseEntity<ApiResponse<List<SkillResponse>>> getSkills() {
        return ResponseEntity.ok(ApiResponse.success("Skills retrieved successfully", resumeService.getSkills()));
    }

    @PostMapping
    @Operation(summary = "Add skill", description = "Adds a new skill")
    public ResponseEntity<ApiResponse<SkillResponse>> createSkill(@Valid @RequestBody SkillRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Skill created successfully", resumeService.createSkill(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update skill", description = "Updates an existing skill")
    public ResponseEntity<ApiResponse<SkillResponse>> updateSkill(@PathVariable Long id, @Valid @RequestBody SkillRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Skill updated successfully", resumeService.updateSkill(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete skill", description = "Deletes a skill")
    public ResponseEntity<ApiResponse<Void>> deleteSkill(@PathVariable Long id) {
        resumeService.deleteSkill(id);
        return ResponseEntity.ok(ApiResponse.success("Skill deleted successfully", null));
    }
}
