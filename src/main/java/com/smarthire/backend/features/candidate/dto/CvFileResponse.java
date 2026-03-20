package com.smarthire.backend.features.candidate.dto;

import com.smarthire.backend.shared.enums.CvFileType;
import com.smarthire.backend.shared.enums.CvSource;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CvFileResponse {
    private Long id;
    private String fileName;
    private CvFileType fileType;
    private Integer fileSize;
    private CvSource source;
    private Boolean isPrimary;
    private LocalDateTime createdAt;
    private String downloadUrl;
}
