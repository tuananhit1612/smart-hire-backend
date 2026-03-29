package com.smarthire.backend.features.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class WeeklyActivityItem {
    private String day;         // "T2", "T3", "T4", "T5", "T6", "T7", "CN"
    private long applications;  // số đơn ứng tuyển trong ngày
    private long views;         // lượt xem hồ sơ (placeholder = 0)
}
