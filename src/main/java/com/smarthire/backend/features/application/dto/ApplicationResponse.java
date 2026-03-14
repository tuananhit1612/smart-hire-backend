package com.smarthire.backend.features.application.dto;

import com.smarthire.backend.shared.enums.ApplicationStage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ApplicationResponse {

    private Long id;
    private Long jobId;
    private String jobTitle;
    private Long candidateProfileId;
    private Long cvFileId;
    private ApplicationStage stage;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
    private List<StageHistoryDto> stageHistory;
}
