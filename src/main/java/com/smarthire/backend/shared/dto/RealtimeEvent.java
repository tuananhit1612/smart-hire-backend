package com.smarthire.backend.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Generic DTO cho tất cả realtime events gửi qua WebSocket.
 *
 * @param <T> Kiểu payload data (khác nhau tùy event type)
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RealtimeEvent<T> {

    /** Event type: APPLICATION_SUBMITTED, APPLICATION_STAGE_CHANGED, AI_MATCHING_COMPLETED, ... */
    private String type;

    /** Payload data — nội dung event, khác nhau tùy type */
    private T payload;

    /** Thời điểm event xảy ra */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /** ID user đã trigger event này */
    private Long triggeredBy;
}
