package com.smarthire.backend.features.company.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCompanyRequest {

    @NotBlank(message = "Company name is required")
    private String name;

    private String website;
    private String industry;
    private String companySize;
    private String description;
    private String address;
    private String city;
}
