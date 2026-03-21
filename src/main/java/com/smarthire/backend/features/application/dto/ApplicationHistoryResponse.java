package com.smarthire.backend.features.application.dto;

import com.smarthire.backend.shared.enums.ApplicationStage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationHistoryResponse {
    private Long id;
    private ApplicationStage fromStage;
    private ApplicationStage toStage;
    private String note;
    private LocalDateTime createdAt;
    private String changedByName;
}
