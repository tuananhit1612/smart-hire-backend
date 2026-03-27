package com.smarthire.backend.features.dashboard.service;

import com.smarthire.backend.features.application.entity.Application;
import com.smarthire.backend.features.application.repository.ApplicationRepository;
import com.smarthire.backend.features.auth.repository.UserRepository;
import com.smarthire.backend.features.company.repository.CompanyRepository;
import com.smarthire.backend.features.dashboard.dto.AdminDashboardResponse;
import com.smarthire.backend.features.dashboard.dto.StageFunnelItem;
import com.smarthire.backend.features.job.repository.JobRepository;
import com.smarthire.backend.shared.enums.ApplicationStage;
import com.smarthire.backend.shared.enums.JobStatus;
import com.smarthire.backend.shared.enums.Role;
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
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final ApplicationRepository applicationRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminOverview() {

        // ── Users ──
        long totalUsers       = userRepository.count();
        long totalCandidates  = userRepository.countByRole(Role.CANDIDATE);
        long totalHrUsers     = userRepository.countByRole(Role.HR);
        long totalAdmins      = userRepository.countByRole(Role.ADMIN);
        long activeUsers      = userRepository.countByIsActive(true);
        long inactiveUsers    = userRepository.countByIsActive(false);

        // ── Jobs ──
        long totalJobs  = jobRepository.count();
        long openJobs   = jobRepository.countByStatus(JobStatus.OPEN);
        long closedJobs = jobRepository.countByStatus(JobStatus.CLOSED);
        long draftJobs  = jobRepository.countByStatus(JobStatus.DRAFT);

        // ── Companies ──
        long totalCompanies = companyRepository.count();

        // ── Applications ──
        List<Application> allApps = applicationRepository.findAll();
        long totalApplications = allApps.size();

        // Build funnel
        List<StageFunnelItem> stageFunnel = buildFunnel(allApps, totalApplications);

        // Rates
        long hiredCount    = countByStage(allApps, ApplicationStage.HIRED);
        long rejectedCount = countByStage(allApps, ApplicationStage.REJECTED);

        double hireRate = totalApplications > 0
                ? Math.round(hiredCount * 10000.0 / totalApplications) / 100.0 : 0.0;
        double rejectRate = totalApplications > 0
                ? Math.round(rejectedCount * 10000.0 / totalApplications) / 100.0 : 0.0;

        return AdminDashboardResponse.builder()
                .totalUsers(totalUsers)
                .totalCandidates(totalCandidates)
                .totalHrUsers(totalHrUsers)
                .totalAdmins(totalAdmins)
                .activeUsers(activeUsers)
                .inactiveUsers(inactiveUsers)
                .totalJobs(totalJobs)
                .openJobs(openJobs)
                .closedJobs(closedJobs)
                .draftJobs(draftJobs)
                .totalCompanies(totalCompanies)
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
}
