package com.smarthire.backend.features.onboarding.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.smarthire.backend.features.onboarding.enums.DocumentType;
import com.smarthire.backend.features.onboarding.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingDocumentResponse {
    private Long id;
    private Long applicationId;
    private DocumentType documentType;
    private VerificationStatus status;
    private String aiFeedback;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime uploadedAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
