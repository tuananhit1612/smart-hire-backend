package com.smarthire.backend.features.job.service;

import com.smarthire.backend.core.exception.BadRequestException;
import com.smarthire.backend.core.exception.ForbiddenException;
import com.smarthire.backend.core.exception.ResourceNotFoundException;
import com.smarthire.backend.core.security.SecurityUtils;
import com.smarthire.backend.features.auth.entity.User;
import com.smarthire.backend.features.company.entity.Company;
import com.smarthire.backend.features.company.repository.CompanyRepository;
import com.smarthire.backend.features.job.dto.*;
import com.smarthire.backend.features.job.entity.Job;
import com.smarthire.backend.features.job.entity.JobSkill;
import com.smarthire.backend.features.job.repository.JobRepository;
import com.smarthire.backend.shared.enums.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;

    @Override
    @Transactional
    public JobResponse createJob(CreateJobRequest request) {
        User currentUser = SecurityUtils.getCurrentUser();
        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + request.getCompanyId()));

        if (company.getCreatedBy() != null && !company.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only create jobs for your own company");
        }

        Job job = Job.builder()
                .company(company)
                .createdBy(currentUser)
                .title(request.getTitle())
                .description(request.getDescription())
                .requirements(request.getRequirements())
                .benefits(request.getBenefits())
                .jobType(parseEnum(JobType.class, request.getJobType(), JobType.FULL_TIME))
                .jobLevel(parseEnum(JobLevel.class, request.getJobLevel(), JobLevel.MID))
                .location(request.getLocation())
                .isRemote(request.getIsRemote() != null ? request.getIsRemote() : false)
                .salaryMin(request.getSalaryMin())
                .salaryMax(request.getSalaryMax())
                .salaryCurrency(request.getSalaryCurrency() != null ? request.getSalaryCurrency() : "VND")
                .deadline(request.getDeadline())
                .status(JobStatus.DRAFT)
                .build();

        if (request.getSkills() != null) {
            request.getSkills().forEach(s -> {
                JobSkill skill = JobSkill.builder()
                        .job(job)
                        .skillName(s.getSkillName())
                        .skillType(parseEnum(SkillType.class, s.getSkillType(), SkillType.MUST_HAVE))
                        .build();
                job.getSkills().add(skill);
            });
        }

        Job saved = jobRepository.save(job);
        log.info("Job created: {} by {}", saved.getTitle(), currentUser.getEmail());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public JobResponse getJobById(Long id) {
        return toResponse(findJobOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> getMyJobs() {
        User currentUser = SecurityUtils.getCurrentUser();
        return jobRepository.findByCreatedByIdOrderByCreatedAtDesc(currentUser.getId())
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> getJobsByCompany(Long companyId) {
        return jobRepository.findByCompanyIdOrderByCreatedAtDesc(companyId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public JobResponse updateJob(Long id, UpdateJobRequest request) {
        Job job = findJobOrThrow(id);
        checkOwnership(job);

        if (request.getTitle() != null) job.setTitle(request.getTitle());
        if (request.getDescription() != null) job.setDescription(request.getDescription());
        if (request.getRequirements() != null) job.setRequirements(request.getRequirements());
        if (request.getBenefits() != null) job.setBenefits(request.getBenefits());
        if (request.getJobType() != null) job.setJobType(parseEnum(JobType.class, request.getJobType(), job.getJobType()));
        if (request.getJobLevel() != null) job.setJobLevel(parseEnum(JobLevel.class, request.getJobLevel(), job.getJobLevel()));
        if (request.getLocation() != null) job.setLocation(request.getLocation());
        if (request.getIsRemote() != null) job.setIsRemote(request.getIsRemote());
        if (request.getSalaryMin() != null) job.setSalaryMin(request.getSalaryMin());
        if (request.getSalaryMax() != null) job.setSalaryMax(request.getSalaryMax());
        if (request.getSalaryCurrency() != null) job.setSalaryCurrency(request.getSalaryCurrency());
        if (request.getDeadline() != null) job.setDeadline(request.getDeadline());

        if (request.getSkills() != null) {
            job.getSkills().clear();
            request.getSkills().forEach(s -> {
                JobSkill skill = JobSkill.builder()
                        .job(job)
                        .skillName(s.getSkillName())
                        .skillType(parseEnum(SkillType.class, s.getSkillType(), SkillType.MUST_HAVE))
                        .build();
                job.getSkills().add(skill);
            });
        }

        Job saved = jobRepository.save(job);
        log.info("Job updated: {}", saved.getTitle());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public JobResponse changeStatus(Long id, String status) {
        Job job = findJobOrThrow(id);
        checkOwnership(job);

        JobStatus newStatus = parseEnum(JobStatus.class, status, null);
        if (newStatus == null) {
            throw new BadRequestException("Invalid status. Must be: DRAFT, OPEN, CLOSED");
        }

        job.setStatus(newStatus);
        Job saved = jobRepository.save(job);
        log.info("Job {} status changed to {}", saved.getTitle(), newStatus);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteJob(Long id) {
        Job job = findJobOrThrow(id);
        checkOwnership(job);
        jobRepository.delete(job);
        log.info("Job deleted: {}", job.getTitle());
    }

    // ── Helpers ──

    private Job findJobOrThrow(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));
    }

    private void checkOwnership(Job job) {
        User currentUser = SecurityUtils.getCurrentUser();
        // Fallback for mock/seeded jobs that might not have a proper creator structure
        if (job.getCreatedBy() != null && !job.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not have permission to modify this job");
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, E defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid value '" + value + "' for " + enumClass.getSimpleName());
        }
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
