package com.smarthire.backend.features.dashboard.controller;

import com.smarthire.backend.features.dashboard.dto.CandidateDashboardResponse;
import com.smarthire.backend.features.dashboard.service.CandidateDashboardService;
import com.smarthire.backend.shared.constants.ApiPaths;
import com.smarthire.backend.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.DASHBOARD + "/candidate")
@RequiredArgsConstructor
public class CandidateDashboardController {

    private final CandidateDashboardService candidateDashboardService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<CandidateDashboardResponse>> getCandidateOverview() {
        CandidateDashboardResponse response = candidateDashboardService.getCandidateOverview();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
