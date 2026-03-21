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

@RestController
@RequestMapping("/api/v1/cv-builder")
@RequiredArgsConstructor
@Tag(name = "CV Builder", description = "APIs for Candidate CV Builder functionalities")
public class CvBuilderController {

    private final CvBuilderService cvBuilderService;

    @GetMapping
    @Operation(summary = "Get CV Builder Data", description = "Retrieve existing CV Builder data for the authenticated candidate")
    public ResponseEntity<CvBuilderResponse> getCvBuilderData() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(cvBuilderService.getCvBuilderData(userId));
    }

    @PutMapping
    @Operation(summary = "Save or Update CV Builder Data", description = "Create or update CV builder json sections for the authenticated candidate")
    public ResponseEntity<CvBuilderResponse> saveCvBuilderData(@Valid @RequestBody CvBuilderRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(cvBuilderService.saveCvBuilderData(userId, request));
    }
}
