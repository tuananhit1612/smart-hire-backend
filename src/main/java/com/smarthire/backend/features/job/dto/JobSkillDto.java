package com.smarthire.backend.features.job.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JobSkillDto {
    private String skillName;
    private String skillType;
}
