package com.smarthire.backend.features.application.service;

import com.smarthire.backend.core.exception.BadRequestException;
import com.smarthire.backend.core.exception.ResourceNotFoundException;
import com.smarthire.backend.core.security.SecurityUtils;
import com.smarthire.backend.features.application.dto.ApplyRequest;
import com.smarthire.backend.features.application.dto.ApplicationResponse;
import com.smarthire.backend.features.application.dto.ChangeStageRequest;
import com.smarthire.backend.features.application.dto.StageHistoryDto;
import com.smarthire.backend.features.application.entity.Application;
import com.smarthire.backend.features.application.entity.ApplicationStageHistory;
import com.smarthire.backend.features.application.repository.ApplicationRepository;
import com.smarthire.backend.features.auth.entity.User;
import com.smarthire.backend.features.candidate.entity.CandidateProfile;
import com.smarthire.backend.features.candidate.repository.CandidateProfileRepository;
import com.smarthire.backend.features.job.entity.Job;
import com.smarthire.backend.features.job.repository.JobRepository;
import com.smarthire.backend.features.notification.dto.CreateNotificationRequest;
import com.smarthire.backend.features.notification.service.NotificationService;
import com.smarthire.backend.features.notification.service.RealtimeEventService;
import com.smarthire.backend.shared.enums.ApplicationStage;
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
    private final RealtimeEventService realtimeEventService;
    private final CandidateProfileRepository candidateProfileRepository;
    private final NotificationService notificationService;
    private final JobRepository jobRepository;

    // ── Apply to a job ──

    @Override
    @Transactional
    public ApplicationResponse apply(ApplyRequest request) {
        User currentUser = SecurityUtils.getCurrentUser();

        // 1. Lookup candidate profile
        CandidateProfile profile = candidateProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new BadRequestException(
                        "Bạn cần tạo hồ sơ ứng viên trước khi ứng tuyển"));

        // 2. Validate job exists
        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job not found with id: " + request.getJobId()));

        // 3. Check duplicate application
        if (applicationRepository.existsByJobIdAndCandidateProfileId(
                request.getJobId(), profile.getId())) {
            throw new BadRequestException("Bạn đã ứng tuyển vào vị trí này rồi");
        }

        // 4. Create and save
        Application application = Application.builder()
                .job(job)
                .candidateProfileId(profile.getId())
                .cvFileId(request.getCvFileId())
                .stage(ApplicationStage.APPLIED)
                .build();

        Application saved = applicationRepository.save(application);
        log.info("User {} applied to job {} (application {})",
                currentUser.getEmail(), request.getJobId(), saved.getId());

        return toResponse(saved);
    }

    // ── Get my applications (candidate) ──

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMyApplications() {
        User currentUser = SecurityUtils.getCurrentUser();

        CandidateProfile profile = candidateProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Candidate profile not found"));

        List<Application> apps = applicationRepository
                .findByCandidateProfileIdOrderByAppliedAtDesc(profile.getId());
        return apps.stream().map(this::toResponse).toList();
    }

    // ── Withdraw application ──

    @Override
    @Transactional
    public void withdrawApplication(Long applicationId) {
        User currentUser = SecurityUtils.getCurrentUser();
        Application app = findOrThrow(applicationId);

        // Verify ownership
        CandidateProfile profile = candidateProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new BadRequestException("Candidate profile not found"));
        if (!app.getCandidateProfileId().equals(profile.getId())) {
            throw new BadRequestException("Bạn không có quyền rút đơn này");
        }

        // Only allow withdraw if still in APPLIED stage
        if (app.getStage() != ApplicationStage.APPLIED) {
            throw new BadRequestException(
                    "Chỉ có thể rút đơn khi đang ở trạng thái APPLIED");
        }

        applicationRepository.delete(app);
        log.info("User {} withdrew application {} for job {}",
                currentUser.getEmail(), applicationId, app.getJob().getId());
    }

    // ── Existing methods ──

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

