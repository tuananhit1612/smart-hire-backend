package com.smarthire.backend.features.candidate.repository;

import com.smarthire.backend.features.candidate.entity.CandidateProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidateProjectRepository extends JpaRepository<CandidateProject, Long> {
    List<CandidateProject> findByCandidateProfileId(Long candidateProfileId);
}
