package com.smarthire.backend.features.candidate.dto;

import com.smarthire.backend.shared.enums.Gender;
import com.smarthire.backend.shared.enums.JobLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateProfileResponse {

    private Long id;
    
    // User info
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;

    // Profile info
    private String headline;
    private String summary;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String address;
    private String city;
    private Integer yearsOfExperience;
    private JobLevel jobLevel;
    
    // Meta
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
