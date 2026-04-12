package com.smarthire.backend.features.company.controller;

import com.smarthire.backend.features.company.dto.CompanyResponse;
import com.smarthire.backend.features.company.dto.CreateCompanyRequest;
import com.smarthire.backend.features.company.dto.UpdateCompanyRequest;
import com.smarthire.backend.features.company.service.CompanyService;
import com.smarthire.backend.shared.constants.ApiPaths;
import com.smarthire.backend.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.COMPANIES)
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    public ResponseEntity<ApiResponse<CompanyResponse>> createCompany(
            @Valid @RequestBody CreateCompanyRequest request) {
        CompanyResponse response = companyService.createCompany(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Company created successfully", response));
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<ApiResponse<CompanyResponse>> getCompanyById(@PathVariable Long id) {
        CompanyResponse response = companyService.getCompanyById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CompanyResponse>>> getAllCompanies() {
        List<CompanyResponse> companies = companyService.getAllCompanies();
        return ResponseEntity.ok(ApiResponse.success(companies));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<CompanyResponse>>> getMyCompanies() {
        List<CompanyResponse> companies = companyService.getMyCompanies();
        return ResponseEntity.ok(ApiResponse.success(companies));
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<ApiResponse<CompanyResponse>> updateCompany(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCompanyRequest request) {
        CompanyResponse response = companyService.updateCompany(id, request);
        return ResponseEntity.ok(ApiResponse.success("Company updated successfully", response));
    }

    @PostMapping(value = "/{id:\\d+}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CompanyResponse>> uploadLogo(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        CompanyResponse response = companyService.uploadLogo(id, file);
        return ResponseEntity.ok(ApiResponse.success("Logo uploaded successfully", response));
    }

    @PostMapping(value = "/{id:\\d+}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CompanyResponse>> uploadCover(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        CompanyResponse response = companyService.uploadCover(id, file);
        return ResponseEntity.ok(ApiResponse.success("Cover uploaded successfully", response));
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<ApiResponse<Void>> deleteCompany(@PathVariable Long id) {
        companyService.deleteCompany(id);
        return ResponseEntity.ok(ApiResponse.success("Company deleted successfully", null));
    }
}
