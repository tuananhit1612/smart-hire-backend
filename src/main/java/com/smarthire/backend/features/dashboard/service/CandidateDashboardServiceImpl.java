package com.smarthire.backend.features.dashboard.service;

import com.smarthire.backend.core.security.SecurityUtils;
import com.smarthire.backend.features.application.entity.Application;
import com.smarthire.backend.features.application.repository.ApplicationRepository;
import com.smarthire.backend.features.candidate.entity.CandidateProfile;
import com.smarthire.backend.features.candidate.repository.CandidateEducationRepository;
import com.smarthire.backend.features.candidate.repository.CandidateExperienceRepository;
import com.smarthire.backend.features.candidate.repository.CandidateProfileRepository;
import com.smarthire.backend.features.candidate.repository.CandidateSkillRepository;
import com.smarthire.backend.features.candidate.repository.CvFileRepository;
import com.smarthire.backend.features.dashboard.dto.CandidateDashboardResponse;
import com.smarthire.backend.features.dashboard.dto.RecentApplicationItem;
import com.smarthire.backend.features.dashboard.dto.WeeklyActivityItem;
import com.smarthire.backend.features.interview.entity.InterviewRoom;
import com.smarthire.backend.features.interview.repository.InterviewRoomRepository;
import com.smarthire.backend.shared.enums.ApplicationStage;
import com.smarthire.backend.shared.enums.InterviewStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateDashboardServiceImpl implements CandidateDashboardService {

    private final ApplicationRepository applicationRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final CandidateEducationRepository educationRepository;
    private final CandidateExperienceRepository experienceRepository;
    private final CandidateSkillRepository skillRepository;
    private final CvFileRepository cvFileRepository;
    private final InterviewRoomRepository interviewRoomRepository;

    @Override
    @Transactional(readOnly = true)
    public CandidateDashboardResponse getCandidateOverview() {
        Long userId = SecurityUtils.getCurrentUserId();

        // ── Find candidate profile ──
        Optional<CandidateProfile> profileOpt = candidateProfileRepository.findByUserId(userId);
        Long candidateProfileId = profileOpt.map(CandidateProfile::getId).orElse(null);

        // ── Applications ──
        List<Application> allApps = candidateProfileId != null
                ? applicationRepository.findByCandidateProfileIdOrderByAppliedAtDesc(candidateProfileId)
                : List.of();

        long totalApplications = allApps.size();

        // Stage breakdown
        Map<String, Long> stageBreakdown = allApps.stream()
                .filter(a -> a.getStage() != null)
                .collect(Collectors.groupingBy(
                        a -> a.getStage().name(),
                        Collectors.counting()
                ));
        // Ensure all stages present
        for (ApplicationStage stage : ApplicationStage.values()) {
            stageBreakdown.putIfAbsent(stage.name(), 0L);
        }

        // Recent applications (top 5)
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        List<RecentApplicationItem> recentApplications = allApps.stream()
                .limit(5)
                .map(app -> RecentApplicationItem.builder()
                        .id(app.getId())
                        .jobId(app.getJob().getId())
                        .jobTitle(app.getJob().getTitle())
                        .companyName(app.getJob().getCompany() != null
                                ? app.getJob().getCompany().getName() : "N/A")
                        .currentStage(app.getStage().name())
                        .appliedAt(app.getAppliedAt() != null ? app.getAppliedAt().format(fmt) : "")
                        .updatedAt(app.getUpdatedAt() != null ? app.getUpdatedAt().format(fmt) : "")
                        .build())
                .toList();

        // ── Upcoming interviews ──
        // Count interviews for applications owned by this candidate that are SCHEDULED
        long upcomingInterviews = 0;
        if (candidateProfileId != null) {
            // Get all application ids for this candidate
            List<Long> appIds = allApps.stream().map(Application::getId).toList();
            if (!appIds.isEmpty()) {
                List<InterviewRoom> scheduledInterviews = interviewRoomRepository
                        .findByStatusOrderByScheduledAtAsc(InterviewStatus.SCHEDULED);
                upcomingInterviews = scheduledInterviews.stream()
                        .filter(ir -> ir.getApplication() != null
                                && appIds.contains(ir.getApplication().getId())
                                && ir.getScheduledAt() != null
                                && ir.getScheduledAt().isAfter(LocalDateTime.now()))
                        .count();
            }
        }

        // ── Profile completeness ──
        int profileCompleteness = calculateProfileCompleteness(profileOpt.orElse(null), candidateProfileId);

        // ── Weekly activity (last 7 days) ──
        List<WeeklyActivityItem> weeklyActivity = buildWeeklyActivity(allApps);

        return CandidateDashboardResponse.builder()
                .totalApplications(totalApplications)
                .stageBreakdown(stageBreakdown)
                .recentApplications(recentApplications)
                .upcomingInterviews(upcomingInterviews)
                .profileCompleteness(profileCompleteness)
                .weeklyActivity(weeklyActivity)
                .build();
    }

    /**
     * Calculate profile completeness based on filled fields.
     * fullName/email = always present from User (20%)
     * phone (10%), education (20%), experience (20%), skills (15%), CV (15%)
     */
    private int calculateProfileCompleteness(CandidateProfile profile, Long candidateProfileId) {
        if (profile == null) return 0;

        int score = 20; // fullName/email always exist

        // Phone from user
        if (profile.getUser() != null && profile.getUser().getPhone() != null
                && !profile.getUser().getPhone().isBlank()) {
            score += 10;
        }

        // Has education
        if (candidateProfileId != null && !educationRepository.findByCandidateProfileIdOrderByStartDateDesc(candidateProfileId).isEmpty()) {
            score += 20;
        }

        // Has experience
        if (candidateProfileId != null && !experienceRepository.findByCandidateProfileIdOrderByStartDateDesc(candidateProfileId).isEmpty()) {
            score += 20;
        }

        // Has skills
        if (candidateProfileId != null && !skillRepository.findByCandidateProfileId(candidateProfileId).isEmpty()) {
            score += 15;
        }

        // Has CV file
        if (candidateProfileId != null && cvFileRepository.countByCandidateProfileId(candidateProfileId) > 0) {
            score += 15;
        }

        return Math.min(score, 100);
    }

    /**
     * Build weekly activity for the last 7 days (Mon-Sun labels).
     */
    private List<WeeklyActivityItem> buildWeeklyActivity(List<Application> allApps) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6); // 7 days including today

        Map<LocalDate, Long> countByDate = allApps.stream()
                .filter(a -> a.getAppliedAt() != null)
                .filter(a -> !a.getAppliedAt().toLocalDate().isBefore(startDate))
                .collect(Collectors.groupingBy(
                        a -> a.getAppliedAt().toLocalDate(),
                        Collectors.counting()
                ));

        List<WeeklyActivityItem> activity = new ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(today); d = d.plusDays(1)) {
            activity.add(WeeklyActivityItem.builder()
                    .day(toDayLabel(d.getDayOfWeek()))
                    .applications(countByDate.getOrDefault(d, 0L))
                    .views(0) // placeholder — not tracked yet
                    .build());
        }
        return activity;
    }

    private String toDayLabel(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> "T2";
            case TUESDAY -> "T3";
            case WEDNESDAY -> "T4";
            case THURSDAY -> "T5";
            case FRIDAY -> "T6";
            case SATURDAY -> "T7";
            case SUNDAY -> "CN";
        };
    }
}
