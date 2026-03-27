package com.smarthire.backend.features.application.dto.employer;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddApplicationNoteRequest {
    @NotBlank(message = "Note text cannot be blank")
    private String text;
}
