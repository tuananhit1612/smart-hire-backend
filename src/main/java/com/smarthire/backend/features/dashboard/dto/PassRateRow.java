package com.smarthire.backend.features.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassRateRow {
    private String position;
    private String department;
    private long totalApplicants;
    private long passed;
    private double passRate;
    private double prevRate;
    private long avgTimeToHire;
}
