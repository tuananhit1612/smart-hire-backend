package com.smarthire.backend.features.candidate.service;

import com.smarthire.backend.features.candidate.entity.AiCvReview;

public interface CvReviewService {

    /**
     * Trigger AI review cho CV file.
     * Synchronous — chờ Gemini trả kết quả.
     */
    AiCvReview reviewCv(Long cvFileId);

    /**
     * Lấy review gần nhất cho CV file.
     */
    AiCvReview getLatestReview(Long cvFileId);

    /**
     * Tối ưu CV bằng AI — rewrite items cần cải thiện.
     * @return JSON string chứa optimized sections.
     */
    String optimizeCv(Long cvFileId);
}
