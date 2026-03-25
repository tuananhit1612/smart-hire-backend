package com.smarthire.backend.features.application.repository;

import com.smarthire.backend.features.application.entity.ApplicationStageHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationStageHistoryRepository extends JpaRepository<ApplicationStageHistory, Long> {
    List<ApplicationStageHistory> findByApplicationIdOrderByCreatedAtDesc(Long applicationId);

    @Query("SELECT h FROM ApplicationStageHistory h " +
           "JOIN FETCH h.application a " +
           "JOIN FETCH a.candidateProfile cp " +
           "JOIN FETCH cp.user u " +
           "JOIN FETCH a.job j " +
           "WHERE a.job.id IN :jobIds " +
           "ORDER BY h.createdAt DESC")
    List<ApplicationStageHistory> findRecentByJobIds(@Param("jobIds") List<Long> jobIds, Pageable pageable);
}
