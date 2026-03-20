package com.smarthire.backend.features.candidate.repository;

import com.smarthire.backend.features.candidate.entity.CandidateEducation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidateEducationRepository extends JpaRepository<CandidateEducation, Long> {
    List<CandidateEducation> findByCandidateProfileIdOrderByStartDateDesc(Long candidateProfileId);
}
