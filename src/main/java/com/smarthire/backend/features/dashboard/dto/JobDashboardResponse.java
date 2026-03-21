package com.smarthire.backend.features.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class JobDashboardResponse {

    private Long jobId;
    private String jobTitle;
    private String jobStatus;

    private Long totalApplications;
    private List<StageFunnelItem> stageFunnel;

    private Double hireRate;
    private Double rejectRate;
}
