package com.smarthire.backend.features.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class HrDashboardResponse {

    private Long totalJobs;
    private Long openJobs;
    private Long closedJobs;
    private Long draftJobs;

    private Long totalApplications;
    private List<StageFunnelItem> stageFunnel;

    private Double hireRate;
    private Double rejectRate;

    private List<WeeklyTrendItem> weeklyTrend;
    private List<TopJobItem> topJobs;
    private List<RecentActivityItem> recentActivities;

    private List<PassRateRow> passRates;
    private List<MissingSkill> missingSkills;
}
