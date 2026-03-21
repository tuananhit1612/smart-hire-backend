package com.smarthire.backend.features.dashboard.service;

import com.smarthire.backend.core.exception.ResourceNotFoundException;
import com.smarthire.backend.core.security.SecurityUtils;
import com.smarthire.backend.features.application.entity.Application;
import com.smarthire.backend.features.application.repository.ApplicationRepository;
import com.smarthire.backend.features.dashboard.dto.HrDashboardResponse;
import com.smarthire.backend.features.dashboard.dto.JobDashboardResponse;
import com.smarthire.backend.features.dashboard.dto.StageFunnelItem;
import com.smarthire.backend.features.job.entity.Job;
import com.smarthire.backend.features.job.repository.JobRepository;
import com.smarthire.backend.shared.enums.ApplicationStage;
import com.smarthire.backend.shared.enums.JobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HrDashboardServiceImpl implements HrDashboardService {

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;

    @Override
    @Transactional(readOnly = true)
    public HrDashboardResponse getHrOverview() {
        Long userId = SecurityUtils.getCurrentUserId();

        // Job counts
        long totalJobs = jobRepository.countByCreatedById(userId);
        long openJobs = jobRepository.countByCreatedByIdAndStatus(userId, JobStatus.OPEN);
        long closedJobs = jobRepository.countByCreatedByIdAndStatus(userId, JobStatus.CLOSED);
        long draftJobs = jobRepository.countByCreatedByIdAndStatus(userId, JobStatus.DRAFT);

        // Get all jobs by this HR, then get all applications
        List<Job> jobs = jobRepository.findByCreatedByIdOrderByCreatedAtDesc(userId);
        List<Long> jobIds = jobs.stream().map(Job::getId).toList();

        List<Application> allApps = jobIds.isEmpty()
                ? List.of()
                : applicationRepository.findByJobIdIn(jobIds);

        long totalApplications = allApps.size();

        // Build funnel
        List<StageFunnelItem> stageFunnel = buildFunnel(allApps, totalApplications);

        // Rates
        long hiredCount = countByStage(allApps, ApplicationStage.HIRED);
        long rejectedCount = countByStage(allApps, ApplicationStage.REJECTED);

        double hireRate = totalApplications > 0
                ? Math.round(hiredCount * 10000.0 / totalApplications) / 100.0 : 0.0;
        double rejectRate = totalApplications > 0
                ? Math.round(rejectedCount * 10000.0 / totalApplications) / 100.0 : 0.0;

        return HrDashboardResponse.builder()
                .totalJobs(totalJobs)
                .openJobs(openJobs)
                .closedJobs(closedJobs)
                .draftJobs(draftJobs)
                .totalApplications(totalApplications)
                .stageFunnel(stageFunnel)
                .hireRate(hireRate)
                .rejectRate(rejectRate)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public JobDashboardResponse getJobStats(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        List<Application> apps = applicationRepository.findByJobIdOrderByAppliedAtDesc(jobId);
        long totalApplications = apps.size();

        List<StageFunnelItem> stageFunnel = buildFunnel(apps, totalApplications);

        long hiredCount = countByStage(apps, ApplicationStage.HIRED);
        long rejectedCount = countByStage(apps, ApplicationStage.REJECTED);

        double hireRate = totalApplications > 0
                ? Math.round(hiredCount * 10000.0 / totalApplications) / 100.0 : 0.0;
        double rejectRate = totalApplications > 0
                ? Math.round(rejectedCount * 10000.0 / totalApplications) / 100.0 : 0.0;

        return JobDashboardResponse.builder()
                .jobId(job.getId())
                .jobTitle(job.getTitle())
                .jobStatus(job.getStatus().name())
                .totalApplications(totalApplications)
                .stageFunnel(stageFunnel)
                .hireRate(hireRate)
                .rejectRate(rejectRate)
                .build();
    }

    // ── Helpers ──

    private List<StageFunnelItem> buildFunnel(List<Application> apps, long total) {
        Map<ApplicationStage, Long> countMap = apps.stream()
                .collect(Collectors.groupingBy(Application::getStage, Collectors.counting()));

        return Arrays.stream(ApplicationStage.values())
                .map(stage -> {
                    long count = countMap.getOrDefault(stage, 0L);
                    double pct = total > 0
                            ? Math.round(count * 10000.0 / total) / 100.0 : 0.0;
                    return StageFunnelItem.builder()
                            .stage(stage.name())
                            .count(count)
                            .percentage(pct)
                            .build();
                })
                .toList();
    }

    private long countByStage(List<Application> apps, ApplicationStage stage) {
        return apps.stream().filter(a -> a.getStage() == stage).count();
    }
}
