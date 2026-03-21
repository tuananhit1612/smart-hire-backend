package com.smarthire.backend.features.report.controller;

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
@RequestMapping(ApiPaths.ADMIN + ApiPaths.REPORTS)
@RequiredArgsConstructor
public class ReportExportController {

    private final ReportExportService reportExportService;

    @GetMapping("/applications/csv")
    public ResponseEntity<byte[]> exportApplicationsCsv() {
        byte[] csv = reportExportService.exportApplicationsCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=applications_report.csv")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(csv);
    }

    @GetMapping("/jobs/csv")
    public ResponseEntity<byte[]> exportJobsCsv() {
        byte[] csv = reportExportService.exportJobsCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=jobs_report.csv")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(csv);
    }
}
