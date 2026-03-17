package com.smarthire.backend.features.candidate.dto;

import com.smarthire.backend.shared.enums.Gender;
import com.smarthire.backend.shared.enums.JobLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateProfileRequest {

    @Size(max = 255, message = "Headline must be less than 255 characters")
    private String headline;

    private String summary;

    private LocalDate dateOfBirth;

    private Gender gender;

    @Size(max = 500, message = "Address must be less than 500 characters")
    private String address;

    @Size(max = 100, message = "City must be less than 100 characters")
    private String city;

    @Min(value = 0, message = "Years of experience cannot be negative")
    private Integer yearsOfExperience;

    private JobLevel jobLevel;
}
