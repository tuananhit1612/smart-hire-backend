package com.smarthire.backend.features.onboarding.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifiedCvData {
    private Long cvFileId;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String linkedin;
    private String website;
    private String country;
    private String state;
    private String city;
    private String gender;

    // Kỹ năng & Mục tiêu nghề nghiệp
    private String summary;
    private List<String> skills;

    // Chi tiết kinh nghiệm & học vấn
    private List<ExperienceData> experience;
    private List<EducationData> education;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExperienceData {
        private String company;
        private String title;
        private String startDate;
        private String endDate;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EducationData {
        private String school;
        private String degree;
        private String major;
        private String startDate;
        private String endDate;
    }
}
