package com.smarthire.backend.features.application.dto;

import com.smarthire.backend.shared.enums.ApplicationStage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class StageHistoryDto {

    private Long id;
    private ApplicationStage fromStage;
    private ApplicationStage toStage;
    private Long changedBy;
    private String note;
    private LocalDateTime createdAt;
}
