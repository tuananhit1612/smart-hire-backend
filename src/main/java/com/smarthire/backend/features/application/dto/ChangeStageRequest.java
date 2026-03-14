package com.smarthire.backend.features.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeStageRequest {

    @NotBlank(message = "Stage is required")
    private String stage;

    private String note;
}
