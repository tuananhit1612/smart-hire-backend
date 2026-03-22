package com.smarthire.backend.features.notification.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Internal DTO dùng trong service để tạo notification.
 * Không expose qua public API.
 */
@Getter
@Builder
public class CreateNotificationRequest {

    private Long userId;
    private String type;
    private String title;
    private String content;
    private String referenceType;
    private Long referenceId;
}
