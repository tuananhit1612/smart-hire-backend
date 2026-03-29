package com.smarthire.backend.features.company.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateCompanyRequest {

    private String name;
    private String website;
    private String industry;
    private String companySize;
    private String description;
    private String address;
    private String city;
    
    private String tagline;
    private String email;
    private String phone;
    private String founded;
    private String coverUrl;
    private List<String> techStack;
    private List<CompanyBenefitDto> benefits;
    private List<CompanySocialLinkDto> socialLinks;
}
