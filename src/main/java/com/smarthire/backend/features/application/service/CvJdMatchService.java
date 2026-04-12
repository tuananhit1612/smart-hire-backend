package com.smarthire.backend.features.application.service;

import com.smarthire.backend.features.application.dto.CvJdMatchResponse;

import java.util.List;

/**
 * Service cho CV-JD Matching (AI003).
 * Cung cấp API trigger matching, xem kết quả, và liệt kê theo job.
 */
public interface CvJdMatchService {

    /**
     * Trigger AI matching cho 1 application.
     * Nếu đã có result và forceReAnalyze=false thì trả về result cũ.
     */
    CvJdMatchResponse triggerMatch(Long applicationId, boolean forceReAnalyze);

    /**
     * Lấy kết quả matching cho 1 application.
     * Throw ResourceNotFoundException nếu chưa có result.
     */
    CvJdMatchResponse getMatchResult(Long applicationId);

    /**
     * Lấy tất cả kết quả matching theo job.
     */
    List<CvJdMatchResponse> getMatchResultsByJob(Long jobId);
}
