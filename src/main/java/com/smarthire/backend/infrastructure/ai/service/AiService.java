package com.smarthire.backend.infrastructure.ai.service;

import com.smarthire.backend.features.candidate.entity.AiCvReview;
import com.smarthire.backend.features.application.entity.ApplicationAiResult;
import com.smarthire.backend.features.application.entity.Application;
import com.smarthire.backend.features.onboarding.dto.VerifiedCvData;

/**
 * High-level AI Service interface.
 * Tái sử dụng cho mọi feature cần AI.
 */
public interface AiService {

    /**
     * Parse CV file (PDF) → extract personal info.
     * Synchronous — upload PDF lên Gemini, chờ kết quả.
     */
    VerifiedCvData parseCvFile(Long cvFileId);

    /**
     * Match CV content vs Job Description → score + analysis.
     */
    ApplicationAiResult matchCvWithJob(Application application);

    /**
     * Review CV quality → issues, suggestions, rating.
     */
    AiCvReview reviewCvFile(Long cvFileId);

    /**
     * Optimize toàn bộ CV — rewrite all improvable items.
     * Input: CV file content.
     * Output: JSON string of optimized sections.
     */
    String optimizeCv(Long cvFileId);
}
