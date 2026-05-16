package com.smarthire.backend.features.candidate.service;

import com.smarthire.backend.core.exception.BadRequestException;
import com.smarthire.backend.core.exception.ForbiddenException;
import com.smarthire.backend.core.exception.ResourceNotFoundException;
import com.smarthire.backend.core.security.SecurityUtils;
import com.smarthire.backend.features.auth.entity.User;
import com.smarthire.backend.features.candidate.entity.AiCvReview;
import com.smarthire.backend.features.candidate.entity.CvFile;
import com.smarthire.backend.features.candidate.repository.AiCvReviewRepository;
import com.smarthire.backend.features.candidate.repository.CvFileRepository;
import com.smarthire.backend.infrastructure.ai.service.AiService;
import com.smarthire.backend.shared.enums.Role;
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
    private final CvFileRepository cvFileRepository;

    @Override
    @Transactional
    public AiCvReview reviewCv(Long cvFileId) {
        log.info("Triggering AI CV review for cvFileId={}", cvFileId);
        verifyCanAccessCv(cvFileId);

        AiCvReview review = aiService.reviewCvFile(cvFileId);
        review = cvReviewRepository.save(review);

        log.info("CV review saved: id={}, rating={}", review.getId(), review.getOverallRating());
        return review;
    }

    @Override
    @Transactional(readOnly = true)
    public AiCvReview getLatestReview(Long cvFileId) {
        verifyCanAccessCv(cvFileId);
        return cvReviewRepository.findTopByCvFileIdOrderByCreatedAtDesc(cvFileId)
                .orElseThrow(() -> new ResourceNotFoundException("CV Review not found for cvFileId: " + cvFileId));
    }

    @Override
    public String optimizeCv(Long cvFileId) {
        log.info("Triggering AI CV optimization for cvFileId={}", cvFileId);
        verifyCanAccessCv(cvFileId);

        var latestReview = cvReviewRepository.findTopByCvFileIdOrderByCreatedAtDesc(cvFileId);
        if (latestReview.isPresent()) {
            String completenessJson = latestReview.get().getDataCompleteness();
            if (completenessJson != null && completenessJson.contains("\"canOptimize\":false")) {
                log.warn("CV optimization blocked due to insufficient data: cvFileId={}", cvFileId);
                throw new BadRequestException(
                        "CV thieu thong tin nghiem trong. Vui long bo sung day du trong CV Builder truoc khi toi uu."
                );
            }
        }

        return aiService.optimizeCv(cvFileId);
    }

    private void verifyCanAccessCv(Long cvFileId) {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        CvFile cvFile = cvFileRepository.findById(cvFileId)
                .orElseThrow(() -> new ResourceNotFoundException("CV File", cvFileId));
        boolean isOwner = cvFile.getCandidateProfile() != null
                && cvFile.getCandidateProfile().getUser() != null
                && cvFile.getCandidateProfile().getUser().getId().equals(currentUser.getId());
        if (!isOwner) {
            throw new ForbiddenException("You do not have permission to access this CV");
        }
    }
}
