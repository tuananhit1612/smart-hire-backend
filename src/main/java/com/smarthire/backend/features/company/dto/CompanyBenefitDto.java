package com.smarthire.backend.features.company.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyBenefitDto {
    private String id;
    private String icon;
    private String title;
    private String description;
}
