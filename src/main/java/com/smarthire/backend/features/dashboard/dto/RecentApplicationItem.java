package com.smarthire.backend.features.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RecentApplicationItem {
    private Long id;
    private Long jobId;
    private String jobTitle;
    private String companyName;
    private String currentStage;   // "APPLIED", "INTERVIEW", "HIRED", "REJECTED"
    private String appliedAt;      // ISO-8601
    private String updatedAt;      // ISO-8601
}
