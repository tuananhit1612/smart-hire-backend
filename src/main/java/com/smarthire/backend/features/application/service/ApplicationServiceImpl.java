package com.smarthire.backend.features.application.service;

import com.smarthire.backend.core.exception.BadRequestException;
import com.smarthire.backend.core.exception.ConflictException;
import com.smarthire.backend.core.exception.ResourceNotFoundException;
import com.smarthire.backend.features.application.dto.*;
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
import com.smarthire.backend.features.auth.entity.User;
import com.smarthire.backend.features.auth.repository.UserRepository;
import com.smarthire.backend.features.notification.dto.CreateNotificationRequest;
import com.smarthire.backend.features.notification.service.NotificationService;
import com.smarthire.backend.features.notification.service.RealtimeEventService;
import com.smarthire.backend.shared.enums.ApplicationStage;
import com.smarthire.backend.shared.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationStageHistoryRepository historyRepository;
    private final JobRepository jobRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final CvFileRepository cvFileRepository;
    private final EmailService emailService;
    private final RealtimeEventService realtimeEventService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    // ── Management features (develop) ──

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(Long id) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        return toResponse(app);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByJob(Long jobId, String stage) {
        List<Application> apps;
        if (stage != null && !stage.isBlank()) {
            ApplicationStage stageEnum = parseStage(stage);
            apps = applicationRepository.findByJobIdAndStageOrderByAppliedAtDesc(jobId, stageEnum);
        } else {
            apps = applicationRepository.findByJobIdOrderByAppliedAtDesc(jobId);
        }
        return apps.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public ApplicationResponse changeStage(Long applicationId, Long userId, ChangeStageRequest request) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        ApplicationStage oldStage = application.getStage();
        ApplicationStage newStage = parseStage(request.getStage());

        if (oldStage == newStage) {
            return toResponse(application);
        }

        application.setStage(newStage);
        Application saved = applicationRepository.save(application);

        // Save history (Now with changedBy)
        ApplicationStageHistory history = ApplicationStageHistory.builder()
                .application(saved)
                .fromStage(oldStage)
                .toStage(newStage)
                .changedBy(actor)
                .note(request.getNote())
                .build();
        historyRepository.save(history);

        log.info("Application {} stage changed by {}: {} -> {}", applicationId, userId, oldStage, newStage);

        // Notifications (develop feature)
        sendStageNotification(saved, oldStage, newStage);

        // Realtime event
        Long candidateUserId = lookupCandidateUserId(saved.getCandidateProfile().getId());
        realtimeEventService.publishStageChanged(saved, oldStage, newStage, null, candidateUserId);

        // In-app notification
        if (candidateUserId != null) {
            notificationService.createNotification(CreateNotificationRequest.builder()
                    .userId(candidateUserId)
                    .type("APPLICATION_STAGE_CHANGED")
                    .title("Cập nhật trạng thái ứng tuyển")
                    .content("Đơn ứng tuyển \"" + saved.getJob().getTitle() + "\" đã chuyển từ "
                            + oldStage.name() + " sang " + newStage.name())
                    .referenceType("APPLICATION")
                    .referenceId(saved.getId())
                    .build());
        }

        return toResponse(saved);
    }

    // ── Candidate features (BE021) ──

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

        return toResponse(application);
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
    public List<ApplicationTrackingResponse> getCandidateApplicationsList(Long userId) {
        CandidateProfile profile = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate Profile not found"));

        return applicationRepository.findByCandidateProfileIdOrderByAppliedAtDesc(profile.getId())
                .stream()
                .map(app -> ApplicationTrackingResponse.builder()
                        .id(app.getId())
                        .jobId(app.getJob().getId())
                        .jobTitle(app.getJob().getTitle())
                        .companyName(app.getJob().getCompany().getName())
                        .currentStage(app.getStage())
                        .appliedAt(app.getAppliedAt())
                        .updatedAt(app.getUpdatedAt())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void withdrawApplication(Long userId, Long applicationId) {
        CandidateProfile profile = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate Profile not found"));

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        if (!application.getCandidateProfile().getId().equals(profile.getId())) {
            throw new BadRequestException("Not authorized to withdraw this application");
        }

        applicationRepository.delete(application);
        log.info("Application {} withdrawn by user {}", applicationId, userId);
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
                        .changedByName(h.getChangedBy() != null ? h.getChangedBy().getFullName() : "System")
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

    // ── Helpers ──

    private void sendStageNotification(Application app, ApplicationStage oldStage, ApplicationStage newStage) {
        if (newStage != ApplicationStage.HIRED
                && newStage != ApplicationStage.REJECTED) {
            return;
        }

        try {
            CandidateProfile profile = app.getCandidateProfile();
            if (profile == null || profile.getUser() == null) {
                log.warn("Cannot send stage email: candidate profile for application {} not found", app.getId());
                return;
            }

            String email = profile.getUser().getEmail();
            String jobTitle = app.getJob().getTitle();
            String subject = buildStageSubject(newStage, jobTitle);
            String body = buildStageEmailBody(profile.getUser().getFullName(), jobTitle, newStage);

            emailService.sendHtmlEmail(email, subject, body);
        } catch (Exception e) {
            log.error("Failed to send stage notification email for application {}: {}", app.getId(), e.getMessage());
        }
    }

    private String buildStageSubject(ApplicationStage stage, String jobTitle) {
        return switch (stage) {
            case HIRED   -> "[SmartHire] ✅ Chúc mừng bạn đã được tuyển dụng - " + jobTitle;
            case REJECTED -> "[SmartHire] Kết quả ứng tuyển - " + jobTitle;
            default -> "[SmartHire] Cập nhật trạng thái ứng tuyển - " + jobTitle;
        };
    }

    private String buildStageEmailBody(String candidateName, String jobTitle, ApplicationStage stage) {
        String message = switch (stage) {
            case HIRED   -> "Chúc mừng! Bạn đã chính thức được <strong>tuyển dụng</strong> cho vị trí <strong>" + jobTitle + "</strong>. Chào mừng bạn đến với đội ngũ!";
            case REJECTED -> "Cảm ơn bạn đã quan tâm đến vị trí <strong>" + jobTitle + "</strong>. Sau khi xem xét kỹ lưỡng, chúng tôi rất tiếc phải thông báo rằng hồ sơ của bạn chưa phù hợp trong đợt tuyển dụng này. Chúng tôi khuyến khích bạn tiếp tục theo dõi các cơ hội khác.";
            default -> "Trạng thái ứng tuyển của bạn cho vị trí <strong>" + jobTitle + "</strong> đã được cập nhật.";
        };

        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                    <h2 style="color: #2563eb;">SmartHire</h2>
                    <p>Xin chào <strong>%s</strong>,</p>
                    <p>%s</p>
                    <br/>
                    <p>Trân trọng,<br/><strong>Đội ngũ SmartHire</strong></p>
                </div>
                """.formatted(candidateName != null ? candidateName : "bạn", message);
    }

    private Long lookupCandidateUserId(Long candidateProfileId) {
        return candidateProfileRepository.findById(candidateProfileId)
                .map(profile -> profile.getUser().getId())
                .orElse(null);
    }

    private ApplicationStage parseStage(String stage) {
        try {
            return ApplicationStage.valueOf(stage.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid stage. Must be: APPLIED, INTERVIEW, HIRED, REJECTED");
        }
    }

    private ApplicationResponse toResponse(Application app) {
        return ApplicationResponse.builder()
                .id(app.getId())
                .jobId(app.getJob().getId())
                .candidateProfileId(app.getCandidateProfile().getId())
                .cvFileId(app.getCvFile() != null ? app.getCvFile().getId() : null)
                .stage(app.getStage())
                .appliedAt(app.getAppliedAt())
                .build();
    }
}
