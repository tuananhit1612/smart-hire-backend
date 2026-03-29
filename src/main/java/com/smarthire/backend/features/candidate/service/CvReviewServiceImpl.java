package com.smarthire.backend.features.candidate.service;

import com.smarthire.backend.core.exception.ResourceNotFoundException;
import com.smarthire.backend.features.candidate.entity.AiCvReview;
import com.smarthire.backend.features.candidate.repository.AiCvReviewRepository;
import com.smarthire.backend.infrastructure.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CvReviewServiceImpl implements CvReviewService {

    private final AiService aiService;
    private final AiCvReviewRepository cvReviewRepository;

    @Override
    @Transactional
    public AiCvReview reviewCv(Long cvFileId) {
        log.info("🤖 Triggering AI CV review for cvFileId={}", cvFileId);

        // Gọi Gemini AI review
        AiCvReview review = aiService.reviewCvFile(cvFileId);
        review = cvReviewRepository.save(review);

        log.info("✅ CV review saved — id={}, rating={}", review.getId(), review.getOverallRating());
        return review;
    }

    @Override
    @Transactional(readOnly = true)
    public AiCvReview getLatestReview(Long cvFileId) {
        return cvReviewRepository.findTopByCvFileIdOrderByCreatedAtDesc(cvFileId)
                .orElseThrow(() -> new ResourceNotFoundException("CV Review not found for cvFileId: " + cvFileId));
    }
}
