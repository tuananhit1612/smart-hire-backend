package com.smarthire.backend.features.application.service;

import com.smarthire.backend.core.exception.BadRequestException;
import com.smarthire.backend.core.exception.ConflictException;
import com.smarthire.backend.core.exception.ResourceNotFoundException;
import com.smarthire.backend.features.application.dto.ApplicationDetailResponse;
import com.smarthire.backend.features.application.dto.ApplicationHistoryResponse;
import com.smarthire.backend.features.application.dto.ApplicationResponse;
import com.smarthire.backend.features.application.dto.ApplicationTrackingResponse;
import com.smarthire.backend.features.application.dto.ApplyJobRequest;
import com.smarthire.backend.features.application.entity.Application;
import com.smarthire.backend.features.application.entity.ApplicationStageHistory;
import com.smarthire.backend.features.application.repository.ApplicationRepository;
import com.smarthire.backend.features.application.repository.ApplicationStageHistoryRepository;
import com.smarthire.backend.features.candidate.entity.CandidateProfile;
import com.smarthire.backend.features.candidate.entity.CvFile;
import com.smarthire.backend.features.candidate.repository.CandidateProfileRepository;
import com.smarthire.backend.features.candidate.repository.CvFileRepository;
import com.smarthire.backend.features.job.entity.Job;
import com.smarthire.backend.features.job.repository.JobRepository;
import com.smarthire.backend.shared.enums.ApplicationStage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationStageHistoryRepository historyRepository;
    private final JobRepository jobRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final CvFileRepository cvFileRepository;

    @Override
    @Transactional
    public ApplicationResponse applyToJob(Long userId, ApplyJobRequest request) {
        CandidateProfile profile = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate Profile not found"));

        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + request.getJobId()));

        if (applicationRepository.existsByJobIdAndCandidateProfileId(job.getId(), profile.getId())) {
            throw new ConflictException("You have already applied for this job");
        }

        CvFile cvFile = cvFileRepository.findById(request.getCvFileId())
                .orElseThrow(() -> new ResourceNotFoundException("CV File not found with id: " + request.getCvFileId()));

        if (!cvFile.getCandidateProfile().getId().equals(profile.getId())) {
            throw new BadRequestException("CV File does not belong to the current candidate");
        }

        Application application = Application.builder()
                .job(job)
                .candidateProfile(profile)
                .cvFile(cvFile)
                .stage(ApplicationStage.APPLIED)
                .build();
        
        application = applicationRepository.save(application);

        ApplicationStageHistory history = ApplicationStageHistory.builder()
                .application(application)
                .fromStage(null)
                .toStage(ApplicationStage.APPLIED)
                .changedBy(profile.getUser())
                .note("Candidate applied successfully")
                .build();
                
        historyRepository.save(history);

        return ApplicationResponse.builder()
                .id(application.getId())
                .jobId(job.getId())
                .candidateProfileId(profile.getId())
                .cvFileId(cvFile.getId())
                .stage(application.getStage())
                .appliedAt(application.getAppliedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationTrackingResponse> getCandidateApplications(Long userId, Pageable pageable) {
        CandidateProfile profile = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate Profile not found"));

        return applicationRepository.findByCandidateProfileId(profile.getId(), pageable)
                .map(app -> ApplicationTrackingResponse.builder()
                        .id(app.getId())
                        .jobId(app.getJob().getId())
                        .jobTitle(app.getJob().getTitle())
                        .companyName(app.getJob().getCompany().getName())
                        .currentStage(app.getStage())
                        .appliedAt(app.getAppliedAt())
                        .updatedAt(app.getUpdatedAt())
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationDetailResponse getApplicationDetail(Long userId, Long applicationId) {
        CandidateProfile profile = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate Profile not found"));

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        if (!application.getCandidateProfile().getId().equals(profile.getId())) {
            throw new BadRequestException("Not authorized to view this application");
        }

        List<ApplicationHistoryResponse> history = historyRepository.findByApplicationIdOrderByCreatedAtDesc(application.getId())
                .stream()
                .map(h -> ApplicationHistoryResponse.builder()
                        .id(h.getId())
                        .fromStage(h.getFromStage())
                        .toStage(h.getToStage())
                        .note(h.getNote())
                        .createdAt(h.getCreatedAt())
                        .changedByName(h.getChangedBy().getFullName())
                        .build())
                .toList();

        return ApplicationDetailResponse.builder()
                .id(application.getId())
                .jobId(application.getJob().getId())
                .jobTitle(application.getJob().getTitle())
                .companyName(application.getJob().getCompany().getName())
                .currentStage(application.getStage())
                .appliedAt(application.getAppliedAt())
                .updatedAt(application.getUpdatedAt())
                .history(history)
                .build();
    }
}
