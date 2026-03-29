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
}
