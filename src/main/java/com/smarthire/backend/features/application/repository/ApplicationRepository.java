package com.smarthire.backend.features.application.repository;

import com.smarthire.backend.features.application.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    boolean existsByJobIdAndCandidateProfileId(Long jobId, Long candidateProfileId);
    Page<Application> findByCandidateProfileId(Long candidateProfileId, Pageable pageable);
}
