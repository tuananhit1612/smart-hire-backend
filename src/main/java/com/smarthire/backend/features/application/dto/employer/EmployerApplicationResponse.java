package com.smarthire.backend.features.application.dto.employer;

import com.smarthire.backend.shared.enums.ApplicationStage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployerApplicationResponse {
    private Long id;
    private Long jobId;
    private Long candidateId;
    private String jobTitle;
    private String name;
    private String email;
    private String phone;
    private String avatarUrl;
    private String currentTitle;
    private Integer experienceYears;
    private ApplicationStage status;
    private LocalDateTime appliedAt;
    
    private AiAnalysisResponse aiAnalysis;
    private List<String> skills;
    private List<EmployerApplicationActivityResponse> activities;
    private List<EmployerApplicationNoteResponse> notes;
    private String onboardingProgress; // e.g. "4/6"
}
