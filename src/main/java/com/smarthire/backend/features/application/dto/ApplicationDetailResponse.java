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
public class ApplicationDetailResponse {
    private Long id;
    private Long jobId;
    private String jobTitle;
    private String companyName;
    private ApplicationStage currentStage;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
    
    private List<ApplicationHistoryResponse> history;
}
