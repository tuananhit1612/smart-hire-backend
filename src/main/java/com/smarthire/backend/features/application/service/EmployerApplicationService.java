package com.smarthire.backend.features.application.service;

import com.smarthire.backend.features.application.dto.ChangeStageRequest;
import com.smarthire.backend.features.application.dto.employer.*;
import org.springframework.data.domain.Pageable;

public interface EmployerApplicationService {
    ApplicantListResponse getApplicantsByJob(Long jobId, Long employerId, String search, String sortBy, Pageable pageable);
    
    EmployerApplicationResponse getApplicantDetail(Long jobId, Long applicantId, Long employerId);
    
    EmployerApplicationResponse updateStage(Long jobId, Long applicantId, Long employerId, ChangeStageRequest request);
    
    EmployerApplicationNoteResponse addNote(Long jobId, Long applicantId, Long employerId, AddApplicationNoteRequest request);
    
    AiAnalysisResponse reAnalyzeApplicant(Long jobId, Long applicantId, Long employerId);
}
