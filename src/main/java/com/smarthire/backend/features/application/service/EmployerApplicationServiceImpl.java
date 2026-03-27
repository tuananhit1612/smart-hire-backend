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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployerApplicationServiceImpl implements EmployerApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationAiResultRepository aiResultRepository;
    private final ApplicationNoteRepository noteRepository;
    private final UserRepository userRepository;
    private final ApplicationService coreApplicationService;

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
    @Transactional
    public EmployerApplicationResponse getApplicantDetail(Long jobId, Long applicantId, Long employerId) {
        Application app = getApplicationAndVerifyEmployer(jobId, applicantId, employerId);
        
        // Auto-generate AI Result if missing for demo purposes
        if (app.getAiResult() == null) {
            app.setAiResult(generateMockAiResult(app));
            applicationRepository.save(app);
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
        }
        
        ApplicationAiResult newResult = generateMockAiResult(app);
        newResult = aiResultRepository.save(newResult);
        app.setAiResult(newResult);
        
        return toAiAnalysisResponse(newResult);
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

    private ApplicationAiResult generateMockAiResult(Application app) {
        Random rnd = new Random();
        int score = 60 + rnd.nextInt(35); // 60-95
        return ApplicationAiResult.builder()
                .application(app)
                .matchScore(score)
                .skillMatch(score - 5)
                .experienceMatch(score + 2)
                .summary("Ứng viên có kỹ năng phù hợp nhưng thiếu một số kinh nghiệm thực tế trong các dự án quy mô lớn.")
                .strengths(Arrays.asList("Kỹ năng lập trình tốt", "Thái độ học hỏi", "Project cá nhân đa dạng"))
                .gaps(Arrays.asList("Kinh nghiệm thực tế chưa nhiều", "Chưa làm việc với microservices"))
                .build();
    }

    private EmployerApplicationResponse toEmployerResponse(Application app) {
        CandidateProfile cp = app.getCandidateProfile();
        
        List<String> skills = Arrays.asList("Java", "Spring Boot", "React", "TypeScript");
        if (app.getCvFile() != null) {
            skills = Arrays.asList("Spring Boot", "AWS", "SQL", "Docker");
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
