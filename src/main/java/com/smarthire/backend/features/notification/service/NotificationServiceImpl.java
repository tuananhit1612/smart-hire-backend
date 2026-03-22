package com.smarthire.backend.features.notification.service;

import com.smarthire.backend.features.notification.dto.CreateNotificationRequest;
import com.smarthire.backend.features.notification.dto.NotificationResponse;
import com.smarthire.backend.features.notification.entity.Notification;
import com.smarthire.backend.features.notification.repository.NotificationRepository;
import com.smarthire.backend.shared.dto.RealtimeEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final RealtimeEventService realtimeEventService;

    @Override
    @Transactional
    public NotificationResponse createNotification(CreateNotificationRequest request) {
        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .title(request.getTitle())
                .content(request.getContent())
                .referenceType(request.getReferenceType())
                .referenceId(request.getReferenceId())
                .build();

        notification = notificationRepository.save(notification);
        log.info("Created notification id={} for user={}, type={}", notification.getId(), request.getUserId(), request.getType());

        // Gửi realtime event qua WebSocket
        NotificationResponse response = toResponse(notification);
        RealtimeEvent<NotificationResponse> event = RealtimeEvent.<NotificationResponse>builder()
                .type("NEW_NOTIFICATION")
                .payload(response)
                .triggeredBy(request.getUserId())
                .build();
        realtimeEventService.sendToUser(request.getUserId().toString(), event);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));

        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied: notification does not belong to user");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
        log.info("Marked notification id={} as read", notificationId);
    }

    @Override
    @Transactional
    public int markAllAsRead(Long userId) {
        int count = notificationRepository.markAllAsReadByUserId(userId);
        log.info("Marked {} notifications as read for user={}", count, userId);
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    // ── Mapper ──

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .content(n.getContent())
                .referenceType(n.getReferenceType())
                .referenceId(n.getReferenceId())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
