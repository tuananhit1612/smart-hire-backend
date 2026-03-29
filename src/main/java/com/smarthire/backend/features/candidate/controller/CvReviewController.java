package com.smarthire.backend.features.candidate.controller;

import com.smarthire.backend.features.candidate.entity.AiCvReview;
import com.smarthire.backend.features.candidate.service.CvReviewService;
import com.smarthire.backend.shared.constants.ApiPaths;
import com.smarthire.backend.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(ApiPaths.AI + "/cv-review")
@RequiredArgsConstructor
public class CvReviewController {

    private final CvReviewService cvReviewService;

    /**
     * POST /api/ai/cv-review/{cvFileId}
     * Trigger AI review cho CV. Synchronous — chờ kết quả.
     */
    @PostMapping("/{cvFileId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerReview(@PathVariable Long cvFileId) {
        AiCvReview review = cvReviewService.reviewCv(cvFileId);
        return ResponseEntity.ok(ApiResponse.success("CV review completed", toResponseMap(review)));
    }

    /**
     * GET /api/ai/cv-review/{cvFileId}
     * Lấy review gần nhất.
     */
    @GetMapping("/{cvFileId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReview(@PathVariable Long cvFileId) {
        AiCvReview review = cvReviewService.getLatestReview(cvFileId);
        return ResponseEntity.ok(ApiResponse.success(toResponseMap(review)));
    }

    /**
     * POST /api/ai/cv-review/{cvFileId}/optimize
     * Tối ưu CV bằng AI — rewrite all improvable items.
     */
    @PostMapping("/{cvFileId}/optimize")
    public ResponseEntity<ApiResponse<String>> optimizeCv(@PathVariable Long cvFileId) {
        String optimizedJson = cvReviewService.optimizeCv(cvFileId);
        return ResponseEntity.ok(ApiResponse.success("CV optimized successfully", optimizedJson));
    }

    private Map<String, Object> toResponseMap(AiCvReview review) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", review.getId());
        map.put("cvFileId", review.getCvFile().getId());

        // New structured fields
        map.put("overallScore", review.getOverallScore());
        map.put("atsScore", review.getAtsScore());
        map.put("sectionScores", review.getSectionScores());
        map.put("topIssues", review.getTopIssues());
        map.put("dataCompleteness", review.getDataCompleteness());

        // Legacy fields
        map.put("summary", review.getSummary());
        map.put("issues", review.getIssues());
        map.put("suggestions", review.getSuggestions());
        map.put("strengths", review.getStrengths());
        map.put("weaknesses", review.getWeaknesses());
        map.put("overallRating", review.getOverallRating());
        map.put("createdAt", review.getCreatedAt());
        return map;
    }
}
