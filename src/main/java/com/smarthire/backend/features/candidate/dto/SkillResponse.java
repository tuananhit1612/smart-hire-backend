package com.smarthire.backend.features.candidate.dto;

import com.smarthire.backend.shared.enums.ProficiencyLevel;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillResponse {
    private Long id;
    private String skillName;
    private ProficiencyLevel proficiencyLevel;
}
