package com.smarthire.backend.features.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class CandidateDashboardResponse {

    // ── Summary stats ──
    private long totalApplications;
    private Map<String, Long> stageBreakdown;    // "APPLIED": 5, "INTERVIEW": 2, ...

    // ── Recent applications (top 5) ──
    private List<RecentApplicationItem> recentApplications;

    // ── Interviews ──
    private long upcomingInterviews;

    // ── Profile ──
    private int profileCompleteness;  // 0-100

    // ── Weekly activity (last 7 days) ──
    private List<WeeklyActivityItem> weeklyActivity;
}
