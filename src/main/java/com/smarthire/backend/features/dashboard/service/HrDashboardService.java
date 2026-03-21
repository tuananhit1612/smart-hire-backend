package com.smarthire.backend.features.dashboard.service;

import com.smarthire.backend.features.dashboard.dto.HrDashboardResponse;
import com.smarthire.backend.features.dashboard.dto.JobDashboardResponse;

public interface HrDashboardService {

    HrDashboardResponse getHrOverview();

    JobDashboardResponse getJobStats(Long jobId);
}
