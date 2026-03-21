package com.smarthire.backend.features.cv.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CvBuilderResponse {

    private Long id;
    private Long cvFileId;
    private Long candidateProfileId;
    private String templateId;
    private Map<String, Object> sectionsData;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
