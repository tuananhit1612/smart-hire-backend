package com.smarthire.backend.features.job.service;

import com.smarthire.backend.features.job.dto.CreateJobRequest;
import com.smarthire.backend.features.job.dto.JobResponse;
import com.smarthire.backend.features.job.dto.UpdateJobRequest;

import java.util.List;

public interface JobService {

    JobResponse createJob(CreateJobRequest request);

    JobResponse getJobById(Long id);

    List<JobResponse> getMyJobs();

    List<JobResponse> getJobsByCompany(Long companyId);

    JobResponse updateJob(Long id, UpdateJobRequest request);

    JobResponse changeStatus(Long id, String status);

    void deleteJob(Long id);
}
