package com.smarthire.backend.features.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RecentActivityItem {
    private String candidateName;
    private String action;
    private String jobTitle;
    private String timestamp;   // ISO-8601
    private String avatarUrl;
}
