package com.smarthire.backend.features.cv.repository;

import com.smarthire.backend.features.cv.entity.CvBuilderData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CvBuilderDataRepository extends JpaRepository<CvBuilderData, Long> {
    Optional<CvBuilderData> findByCandidateProfileId(Long candidateProfileId);
}
