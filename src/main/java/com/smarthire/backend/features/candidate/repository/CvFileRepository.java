package com.smarthire.backend.features.candidate.repository;

import com.smarthire.backend.features.candidate.entity.CvFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CvFileRepository extends JpaRepository<CvFile, Long> {

    List<CvFile> findByCandidateProfileIdOrderByCreatedAtDesc(Long candidateProfileId);

    Optional<CvFile> findByCandidateProfileIdAndIsPrimaryTrue(Long candidateProfileId);

    long countByCandidateProfileId(Long candidateProfileId);

    @Modifying
    @Query("UPDATE CvFile c SET c.isPrimary = false WHERE c.candidateProfile.id = :profileId")
    void resetPrimaryByProfileId(@Param("profileId") Long profileId);
}
