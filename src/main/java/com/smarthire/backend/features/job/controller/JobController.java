package com.smarthire.backend.features.job.controller;

import com.smarthire.backend.features.job.dto.CreateJobRequest;
import com.smarthire.backend.features.job.dto.JobResponse;
import com.smarthire.backend.features.job.dto.UpdateJobRequest;
import com.smarthire.backend.features.job.service.JobService;
import com.smarthire.backend.shared.constants.ApiPaths;
import com.smarthire.backend.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiPaths.JOBS)
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<ApiResponse<JobResponse>> createJob(
            @Valid @RequestBody CreateJobRequest request) {
        JobResponse response = jobService.createJob(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Job created successfully", response));
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<ApiResponse<JobResponse>> getJobById(@PathVariable Long id) {
        JobResponse response = jobService.getJobById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<JobResponse>>> getMyJobs() {
        List<JobResponse> jobs = jobService.getMyJobs();
        return ResponseEntity.ok(ApiResponse.success(jobs));
    }

    @GetMapping("/company/{companyId:\\d+}")
    public ResponseEntity<ApiResponse<List<JobResponse>>> getJobsByCompany(@PathVariable Long companyId) {
        List<JobResponse> jobs = jobService.getJobsByCompany(companyId);
        return ResponseEntity.ok(ApiResponse.success(jobs));
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<ApiResponse<JobResponse>> updateJob(
            @PathVariable Long id,
            @Valid @RequestBody UpdateJobRequest request) {
        JobResponse response = jobService.updateJob(id, request);
        return ResponseEntity.ok(ApiResponse.success("Job updated successfully", response));
    }

    @PatchMapping("/{id:\\d+}/status")
    public ResponseEntity<ApiResponse<JobResponse>> changeStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        JobResponse response = jobService.changeStatus(id, body.get("status"));
        return ResponseEntity.ok(ApiResponse.success("Job status updated", response));
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<ApiResponse<Void>> deleteJob(@PathVariable Long id) {
        jobService.deleteJob(id);
        return ResponseEntity.ok(ApiResponse.success("Job deleted successfully", null));
    }
}
