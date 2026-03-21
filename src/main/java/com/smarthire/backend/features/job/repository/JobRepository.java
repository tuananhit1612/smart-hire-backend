package com.smarthire.backend.features.job.repository;

import com.smarthire.backend.features.job.entity.Job;
import com.smarthire.backend.shared.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    List<Job> findByCreatedByIdOrderByCreatedAtDesc(Long userId);

    List<Job> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    List<Job> findByStatusOrderByCreatedAtDesc(JobStatus status);

    long countByStatus(JobStatus status);
}
