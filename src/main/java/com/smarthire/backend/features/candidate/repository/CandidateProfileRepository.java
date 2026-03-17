package com.smarthire.backend.features.candidate.repository;

import com.smarthire.backend.features.candidate.entity.CandidateProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, Long> {
    
    Optional<CandidateProfile> findByUserId(Long userId);
    
    boolean existsByUserId(Long userId);
}
