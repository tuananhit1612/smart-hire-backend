package com.smarthire.backend.features.dashboard.controller;

import com.smarthire.backend.features.dashboard.dto.HrDashboardResponse;
import com.smarthire.backend.features.dashboard.dto.JobDashboardResponse;
import com.smarthire.backend.features.dashboard.service.HrDashboardService;
import com.smarthire.backend.shared.constants.ApiPaths;
import com.smarthire.backend.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.DASHBOARD + "/hr")
@RequiredArgsConstructor
public class HrDashboardController {

    private final HrDashboardService hrDashboardService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<HrDashboardResponse>> getHrOverview() {
        HrDashboardResponse response = hrDashboardService.getHrOverview();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/jobs/{jobId}/stats")
    public ResponseEntity<ApiResponse<JobDashboardResponse>> getJobStats(@PathVariable Long jobId) {
        JobDashboardResponse response = hrDashboardService.getJobStats(jobId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
