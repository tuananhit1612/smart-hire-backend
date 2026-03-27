package com.smarthire.backend.features.application.dto.employer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployerApplicationNoteResponse {
    private Long id;
    private String text;
    private String author;
    private LocalDateTime createdAt;
}
