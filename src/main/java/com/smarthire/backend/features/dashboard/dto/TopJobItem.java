package com.smarthire.backend.features.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TopJobItem {
    private Long jobId;
    private String title;
    private String status;
    private long applicationCount;
    private long hiredCount;
    private long newToday;
}
