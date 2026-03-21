package com.smarthire.backend.features.report.controller;

import com.smarthire.backend.core.security.SecurityUtils;
import com.smarthire.backend.features.report.service.ReportExportService;
import com.smarthire.backend.shared.constants.ApiPaths;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.DASHBOARD + ApiPaths.REPORTS)
@RequiredArgsConstructor
public class HrReportExportController {

    private final ReportExportService reportExportService;

    @GetMapping("/applications/csv")
    public ResponseEntity<byte[]> exportApplicationsCsv() {
        Long userId = SecurityUtils.getCurrentUserId();
        byte[] csv = reportExportService.exportHrApplicationsCsv(userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=hr_applications_report.csv")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(csv);
    }

    @GetMapping("/jobs/csv")
    public ResponseEntity<byte[]> exportJobsCsv() {
        Long userId = SecurityUtils.getCurrentUserId();
        byte[] csv = reportExportService.exportHrJobsCsv(userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=hr_jobs_report.csv")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(csv);
    }
}
