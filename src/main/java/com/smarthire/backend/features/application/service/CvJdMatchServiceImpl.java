package com.smarthire.backend.features.application.service;

import com.smarthire.backend.core.exception.ResourceNotFoundException;
import com.smarthire.backend.features.application.dto.CvJdMatchResponse;
import com.smarthire.backend.features.application.entity.Application;
import com.smarthire.backend.features.application.entity.ApplicationAiResult;
import com.smarthire.backend.features.application.repository.ApplicationAiResultRepository;
import com.smarthire.backend.features.application.repository.ApplicationRepository;
import com.smarthire.backend.infrastructure.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CV-JD Matching Service Implementation (AI003).
 * Xử lý logic trigger matching, lấy kết quả, liệt kê theo job.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CvJdMatchServiceImpl implements CvJdMatchService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationAiResultRepository aiResultRepository;
    private final AiService aiService;

    @Override
    @Transactional
    public CvJdMatchResponse triggerMatch(Long applicationId, boolean forceReAnalyze) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found with id: " + applicationId));

        // Nếu đã có result và không force → trả về result cũ
        if (!forceReAnalyze && application.getAiResult() != null) {
            log.info("📋 AI Result already exists for application {} — returning cached result", applicationId);
            return toResponse(application.getAiResult(), application);
        }

        // Xóa result cũ nếu force re-analyze
        if (forceReAnalyze && application.getAiResult() != null) {
            log.info("🔄 Force re-analyze — deleting old AI result for application {}", applicationId);
            aiResultRepository.delete(application.getAiResult());
            aiResultRepository.flush();
            application.setAiResult(null);
        }

        // Gọi AI matching
        try {
            ApplicationAiResult aiResult = aiService.matchCvWithJob(application);
            aiResult = aiResultRepository.save(aiResult);
            application.setAiResult(aiResult);
            log.info("✅ AI CV-JD matching completed for application {} — score={}",
                    applicationId, aiResult.getMatchScore());
            return toResponse(aiResult, application);
        } catch (Exception e) {
            log.error("❌ AI CV-JD matching failed for application {}: {}", applicationId, e.getMessage());

            // Tạo fallback result để ghi nhận lỗi
            ApplicationAiResult fallback = ApplicationAiResult.builder()
                    .application(application)
                    .matchScore(0)
                    .skillMatch(0)
                    .experienceMatch(0)
                    .summary("AI analysis could not be completed: " + e.getMessage())
                    .strengths(List.of())
                    .gaps(List.of("AI service error"))
                    .recommendations(List.of("Please try re-analyzing later"))
                    .build();
            fallback = aiResultRepository.save(fallback);
            application.setAiResult(fallback);
            return toResponse(fallback, application);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CvJdMatchResponse getMatchResult(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found with id: " + applicationId));

        ApplicationAiResult aiResult = aiResultRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "AI match result not found for application: " + applicationId));

        return toResponse(aiResult, application);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CvJdMatchResponse> getMatchResultsByJob(Long jobId) {
        List<ApplicationAiResult> results = aiResultRepository.findByApplicationJobId(jobId);
        return results.stream()
                .map(r -> toResponse(r, r.getApplication()))
                .toList();
    }

    // ══════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════

    private CvJdMatchResponse toResponse(ApplicationAiResult result, Application application) {
        String candidateName = "Candidate";
        Long candidateProfileId = null;
        try {
            if (application.getCandidateProfile() != null) {
                candidateProfileId = application.getCandidateProfile().getId();
                if (application.getCandidateProfile().getUser() != null) {
                    candidateName = application.getCandidateProfile().getUser().getFullName();
                }
            }
        } catch (Exception e) {
            log.debug("Could not resolve candidate details: {}", e.getMessage());
        }

        return CvJdMatchResponse.builder()
                .applicationId(application.getId())
                .jobId(application.getJob().getId())
                .jobTitle(application.getJob().getTitle())
                .candidateProfileId(candidateProfileId)
                .candidateName(candidateName)
                .scoreTotal(result.getMatchScore())
                .scoreBreakdown(CvJdMatchResponse.ScoreBreakdown.builder()
                        .skillsMatch(result.getSkillMatch())
                        .experienceMatch(result.getExperienceMatch())
                        .build())
                .strengths(result.getStrengths())
                .gaps(result.getGaps())
                .recommendations(result.getRecommendations())
                .explanation(result.getSummary())
                .analyzedAt(result.getAnalyzedAt())
                .build();
    }
}
