package com.smarthire.backend.features.candidate.repository;

import com.smarthire.backend.features.candidate.entity.AiCvReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiCvReviewRepository extends JpaRepository<AiCvReview, Long> {
    Optional<AiCvReview> findTopByCvFileIdOrderByCreatedAtDesc(Long cvFileId);
}
