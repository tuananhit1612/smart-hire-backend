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
import com.smarthire.backend.features.candidate.repository.CandidateProfileRepository;
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
