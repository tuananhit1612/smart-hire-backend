package com.smarthire.backend.features.job.dto;

import com.smarthire.backend.shared.enums.JobLevel;
import com.smarthire.backend.shared.enums.JobStatus;
import com.smarthire.backend.shared.enums.JobType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class JobResponse {

    private Long id;
    private Long companyId;
    private String companyName;
    private String companyLogoUrl;
    private Long createdBy;
    private String title;
    private String description;
    private String requirements;
    private String benefits;
    private JobType jobType;
    private JobLevel jobLevel;
    private String location;
    private Boolean isRemote;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String salaryCurrency;
    private LocalDate deadline;
    private JobStatus status;
    private List<JobSkillDto> skills;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
