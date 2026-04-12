package com.smarthire.backend.features.onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingAiVerificationResult {
    private boolean isValid;
    private String feedbackReason;
    private String extractedName; // From OCR to cross-check with CandidateProfile Name
    private String extractedIdNumber;
    private String extractedDob;
}
