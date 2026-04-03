package com.smarthire.backend.features.onboarding.controller;

import com.smarthire.backend.core.security.CustomUserDetails;
import com.smarthire.backend.features.onboarding.dto.OnboardingDocumentResponse;
import com.smarthire.backend.features.onboarding.enums.DocumentType;
import com.smarthire.backend.features.onboarding.enums.VerificationStatus;
import com.smarthire.backend.features.onboarding.service.OnboardingDocumentService;
import com.smarthire.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@RestController
@RequestMapping("/api/onboarding-documents")
@RequiredArgsConstructor
@Tag(name = "Onboarding Documents", description = "Quản lý hồ sơ trúng tuyển và kiểm duyệt bằng AI")
@SecurityRequirement(name = "bearerAuth")
public class OnboardingDocumentController {

    private final OnboardingDocumentService onboardingDocumentService;

    @PostMapping(value = "/application/{applicationId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('CANDIDATE', 'HR', 'ADMIN')")
    @Operation(summary = "Tải lên hồ sơ Onboarding cho 1 đơn ứng tuyển")
    public ResponseEntity<ApiResponse<OnboardingDocumentResponse>> uploadDocument(
            @PathVariable Long applicationId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") DocumentType documentType,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        OnboardingDocumentResponse response = onboardingDocumentService.uploadDocument(applicationId, currentUser.getUser().getId(), file, documentType);
        return ResponseEntity.ok(ApiResponse.success("Tải lên hồ sơ thành công", response));
    }

    @GetMapping("/application/{applicationId}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'HR', 'ADMIN')")
    @Operation(summary = "Lấy danh sách hồ sơ Onboarding của 1 đơn ứng tuyển")
    public ResponseEntity<ApiResponse<List<OnboardingDocumentResponse>>> getDocumentsByApplication(
            @PathVariable Long applicationId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        List<OnboardingDocumentResponse> documents = onboardingDocumentService.getDocumentsByApplication(applicationId, currentUser.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'HR', 'ADMIN')")
    @Operation(summary = "Tải xuống file hồ sơ PII (Endpoint bảo mật có token)")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) throws IOException {

        Resource resource = onboardingDocumentService.downloadDocument(id, currentUser.getUser().getId());
        
        String contentType = Files.probeContentType(resource.getFile().toPath());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    @Operation(summary = "Cập nhật trạng thái duyệt hồ sơ (Dành cho HR)")
    public ResponseEntity<ApiResponse<OnboardingDocumentResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam VerificationStatus status,
            @RequestParam(required = false) String comment,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        OnboardingDocumentResponse response = onboardingDocumentService.updateDocumentStatus(id, currentUser.getUser().getId(), status, comment);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", response));
    }
}
