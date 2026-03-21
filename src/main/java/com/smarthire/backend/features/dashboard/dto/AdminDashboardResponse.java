package com.smarthire.backend.features.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class AdminDashboardResponse {

    // ── Users ──
    private long totalUsers;
    private long totalCandidates;
    private long totalHrUsers;
    private long totalAdmins;
    private long activeUsers;
    private long inactiveUsers;

    // ── Jobs ──
    private long totalJobs;
    private long openJobs;
    private long closedJobs;
    private long draftJobs;

    // ── Companies ──
    private long totalCompanies;

    // ── Applications ──
    private long totalApplications;
    private List<StageFunnelItem> stageFunnel;

    // ── Rates ──
    private double hireRate;
    private double rejectRate;
}
