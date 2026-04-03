package com.smarthire.backend.features.job.controller;

import com.smarthire.backend.features.job.dto.JobResponse;
import com.smarthire.backend.features.job.entity.Job;
import com.smarthire.backend.features.job.repository.JobRepository;
import com.smarthire.backend.features.job.repository.JobSpecifications;
import com.smarthire.backend.features.job.dto.JobSkillDto;
import com.smarthire.backend.features.job.service.JobService;
import com.smarthire.backend.shared.constants.ApiPaths;
import com.smarthire.backend.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping(ApiPaths.PUBLIC + "/jobs")
@RequiredArgsConstructor
public class PublicJobController {

    private final JobRepository jobRepository;
    private final JobService jobService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<JobResponse>>> searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String jobLevel,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) BigDecimal salaryMin,
            @RequestParam(required = false) BigDecimal salaryMax) {

        List<Job> jobs = jobRepository.findAll(
                JobSpecifications.search(keyword, location, jobLevel, jobType, salaryMin, salaryMax));

        List<JobResponse> responses = jobs.stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobResponse>> getJobDetail(@PathVariable Long id) {
        JobResponse response = jobService.getJobById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private JobResponse toResponse(Job job) {
        List<JobSkillDto> skillDtos = job.getSkills().stream()
                .map(s -> JobSkillDto.builder()
                        .skillName(s.getSkillName())
                        .skillType(s.getSkillType().name())
                        .build())
                .toList();

        return JobResponse.builder()
                .id(job.getId())
                .companyId(job.getCompany().getId())
                .companyName(job.getCompany().getName())
                .companyLogoUrl(job.getCompany().getLogoUrl())
                .createdBy(job.getCreatedBy().getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .requirements(job.getRequirements())
                .benefits(job.getBenefits())
                .jobType(job.getJobType())
                .jobLevel(job.getJobLevel())
                .location(job.getLocation())
                .isRemote(job.getIsRemote())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .salaryCurrency(job.getSalaryCurrency())
                .deadline(job.getDeadline())
                .status(job.getStatus())
                .skills(skillDtos)
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}
