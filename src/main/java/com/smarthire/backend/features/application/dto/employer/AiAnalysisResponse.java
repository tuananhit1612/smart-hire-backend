package com.smarthire.backend.features.application.dto.employer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisResponse {
    private Integer matchScore;
    private String summary;
    private ScoreBreakdown breakdown;
    private List<String> strengths;
    private List<String> gaps;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreBreakdown {
        private Integer skillMatch;
        private Integer experienceMatch;
    }
}
