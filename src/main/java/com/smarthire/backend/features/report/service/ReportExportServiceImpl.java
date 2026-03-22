package com.smarthire.backend.features.report.service;

import com.smarthire.backend.features.application.entity.Application;
import com.smarthire.backend.features.application.repository.ApplicationRepository;
import com.smarthire.backend.features.job.entity.Job;
import com.smarthire.backend.features.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportExportServiceImpl implements ReportExportService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String UTF8_BOM = "\uFEFF";

    // ── Admin: all applications ─────────────────────────────────

    @Override
    public byte[] exportApplicationsCsv() {
        List<Application> apps = applicationRepository.findAll();
        return buildApplicationsCsv(apps);
    }

    // ── Admin: all jobs ─────────────────────────────────────────

    @Override
    public byte[] exportJobsCsv() {
        List<Job> jobs = jobRepository.findAll();
        return buildJobsCsv(jobs);
    }

    // ── HR: own applications ────────────────────────────────────

    @Override
    public byte[] exportHrApplicationsCsv(Long userId) {
        List<Job> hrJobs = jobRepository.findByCreatedByIdOrderByCreatedAtDesc(userId);
        List<Long> jobIds = hrJobs.stream().map(Job::getId).toList();

        if (jobIds.isEmpty()) {
            return buildApplicationsCsv(List.of());
        }
        List<Application> apps = applicationRepository.findByJobIdIn(jobIds);
        return buildApplicationsCsv(apps);
    }

    // ── HR: own jobs ────────────────────────────────────────────

    @Override
    public byte[] exportHrJobsCsv(Long userId) {
        List<Job> jobs = jobRepository.findByCreatedByIdOrderByCreatedAtDesc(userId);
        return buildJobsCsv(jobs);
    }

    // ── CSV builders ────────────────────────────────────────────

    private byte[] buildApplicationsCsv(List<Application> apps) {
        StringBuilder sb = new StringBuilder(UTF8_BOM);
        sb.append("ID,Job Title,Candidate Profile ID,Stage,Applied At,Updated At\n");

        for (Application app : apps) {
            sb.append(app.getId()).append(',');
            sb.append(escapeCsv(app.getJob().getTitle())).append(',');
            sb.append(app.getCandidateProfileId()).append(',');
            sb.append(app.getStage()).append(',');
            sb.append(app.getAppliedAt() != null ? app.getAppliedAt().format(FMT) : "").append(',');
            sb.append(app.getUpdatedAt() != null ? app.getUpdatedAt().format(FMT) : "");
            sb.append('\n');
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] buildJobsCsv(List<Job> jobs) {
        StringBuilder sb = new StringBuilder(UTF8_BOM);
        sb.append("ID,Title,Company,Status,Type,Level,Location,Salary Min,Salary Max,Deadline,Created At\n");

        for (Job job : jobs) {
            sb.append(job.getId()).append(',');
            sb.append(escapeCsv(job.getTitle())).append(',');
            sb.append(escapeCsv(job.getCompany() != null ? job.getCompany().getName() : "")).append(',');
            sb.append(job.getStatus()).append(',');
            sb.append(job.getJobType()).append(',');
            sb.append(job.getJobLevel()).append(',');
            sb.append(escapeCsv(job.getLocation() != null ? job.getLocation() : "")).append(',');
            sb.append(job.getSalaryMin() != null ? job.getSalaryMin().toPlainString() : "").append(',');
            sb.append(job.getSalaryMax() != null ? job.getSalaryMax().toPlainString() : "").append(',');
            sb.append(job.getDeadline() != null ? job.getDeadline().toString() : "").append(',');
            sb.append(job.getCreatedAt() != null ? job.getCreatedAt().format(FMT) : "");
            sb.append('\n');
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
