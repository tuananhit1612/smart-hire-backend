package com.smarthire.backend.features.dashboard.service;

import com.smarthire.backend.core.exception.ResourceNotFoundException;
import com.smarthire.backend.core.security.SecurityUtils;
import com.smarthire.backend.features.application.entity.Application;
import com.smarthire.backend.features.application.entity.ApplicationStageHistory;
import com.smarthire.backend.features.application.repository.ApplicationRepository;
import com.smarthire.backend.features.application.repository.ApplicationStageHistoryRepository;
import com.smarthire.backend.features.dashboard.dto.*;
import com.smarthire.backend.features.job.entity.Job;
import com.smarthire.backend.features.job.repository.JobRepository;
import com.smarthire.backend.shared.enums.ApplicationStage;
import com.smarthire.backend.shared.enums.JobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HrDashboardServiceImpl implements HrDashboardService {

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationStageHistoryRepository stageHistoryRepository;

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

        // ── New metrics ──

        List<WeeklyTrendItem> weeklyTrend = buildWeeklyTrend(allApps);
        List<TopJobItem> topJobs = buildTopJobs(allApps, jobs);
        List<RecentActivityItem> recentActivities = buildRecentActivities(jobIds);

        return HrDashboardResponse.builder()
                .totalJobs(totalJobs)
                .openJobs(openJobs)
                .closedJobs(closedJobs)
                .draftJobs(draftJobs)
                .totalApplications(totalApplications)
                .stageFunnel(stageFunnel)
                .hireRate(hireRate)
                .rejectRate(rejectRate)
                .weeklyTrend(weeklyTrend)
                .topJobs(topJobs)
                .recentActivities(recentActivities)
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
                .filter(a -> a.getStage() != null)
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

    /**
     * Group applications by appliedAt date for the last 8 days.
     * Returns one entry per day with the count of new applications.
     */
    private List<WeeklyTrendItem> buildWeeklyTrend(List<Application> allApps) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(7); // 8 days: today and 7 prior

        Map<LocalDate, Long> countByDate = allApps.stream()
                .filter(a -> a.getAppliedAt() != null)
                .filter(a -> !a.getAppliedAt().toLocalDate().isBefore(startDate))
                .collect(Collectors.groupingBy(
                        a -> a.getAppliedAt().toLocalDate(),
                        Collectors.counting()
                ));

        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        List<WeeklyTrendItem> trend = new ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(today); d = d.plusDays(1)) {
            trend.add(WeeklyTrendItem.builder()
                    .date(d.format(fmt))
                    .count(countByDate.getOrDefault(d, 0L))
                    .build());
        }
        return trend;
    }

    /**
     * Build top-5 jobs ranked by total application count.
     * Also includes hired count and applications received today.
     */
    private List<TopJobItem> buildTopJobs(List<Application> allApps, List<Job> jobs) {
        Map<Long, Job> jobMap = jobs.stream()
                .collect(Collectors.toMap(Job::getId, j -> j));

        Map<Long, List<Application>> appsByJob = allApps.stream()
                .collect(Collectors.groupingBy(a -> a.getJob().getId()));

        LocalDate today = LocalDate.now();

        return appsByJob.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .limit(5)
                .map(entry -> {
                    Long jobId = entry.getKey();
                    List<Application> apps = entry.getValue();
                    Job job = jobMap.get(jobId);

                    long hired = apps.stream()
                            .filter(a -> a.getStage() == ApplicationStage.HIRED).count();
                    long newToday = apps.stream()
                            .filter(a -> a.getAppliedAt() != null
                                    && a.getAppliedAt().toLocalDate().equals(today))
                            .count();

                    return TopJobItem.builder()
                            .jobId(jobId)
                            .title(job != null ? job.getTitle() : "Unknown")
                            .status(job != null ? job.getStatus().name() : "UNKNOWN")
                            .applicationCount(apps.size())
                            .hiredCount(hired)
                            .newToday(newToday)
                            .build();
                })
                .toList();
    }

    /**
     * Fetch latest 10 stage-change events across all of this HR's jobs.
     * Formats a human-readable action string from the stage transition.
     */
    private List<RecentActivityItem> buildRecentActivities(List<Long> jobIds) {
        if (jobIds.isEmpty()) return List.of();

        List<ApplicationStageHistory> histories =
                stageHistoryRepository.findRecentByJobIds(jobIds, PageRequest.of(0, 10));

        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        return histories.stream()
                .map(h -> {
                    Application app = h.getApplication();
                    String candidateName = app.getCandidateProfile() != null
                            && app.getCandidateProfile().getUser() != null
                            ? app.getCandidateProfile().getUser().getFullName()
                            : "Unknown";
                    String avatarUrl = app.getCandidateProfile() != null
                            && app.getCandidateProfile().getUser() != null
                            ? app.getCandidateProfile().getUser().getAvatarUrl()
                            : null;
                    String jobTitle = app.getJob() != null ? app.getJob().getTitle() : "Unknown";

                    String action = formatAction(h.getFromStage(), h.getToStage());

                    return RecentActivityItem.builder()
                            .candidateName(candidateName)
                            .action(action)
                            .jobTitle(jobTitle)
                            .timestamp(h.getCreatedAt() != null ? h.getCreatedAt().format(fmt) : "")
                            .avatarUrl(avatarUrl)
                            .build();
                })
                .toList();
    }

    private String formatAction(ApplicationStage from, ApplicationStage to) {
        if (from == null) {
            return "Applied";
        }
        return "Moved from " + formatStage(from) + " to " + formatStage(to);
    }

    private String formatStage(ApplicationStage stage) {
        if (stage == null) return "Unknown";
        return switch (stage) {
            case APPLIED -> "Applied";
            case SCREENING -> "Screening";
            case INTERVIEW -> "Interview";
            case OFFER -> "Offer";
            case HIRED -> "Hired";
            case REJECTED -> "Rejected";
        };
    }
}
