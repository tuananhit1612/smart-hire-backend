package com.smarthire.backend.features.notification.service;

import com.smarthire.backend.features.notification.dto.CreateNotificationRequest;
import com.smarthire.backend.features.notification.dto.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    NotificationResponse createNotification(CreateNotificationRequest request);

    Page<NotificationResponse> getNotifications(Long userId, Pageable pageable);

    void markAsRead(Long notificationId, Long userId);

    int markAllAsRead(Long userId);

    long getUnreadCount(Long userId);
}
