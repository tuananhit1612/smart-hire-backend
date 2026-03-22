package com.smarthire.backend.features.application.service;

import com.smarthire.backend.core.exception.BadRequestException;
import com.smarthire.backend.core.exception.ResourceNotFoundException;
import com.smarthire.backend.core.security.SecurityUtils;
import com.smarthire.backend.features.application.dto.ApplicationResponse;
import com.smarthire.backend.features.application.dto.ChangeStageRequest;
import com.smarthire.backend.features.application.dto.StageHistoryDto;
import com.smarthire.backend.features.application.entity.Application;
import com.smarthire.backend.features.application.entity.ApplicationStageHistory;
import com.smarthire.backend.features.application.repository.ApplicationRepository;
import com.smarthire.backend.features.auth.entity.User;
import com.smarthire.backend.features.candidate.entity.CandidateProfile;
import com.smarthire.backend.features.candidate.repository.CandidateProfileRepository;
import com.smarthire.backend.features.candidate.repository.CandidateProfileRepository;
import com.smarthire.backend.features.notification.dto.CreateNotificationRequest;
import com.smarthire.backend.features.notification.service.NotificationService;
import com.smarthire.backend.features.notification.service.RealtimeEventService;
import com.smarthire.backend.shared.enums.ApplicationStage;
import com.smarthire.backend.shared.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final EmailService emailService;
    private final RealtimeEventService realtimeEventService;
    private final CandidateProfileRepository candidateProfileRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(Long id) {
        Application app = findOrThrow(id);
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
    public ApplicationResponse changeStage(Long applicationId, ChangeStageRequest request) {
        User currentUser = SecurityUtils.getCurrentUser();
        Application app = findOrThrow(applicationId);

        ApplicationStage newStage = parseStage(request.getStage());
        ApplicationStage oldStage = app.getStage();

        if (oldStage == newStage) {
            throw new BadRequestException("Application is already in stage: " + newStage);
        }

        // Record history
        ApplicationStageHistory history = ApplicationStageHistory.builder()
                .application(app)
                .fromStage(oldStage)
                .toStage(newStage)
                .changedBy(currentUser)
                .note(request.getNote())
                .build();
        app.getStageHistory().add(history);
        app.setStage(newStage);

        Application saved = applicationRepository.save(app);
        log.info("Application {} stage changed: {} -> {} by {}", applicationId, oldStage, newStage, currentUser.getEmail());

        // Send email notification for significant stage transitions
        sendStageNotification(saved, oldStage, newStage);
        log.info("Application {} stage changed: {} -> {} by {}", applicationId, oldStage, newStage,
                currentUser.getEmail());

        // ── Phát realtime event qua WebSocket ──
        Long candidateUserId = lookupCandidateUserId(saved.getCandidateProfileId());
        realtimeEventService.publishStageChanged(saved, oldStage, newStage, currentUser.getId(), candidateUserId);

        // ── Tạo in-app notification cho candidate ──
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

    /**
     * Send email to candidate when application stage changes to a notifiable stage.
     */
    private void sendStageNotification(Application app, ApplicationStage oldStage, ApplicationStage newStage) {
        if (newStage != ApplicationStage.OFFER
                && newStage != ApplicationStage.HIRED
                && newStage != ApplicationStage.REJECTED) {
            return;
        }

        try {
            CandidateProfile profile = candidateProfileRepository.findById(app.getCandidateProfileId())
                    .orElse(null);
            if (profile == null || profile.getUser() == null) {
                log.warn("Cannot send stage email: candidate profile {} not found", app.getCandidateProfileId());
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
            case OFFER   -> "[SmartHire] 🎉 Bạn nhận được đề nghị công việc - " + jobTitle;
            case HIRED   -> "[SmartHire] ✅ Chúc mừng bạn đã được tuyển dụng - " + jobTitle;
            case REJECTED -> "[SmartHire] Kết quả ứng tuyển - " + jobTitle;
            default -> "[SmartHire] Cập nhật trạng thái ứng tuyển - " + jobTitle;
        };
    }

    private String buildStageEmailBody(String candidateName, String jobTitle, ApplicationStage stage) {
        String message = switch (stage) {
            case OFFER   -> "Chúng tôi vui mừng thông báo bạn đã nhận được <strong>đề nghị công việc</strong> cho vị trí <strong>" + jobTitle + "</strong>. Vui lòng đăng nhập vào hệ thống để xem chi tiết.";
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

    // ── Helpers ──

    private Application findOrThrow(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));
    }

    private ApplicationStage parseStage(String stage) {
        try {
            return ApplicationStage.valueOf(stage.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Invalid stage. Must be: APPLIED, SCREENING, INTERVIEW, OFFER, HIRED, REJECTED");
        }
    }

    /**
     * Lookup userId từ candidateProfileId.
     * Trả về null nếu không tìm thấy (event vẫn broadcast topic nhưng không gửi per-user).
     */
    private Long lookupCandidateUserId(Long candidateProfileId) {
        return candidateProfileRepository.findById(candidateProfileId)
                .map(profile -> profile.getUser().getId())
                .orElse(null);
    }

    private ApplicationResponse toResponse(Application app) {
        List<StageHistoryDto> historyDtos = app.getStageHistory().stream()
                .map(h -> StageHistoryDto.builder()
                        .id(h.getId())
                        .fromStage(h.getFromStage())
                        .toStage(h.getToStage())
                        .changedBy(h.getChangedBy().getId())
                        .note(h.getNote())
                        .createdAt(h.getCreatedAt())
                        .build())
                .toList();

        return ApplicationResponse.builder()
                .id(app.getId())
                .jobId(app.getJob().getId())
                .jobTitle(app.getJob().getTitle())
                .candidateProfileId(app.getCandidateProfileId())
                .cvFileId(app.getCvFileId())
                .stage(app.getStage())
                .appliedAt(app.getAppliedAt())
                .updatedAt(app.getUpdatedAt())
                .stageHistory(historyDtos)
                .build();
    }
}
