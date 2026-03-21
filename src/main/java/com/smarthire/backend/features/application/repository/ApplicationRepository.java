package com.smarthire.backend.features.application.repository;

import com.smarthire.backend.features.application.entity.Application;
import com.smarthire.backend.shared.enums.ApplicationStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // ── Report export (eager fetch) ──

    @Query("SELECT a FROM Application a JOIN FETCH a.job")
    List<Application> findAllWithJob();

    @Query("SELECT a FROM Application a JOIN FETCH a.job WHERE a.job.id IN :jobIds")
    List<Application> findByJobIdInWithJob(@Param("jobIds") List<Long> jobIds);
}
