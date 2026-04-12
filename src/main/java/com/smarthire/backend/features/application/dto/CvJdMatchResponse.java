package com.smarthire.backend.features.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO cho CV-JD Matching API (FR-32).
 * Trả về score, breakdown, strengths, gaps, recommendations, explanation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CvJdMatchResponse {

    private Long applicationId;
    private Long jobId;
    private String jobTitle;
    private Long candidateProfileId;
    private String candidateName;

    // ── Score ──
    private Integer scoreTotal;
    private ScoreBreakdown scoreBreakdown;

    // ── Analysis ──
    private List<String> strengths;
    private List<String> gaps;
    private List<String> recommendations;
    private String explanation;

    // ── Metadata ──
    private LocalDateTime analyzedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreBreakdown {
        private Integer skillsMatch;
        private Integer experienceMatch;
    }
}
