package com.smarthire.backend.features.dashboard.controller;

import com.smarthire.backend.features.dashboard.dto.AdminDashboardResponse;
import com.smarthire.backend.features.dashboard.service.AdminDashboardService;
import com.smarthire.backend.shared.constants.ApiPaths;
import com.smarthire.backend.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.ADMIN + "/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getAdminOverview() {
        AdminDashboardResponse response = adminDashboardService.getAdminOverview();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
