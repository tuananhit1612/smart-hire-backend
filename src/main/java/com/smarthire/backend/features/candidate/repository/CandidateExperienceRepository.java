package com.smarthire.backend.features.candidate.repository;

import com.smarthire.backend.features.candidate.entity.CandidateExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidateExperienceRepository extends JpaRepository<CandidateExperience, Long> {
    List<CandidateExperience> findByCandidateProfileIdOrderByStartDateDesc(Long candidateProfileId);
}
