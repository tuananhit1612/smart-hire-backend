package com.smarthire.backend.features.notification.service;

import com.smarthire.backend.features.application.entity.Application;
import com.smarthire.backend.shared.dto.RealtimeEvent;
import com.smarthire.backend.shared.enums.ApplicationStage;
import com.smarthire.backend.shared.enums.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service phát realtime event qua WebSocket STOMP.
 *
 * Destinations:
 * - /topic/jobs/{jobId}/applications  → broadcast cho HR xem job
 * - /user/{userId}/queue/notifications → gửi cho user cụ thể
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RealtimeEventService {

    private final SimpMessagingTemplate messagingTemplate;

    // ── Generic send methods ──

    /**
     * Gửi event broadcast tới topic (e.g. /topic/jobs/5/applications).
     */
    public <T> void sendToTopic(String destination, RealtimeEvent<T> event) {
        log.info("Sending event [{}] to topic: {}", event.getType(), destination);
        messagingTemplate.convertAndSend(destination, event);
    }

    /**
     * Gửi event cho user cụ thể qua /user/{userId}/queue/notifications.
     * Spring tự route dựa trên userId (Principal name).
     */
    public <T> void sendToUser(String userId, RealtimeEvent<T> event) {
        log.info("Sending event [{}] to user: {}", event.getType(), userId);
        messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", event);
    }

    // ── Helper: Stage changed event ──

    /**
     * Phát event khi HR đổi stage ứng viên.
     * - Broadcast tới /topic/jobs/{jobId}/applications (HR team)
     * - Gửi cho candidate user qua /user/{candidateUserId}/queue/notifications
     *
     * @param app        Application sau khi đã update stage
     * @param oldStage   Stage cũ
     * @param newStage   Stage mới
     * @param changedBy  userId của HR đã đổi stage
     * @param candidateUserId userId của candidate (cần lookup bên ngoài)
     */
    public void publishStageChanged(Application app, ApplicationStage oldStage,
                                     ApplicationStage newStage, Long changedBy,
                                     Long candidateUserId) {
        Map<String, Object> payload = Map.of(
                "applicationId", app.getId(),
                "jobId", app.getJob().getId(),
                "jobTitle", app.getJob().getTitle(),
                "fromStage", oldStage.name(),
                "toStage", newStage.name()
        );

        RealtimeEvent<Map<String, Object>> event = RealtimeEvent.<Map<String, Object>>builder()
                .type(EventType.APPLICATION_STAGE_CHANGED.name())
                .payload(payload)
                .triggeredBy(changedBy)
                .build();

        // Broadcast cho HR team đang xem job này
        String topicDest = "/topic/jobs/" + app.getJob().getId() + "/applications";
        sendToTopic(topicDest, event);

        // Gửi cho candidate
        if (candidateUserId != null) {
            sendToUser(candidateUserId.toString(), event);
        }

        log.info("Published STAGE_CHANGED event: app={}, {} → {}", app.getId(), oldStage, newStage);
    }

    // ── Helper: Application submitted event (sẵn cho BE020) ──

    /**
     * Phát event khi candidate apply job.
     *
     * @param app             Application vừa tạo
     * @param candidateUserId userId của candidate
     */
    public void publishApplicationSubmitted(Application app, Long candidateUserId) {
        Map<String, Object> payload = Map.of(
                "applicationId", app.getId(),
                "jobId", app.getJob().getId(),
                "jobTitle", app.getJob().getTitle(),
                "candidateProfileId", app.getCandidateProfile().getId(),
                "stage", app.getStage().name()
        );

        RealtimeEvent<Map<String, Object>> event = RealtimeEvent.<Map<String, Object>>builder()
                .type(EventType.APPLICATION_SUBMITTED.name())
                .payload(payload)
                .triggeredBy(candidateUserId)
                .build();

        // Broadcast cho HR team
        String topicDest = "/topic/jobs/" + app.getJob().getId() + "/applications";
        sendToTopic(topicDest, event);

        // Xác nhận cho candidate
        if (candidateUserId != null) {
            sendToUser(candidateUserId.toString(), event);
        }

        log.info("Published APPLICATION_SUBMITTED event: app={}, job={}", app.getId(), app.getJob().getId());
    }

    // ── Helper: AI completed event (sẵn cho AI001-AI005) ──

    /**
     * Phát event khi AI xử lý xong (matching, parsing, review).
     *
     * @param eventType  Loại AI event
     * @param payload    Kết quả AI
     * @param userId     User nhận kết quả
     */
    public <T> void publishAICompleted(EventType eventType, T payload, Long userId) {
        RealtimeEvent<T> event = RealtimeEvent.<T>builder()
                .type(eventType.name())
                .payload(payload)
                .triggeredBy(userId)
                .build();

        sendToUser(userId.toString(), event);
        log.info("Published {} event for user={}", eventType.name(), userId);
    }
}
