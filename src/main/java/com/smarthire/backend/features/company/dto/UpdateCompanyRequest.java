package com.smarthire.backend.features.company.dto;

import lombok.Getter;
import lombok.Setter;

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
}
