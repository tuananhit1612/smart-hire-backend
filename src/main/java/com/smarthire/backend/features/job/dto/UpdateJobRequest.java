package com.smarthire.backend.features.job.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class UpdateJobRequest {

    private String title;
    private String description;
    private String requirements;
    private String benefits;
    private String jobType;
    private String jobLevel;
    private String location;
    private Boolean isRemote;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String salaryCurrency;
    private LocalDate deadline;
    private List<JobSkillDto> skills;
}
