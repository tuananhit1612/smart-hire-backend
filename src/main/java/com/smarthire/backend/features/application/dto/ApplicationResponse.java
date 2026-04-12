package com.smarthire.backend.features.application.dto;

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
public class ApplicationResponse {
    private Long id;
    private Long jobId;
    private String jobTitle;
    private Long candidateProfileId;
    
    // Candidate details
    private String candidateName;
    private String candidateEmail;
    private String candidateHeadline;
    private Integer candidateYearsOfExperience;
    
    // CV Details
    private Long cvFileId;
    private String cvFileName;
    private String cvFilePath;
    
    private ApplicationStage stage;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
    private List<StageHistoryDto> stageHistory;
}
