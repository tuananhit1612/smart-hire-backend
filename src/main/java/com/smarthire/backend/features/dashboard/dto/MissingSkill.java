package com.smarthire.backend.features.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissingSkill {
    private String skill;
    private String category;
    private long demandCount;
    private double candidateGap;
    private String trend;
    private String suggestedAction;
}
