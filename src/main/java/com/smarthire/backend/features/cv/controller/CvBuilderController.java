package com.smarthire.backend.features.cv.controller;

import com.smarthire.backend.core.security.SecurityUtils;
import com.smarthire.backend.features.cv.dto.CvBuilderRequest;
import com.smarthire.backend.features.cv.dto.CvBuilderResponse;
import com.smarthire.backend.features.cv.service.CvBuilderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cv-builder")
@RequiredArgsConstructor
@Tag(name = "CV Builder", description = "APIs for Candidate CV Builder functionalities")
public class CvBuilderController {

    private final CvBuilderService cvBuilderService;

    @GetMapping
    @Operation(summary = "Get All CVs", description = "Retrieve all CV Builder data entries for the authenticated candidate")
    public ResponseEntity<List<CvBuilderResponse>> getAllCvBuilderData() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(cvBuilderService.getAllCvBuilderData(userId));
    }

    @PostMapping
    @Operation(summary = "Create New CV", description = "Create a new CV builder entry (never overwrites existing CVs)")
    public ResponseEntity<CvBuilderResponse> createCvBuilderData(@Valid @RequestBody CvBuilderRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(cvBuilderService.createCvBuilderData(userId, request));
    }

    @PutMapping("/{cvFileId}")
    @Operation(summary = "Update CV by CvFile ID", description = "Update an existing CV builder data for a specific CV file")
    public ResponseEntity<CvBuilderResponse> updateCvBuilderData(
            @PathVariable Long cvFileId,
            @Valid @RequestBody CvBuilderRequest request) {
        return ResponseEntity.ok(cvBuilderService.updateCvBuilderDataByCvFileId(cvFileId, request));
    }

    @GetMapping("/{cvFileId}")
    @Operation(summary = "Get CV Builder Data by CvFile ID", description = "Retrieve CV Builder data for a specific CV file")
    public ResponseEntity<CvBuilderResponse> getCvBuilderDataByCvFileId(@PathVariable Long cvFileId) {
        return ResponseEntity.ok(cvBuilderService.getCvBuilderDataByCvFileId(cvFileId));
    }
}
