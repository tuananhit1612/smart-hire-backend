package com.smarthire.backend.features.notification.controller;

import com.smarthire.backend.core.security.SecurityUtils;
import com.smarthire.backend.features.notification.dto.NotificationResponse;
import com.smarthire.backend.features.notification.service.NotificationService;
import com.smarthire.backend.shared.constants.ApiPaths;
import com.smarthire.backend.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(ApiPaths.NOTIFICATIONS)
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Danh sách notification (phân trang).
     * GET /api/notifications?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = SecurityUtils.getCurrentUserId();
        Page<NotificationResponse> notifications = notificationService.getNotifications(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    /**
     * Đếm notification chưa đọc.
     * GET /api/notifications/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("unreadCount", count)));
    }

    /**
     * Đánh dấu 1 notification đã đọc.
     * PATCH /api/notifications/{id}/read
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    /**
     * Đánh dấu tất cả notification đã đọc.
     * PATCH /api/notifications/read-all
     */
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsRead() {
        Long userId = SecurityUtils.getCurrentUserId();
        int count = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", Map.of("updated", count)));
    }
}
