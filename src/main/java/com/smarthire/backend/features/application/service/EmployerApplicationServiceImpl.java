package com.smarthire.backend.features.application.service;

import com.smarthire.backend.core.exception.BadRequestException;
import com.smarthire.backend.core.exception.ResourceNotFoundException;
import com.smarthire.backend.features.application.dto.ChangeStageRequest;
import com.smarthire.backend.features.application.dto.employer.*;
import com.smarthire.backend.features.application.entity.Application;
import com.smarthire.backend.features.application.entity.ApplicationAiResult;
import com.smarthire.backend.features.application.entity.ApplicationNote;
import com.smarthire.backend.features.application.repository.ApplicationAiResultRepository;
import com.smarthire.backend.features.application.repository.ApplicationNoteRepository;
import com.smarthire.backend.features.application.repository.ApplicationRepository;
import com.smarthire.backend.features.auth.entity.User;
import com.smarthire.backend.features.auth.repository.UserRepository;
import com.smarthire.backend.features.candidate.entity.CandidateProfile;
import com.smarthire.backend.infrastructure.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployerApplicationServiceImpl implements EmployerApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationAiResultRepository aiResultRepository;
    private final ApplicationNoteRepository noteRepository;
    private final UserRepository userRepository;
    private final ApplicationService coreApplicationService;
    private final AiService aiService;

    @Override
    @Transactional(readOnly = true)
    public ApplicantListResponse getApplicantsByJob(Long jobId, Long employerId, String search, String sortBy, Pageable pageable) {
        // Simplified query for testing - in production should verify employerId owns jobId
        Page<Application> applications = applicationRepository.findByJobId(jobId, pageable);
        
        List<EmployerApplicationResponse> dtoList = applications.stream()
                .map(this::toEmployerResponse)
                .toList();
                
        // Filtering and sorting is simplified here for demonstration, ideally handled in DB
        return ApplicantListResponse.builder()
                .data(dtoList)
                .total(applications.getTotalElements())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployerApplicationResponse> getAllApplicantsForEmployer(Long employerId) {
        try {
            List<Application> applications = applicationRepository.findByJob_CreatedBy_IdOrderByAppliedAtDesc(employerId);
            return applications.stream()
                    .map(this::toEmployerResponse)
                    .toList();
        } catch (Exception e) {
            log.error("Error fetching applicants for employer {}: {}", employerId, e.getMessage(), e);
            return List.of(); // Return empty list instead of crashing
        }
    }

    @Override
    @Transactional
    public EmployerApplicationResponse getApplicantDetail(Long jobId, Long applicantId, Long employerId) {
        Application app = getApplicationAndVerifyEmployer(jobId, applicantId, employerId);
        
        // Auto-generate AI Result if missing
        if (app.getAiResult() == null) {
            try {
                ApplicationAiResult aiResult = aiService.matchCvWithJob(app);
                aiResult = aiResultRepository.save(aiResult);
                app.setAiResult(aiResult);
                log.info("✅ AI match generated for application {}", app.getId());
            } catch (Exception e) {
                log.error("❌ AI match failed for application {}: {}", app.getId(), e.getMessage());
                // Fallback: create basic result
                ApplicationAiResult fallback = ApplicationAiResult.builder()
                        .application(app)
                        .matchScore(0)
                        .skillMatch(0)
                        .experienceMatch(0)
                        .summary("AI analysis is currently unavailable. Please try re-analyzing.")
                        .strengths(List.of())
                        .gaps(List.of())
                        .build();
                app.setAiResult(aiResultRepository.save(fallback));
            }
        }
        
        return toEmployerResponse(app);
    }

    @Override
    @Transactional
    public EmployerApplicationResponse updateStage(Long jobId, Long applicantId, Long employerId, ChangeStageRequest request) {
        Application app = getApplicationAndVerifyEmployer(jobId, applicantId, employerId);
        coreApplicationService.changeStage(app.getId(), employerId, request);
        return getApplicantDetail(jobId, applicantId, employerId);
    }

    @Override
    @Transactional
    public EmployerApplicationNoteResponse addNote(Long jobId, Long applicantId, Long employerId, AddApplicationNoteRequest request) {
        Application app = getApplicationAndVerifyEmployer(jobId, applicantId, employerId);
        User author = userRepository.findById(employerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ApplicationNote note = ApplicationNote.builder()
                .application(app)
                .author(author)
                .text(request.getText())
                .build();

        note = noteRepository.save(note);
        
        return EmployerApplicationNoteResponse.builder()
                .id(note.getId())
                .text(note.getText())
                .author(author.getFullName())
                .createdAt(note.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public AiAnalysisResponse reAnalyzeApplicant(Long jobId, Long applicantId, Long employerId) {
        Application app = getApplicationAndVerifyEmployer(jobId, applicantId, employerId);
        
        ApplicationAiResult result = app.getAiResult();
        if (result != null) {
            aiResultRepository.delete(result);
            aiResultRepository.flush();
            app.setAiResult(null);
        }
        
        try {
            ApplicationAiResult newResult = aiService.matchCvWithJob(app);
            newResult = aiResultRepository.save(newResult);
            app.setAiResult(newResult);
            log.info("✅ AI re-analysis completed for application {}", app.getId());
            return toAiAnalysisResponse(newResult);
        } catch (Exception e) {
            log.error("❌ AI re-analysis failed: {}", e.getMessage());
            ApplicationAiResult fallback = ApplicationAiResult.builder()
                    .application(app)
                    .matchScore(0)
                    .skillMatch(0)
                    .experienceMatch(0)
                    .summary("AI re-analysis failed: " + e.getMessage())
                    .strengths(List.of())
                    .gaps(List.of("AI service error"))
                    .build();
            fallback = aiResultRepository.save(fallback);
            app.setAiResult(fallback);
            return toAiAnalysisResponse(fallback);
        }
    }

    private Application getApplicationAndVerifyEmployer(Long jobId, Long applicantId, Long employerId) {
        Application app = applicationRepository.findById(applicantId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
                
        if (!app.getJob().getId().equals(jobId)) {
            throw new BadRequestException("Application does not belong to this job");
        }
        
        // TODO: Verify employerId actually owns this job's company
        return app;
    }

    private EmployerApplicationResponse toEmployerResponse(Application app) {
        CandidateProfile cp = app.getCandidateProfile();
        
        // Lấy skills từ Job requirements thay vì hardcode
        List<String> skills = List.of();
        if (app.getJob().getSkills() != null) {
            skills = app.getJob().getSkills().stream()
                    .map(s -> s.getSkillName())
                    .toList();
        }

        AiAnalysisResponse ai = null;
        if (app.getAiResult() != null) {
            ai = toAiAnalysisResponse(app.getAiResult());
        }

        List<EmployerApplicationActivityResponse> activities = app.getHistory().stream()
                .map(h -> EmployerApplicationActivityResponse.builder()
                        .id(h.getId())
                        .action(h.getToStage() != null ? h.getToStage().name() : "APPLIED")
                        .timestamp(h.getCreatedAt())
                        .build())
                .toList();

        List<EmployerApplicationNoteResponse> notes = app.getNotes().stream()
                .map(n -> EmployerApplicationNoteResponse.builder()
                        .id(n.getId())
                        .text(n.getText())
                        .author(n.getAuthor().getFullName())
                        .createdAt(n.getCreatedAt())
                        .build())
                .toList();

        return EmployerApplicationResponse.builder()
                .id(app.getId())
                .jobId(app.getJob().getId())
                .candidateId(cp.getId())
                .jobTitle(app.getJob().getTitle())
                .name(cp.getUser().getFullName())
                .email(cp.getUser().getEmail())
                .phone("0123456789")
                .avatarUrl(null)
                .currentTitle("Software Engineer")
                .experienceYears(2)
                .status(app.getStage())
                .appliedAt(app.getAppliedAt())
                .aiAnalysis(ai)
                .skills(skills)
                .activities(activities)
                .notes(notes)
                .build();
    }

    private AiAnalysisResponse toAiAnalysisResponse(ApplicationAiResult r) {
        return AiAnalysisResponse.builder()
                .matchScore(r.getMatchScore())
                .summary(r.getSummary())
                .breakdown(AiAnalysisResponse.ScoreBreakdown.builder()
                        .skillMatch(r.getSkillMatch())
                        .experienceMatch(r.getExperienceMatch())
                        .build())
                .strengths(r.getStrengths())
                .gaps(r.getGaps())
                .build();
    }
}
