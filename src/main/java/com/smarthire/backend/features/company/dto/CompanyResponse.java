package com.smarthire.backend.features.company.dto;

import com.smarthire.backend.shared.enums.CompanySize;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class CompanyResponse {

    private Long id;
    private String name;
    private String logoUrl;
    private String coverUrl;
    private String website;
    private String industry;
    private CompanySize companySize;
    private String description;
    private String address;
    private String city;

    private String tagline;
    private String email;
    private String phone;
    private String founded;
    private List<String> techStack;
    private List<CompanyBenefitDto> benefits;
    private List<CompanySocialLinkDto> socialLinks;

    private Long createdBy;
    private Boolean isVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
