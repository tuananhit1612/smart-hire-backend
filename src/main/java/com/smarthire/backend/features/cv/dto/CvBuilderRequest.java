package com.smarthire.backend.features.cv.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CvBuilderRequest {

    /** Tên CV (tuỳ chọn) — dùng khi tạo mới hoặc đổi tên */
    private String title;

    private String templateId;

    @NotNull(message = "Sections data cannot be null")
    private Map<String, Object> sectionsData;
}
