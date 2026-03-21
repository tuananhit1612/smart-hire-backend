package com.smarthire.backend.features.application.repository;

import com.smarthire.backend.features.application.entity.Application;
import com.smarthire.backend.shared.enums.ApplicationStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByJobIdOrderByAppliedAtDesc(Long jobId);

    List<Application> findByJobIdAndStageOrderByAppliedAtDesc(Long jobId, ApplicationStage stage);

    List<Application> findByCandidateProfileIdOrderByAppliedAtDesc(Long candidateProfileId);

    boolean existsByJobIdAndCandidateProfileId(Long jobId, Long candidateProfileId);

    // ── Dashboard queries ──

    long countByJobId(Long jobId);

    long countByJobIdAndStage(Long jobId, ApplicationStage stage);

    List<Application> findByJobIdIn(List<Long> jobIds);
}
