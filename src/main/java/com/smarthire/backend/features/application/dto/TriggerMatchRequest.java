package com.smarthire.backend.features.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO để trigger CV-JD matching.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriggerMatchRequest {

    @NotNull(message = "Application ID is required")
    private Long applicationId;

    @Builder.Default
    private Boolean forceReAnalyze = false;
}
