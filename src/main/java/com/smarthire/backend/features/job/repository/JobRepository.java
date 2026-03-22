package com.smarthire.backend.features.job.repository;

import com.smarthire.backend.features.job.entity.Job;
import com.smarthire.backend.shared.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    List<Job> findByCreatedByIdOrderByCreatedAtDesc(Long userId);

    List<Job> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    List<Job> findByStatusOrderByCreatedAtDesc(JobStatus status);

    // ── Dashboard queries ──

    long countByCreatedById(Long userId);

    long countByCreatedByIdAndStatus(Long userId, JobStatus status);
    long countByStatus(JobStatus status);

    // ── Report export (eager fetch) ──

    @Query("SELECT j FROM Job j LEFT JOIN FETCH j.company")
    List<Job> findAllWithCompany();

    @Query("SELECT j FROM Job j LEFT JOIN FETCH j.company WHERE j.createdBy.id = :userId ORDER BY j.createdAt DESC")
    List<Job> findByCreatedByIdWithCompany(@Param("userId") Long userId);
}
