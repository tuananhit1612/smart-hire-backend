package com.smarthire.backend.features.candidate.controller;

import com.smarthire.backend.features.candidate.dto.CvFileResponse;
import com.smarthire.backend.features.candidate.service.CvFileService;
import com.smarthire.backend.shared.constants.ApiPaths;
import com.smarthire.backend.shared.dto.ApiResponse;
import com.smarthire.backend.shared.enums.CvSource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.CANDIDATE_PROFILE + "/cv-files")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CANDIDATE')")
@Tag(name = "CV File Management", description = "Upload, list, set primary, delete and download CV files")
public class CvFileController {

    private final CvFileService cvFileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload CV", description = "Upload a PDF or DOCX CV file. First CV is automatically set as primary.")
    public ResponseEntity<ApiResponse<CvFileResponse>> uploadCv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "source", defaultValue = "UPLOAD") CvSource source) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("CV uploaded successfully", cvFileService.uploadCv(file, source)));
    }

    @GetMapping
    @Operation(summary = "List my CVs", description = "Returns all CV files of the current candidate, newest first")
    public ResponseEntity<ApiResponse<List<CvFileResponse>>> getMyCvFiles() {
        return ResponseEntity.ok(ApiResponse.success("CV files retrieved successfully", cvFileService.getMyCvFiles()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get CV detail", description = "Returns details of a specific CV file")
    public ResponseEntity<ApiResponse<CvFileResponse>> getCvFileById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("CV file retrieved successfully", cvFileService.getCvFileById(id)));
    }

    @PutMapping("/{id}/primary")
    @Operation(summary = "Set CV as primary", description = "Sets the specified CV as the primary CV")
    public ResponseEntity<ApiResponse<CvFileResponse>> setPrimary(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("CV set as primary successfully", cvFileService.setPrimary(id)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete CV", description = "Deletes a CV file. Cannot delete the primary CV.")
    public ResponseEntity<ApiResponse<Void>> deleteCvFile(@PathVariable Long id) {
        cvFileService.deleteCvFile(id);
        return ResponseEntity.ok(ApiResponse.success("CV deleted successfully", null));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download CV", description = "Downloads the CV file (Inline for PDFs)")
    public ResponseEntity<Resource> downloadCvFile(@PathVariable Long id) {
        Resource resource = cvFileService.downloadCvFile(id);
        String filename = resource.getFilename();
        
        // Use inline for PDFs so browsers/iframes can preview them directly
        String disposition = (filename != null && filename.toLowerCase().endsWith(".pdf")) 
                ? "inline" 
                : "attachment";
                
        MediaType mediaType = (filename != null && filename.toLowerCase().endsWith(".pdf"))
                ? MediaType.APPLICATION_PDF
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + filename + "\"")
                .body(resource);
    }
}
