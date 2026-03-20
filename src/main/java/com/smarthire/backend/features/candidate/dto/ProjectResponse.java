package com.smarthire.backend.features.candidate.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {
    private Long id;
    private String projectName;
    private String description;
    private String technologies;
}
