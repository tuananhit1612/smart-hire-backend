package com.smarthire.backend.shared.enums;

/**
 * Các loại event realtime phát qua WebSocket.
 */
public enum EventType {
    APPLICATION_SUBMITTED,       // Candidate apply job
    APPLICATION_STAGE_CHANGED,   // HR đổi stage ứng viên
    AI_MATCHING_COMPLETED,       // AI chấm matching xong
    AI_CV_PARSED,                // AI parse CV xong
    AI_CV_REVIEWED               // AI review CV xong
}
