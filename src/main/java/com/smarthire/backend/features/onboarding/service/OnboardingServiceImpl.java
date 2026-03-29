package com.smarthire.backend.features.onboarding.service;

import com.smarthire.backend.core.exception.BadRequestException;
import com.smarthire.backend.core.exception.ResourceNotFoundException;
import com.smarthire.backend.core.security.SecurityUtils;
import com.smarthire.backend.features.auth.entity.User;
import com.smarthire.backend.features.auth.repository.UserRepository;
import com.smarthire.backend.features.candidate.dto.CvFileResponse;
import com.smarthire.backend.features.candidate.entity.AiCvParsed;
import com.smarthire.backend.features.candidate.entity.CandidateProfile;
import com.smarthire.backend.features.candidate.entity.CandidateSkill;
import com.smarthire.backend.features.candidate.entity.CvFile;
import com.smarthire.backend.features.candidate.repository.AiCvParsedRepository;
import com.smarthire.backend.features.candidate.repository.CandidateEducationRepository;
import com.smarthire.backend.features.candidate.repository.CandidateExperienceRepository;
import com.smarthire.backend.features.candidate.repository.CandidateProfileRepository;
import com.smarthire.backend.features.candidate.repository.CandidateSkillRepository;
import com.smarthire.backend.features.candidate.repository.CvFileRepository;
import com.smarthire.backend.features.candidate.service.CvFileService;
import com.smarthire.backend.features.onboarding.dto.OnboardingCompleteRequest;
import com.smarthire.backend.features.onboarding.dto.ParseStatusResponse;
import com.smarthire.backend.features.onboarding.dto.UploadCvResponse;
import com.smarthire.backend.features.onboarding.dto.VerifiedCvData;
import com.smarthire.backend.infrastructure.ai.service.AiService;
import com.smarthire.backend.shared.enums.JobLevel;
import com.smarthire.backend.shared.enums.CvSource;
import com.smarthire.backend.shared.enums.ParseStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingServiceImpl implements OnboardingService {

    private final CandidateProfileRepository profileRepository;
    private final CvFileService cvFileService;
    private final CvFileRepository cvFileRepository;
    private final AiCvParsedRepository aiCvParsedRepository;
    private final UserRepository userRepository;
    private final AiService aiService;
    private final CandidateSkillRepository skillRepository;
    private final CandidateExperienceRepository experienceRepository;
    private final CandidateEducationRepository educationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public UploadCvResponse uploadCvForOnboarding(MultipartFile file) {
        User currentUser = SecurityUtils.getCurrentUser();

        // Check and create empty candidate profile if it doesn't exist yet
        if (!profileRepository.existsByUserId(currentUser.getId())) {
            CandidateProfile newProfile = CandidateProfile.builder()
                    .user(currentUser)
                    .build();
            profileRepository.save(newProfile);
        }

        // Delegate to existing CvFileService to store the file
        CvFileResponse cvFileResponse = cvFileService.uploadCv(file, CvSource.UPLOAD);

        CvFile cvFile = cvFileRepository.findById(cvFileResponse.getId())
                .orElseThrow(() -> new ResourceNotFoundException("CvFile", cvFileResponse.getId()));

        // Create AI Parsing entry
        AiCvParsed aiCvParsed = AiCvParsed.builder()
                .cvFile(cvFile)
                .status(ParseStatus.PROCESSING)
                .build();
        aiCvParsedRepository.save(aiCvParsed);

        return UploadCvResponse.builder()
                .cvFileId(cvFile.getId())
                .status(ParseStatus.PROCESSING.name())
                .message("Đang tiến hành trích xuất dữ liệu CV...")
                .build();
    }

    @Override
    @Transactional
    public ParseStatusResponse getParseStatus(Long cvFileId) {
        AiCvParsed aiCvParsed = aiCvParsedRepository.findByCvFileId(cvFileId)
                .orElseThrow(() -> new ResourceNotFoundException("AiCvParsed for CV", cvFileId));

        // Nếu đã COMPLETED trước đó → trả kết quả từ DB
        if (aiCvParsed.getStatus() == ParseStatus.COMPLETED && aiCvParsed.getParsedData() != null) {
            try {
                VerifiedCvData cachedData = objectMapper.readValue(aiCvParsed.getParsedData(), VerifiedCvData.class);
                cachedData.setCvFileId(cvFileId);
                return ParseStatusResponse.builder()
                        .status(ParseStatus.COMPLETED)
                        .message("Trích xuất thành công")
                        .data(cachedData)
                        .build();
            } catch (Exception e) {
                log.warn("Failed to read cached parsed data, re-parsing...", e);
            }
        }

        // Nếu PROCESSING hoặc PENDING → Gọi Gemini AI synchronous
        if (aiCvParsed.getStatus() == ParseStatus.PROCESSING || aiCvParsed.getStatus() == ParseStatus.PENDING) {
            try {
                log.info("🤖 Starting AI CV parse for cvFileId={}", cvFileId);
                VerifiedCvData parsedData = aiService.parseCvFile(cvFileId);

                // Nếu email trống → dùng email user hiện tại
                if (parsedData.getEmail() == null || parsedData.getEmail().isEmpty()) {
                    parsedData.setEmail(SecurityUtils.getCurrentUserEmail());
                }

                // Lưu kết quả vào DB
                aiCvParsed.setParsedData(objectMapper.writeValueAsString(parsedData));
                aiCvParsed.setStatus(ParseStatus.COMPLETED);
                aiCvParsedRepository.save(aiCvParsed);

                log.info("✅ AI CV parse completed for cvFileId={}", cvFileId);

                return ParseStatusResponse.builder()
                        .status(ParseStatus.COMPLETED)
                        .message("AI đã trích xuất CV thành công")
                        .data(parsedData)
                        .build();
            } catch (Exception e) {
                log.error("❌ AI CV parse failed for cvFileId={}: {}", cvFileId, e.getMessage());
                aiCvParsed.setStatus(ParseStatus.FAILED);
                aiCvParsed.setErrorMessage("AI parsing failed: " + e.getMessage());
                aiCvParsedRepository.save(aiCvParsed);

                return ParseStatusResponse.builder()
                        .status(ParseStatus.FAILED)
                        .message("Trích xuất CV thất bại. Vui lòng thử lại.")
                        .build();
            }
        }

        // FAILED status
        return ParseStatusResponse.builder()
                .status(aiCvParsed.getStatus())
                .message(aiCvParsed.getErrorMessage())
                .build();
    }

    @Override
    @Transactional
    public void completeOnboarding(OnboardingCompleteRequest request) {
        User currentUser = SecurityUtils.getCurrentUser();

        CandidateProfile profile = profileRepository.findByUserId(currentUser.getId())
                .orElseGet(() -> {
                    CandidateProfile newProfile = CandidateProfile.builder()
                            .user(currentUser)
                            .build();
                    return profileRepository.save(newProfile);
                });

        // 1. Update User
        if (request.getVerifiedCvData() != null) {
            VerifiedCvData cvData = request.getVerifiedCvData();
            String firstName = cvData.getFirstName() != null ? cvData.getFirstName().trim() : "";
            String lastName = cvData.getLastName() != null ? cvData.getLastName().trim() : "";
            String fullName = (firstName + " " + lastName).trim();

            if (!fullName.isEmpty()) {
                currentUser.setFullName(fullName);
            }
            if (cvData.getPhone() != null && !cvData.getPhone().isEmpty()) {
                currentUser.setPhone(cvData.getPhone());
            }
            userRepository.save(currentUser);

            // 2. Update CandidateProfile with detailed info
            if (cvData.getGender() != null && !cvData.getGender().isEmpty()) {
                try {
                    profile.setGender(com.smarthire.backend.shared.enums.Gender.valueOf(cvData.getGender().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid gender string received: {}", cvData.getGender());
                }
            }
            profile.setCountry(cvData.getCountry());
            profile.setState(cvData.getState());
            profile.setCity(cvData.getCity());
            profile.setLinkedinUrl(cvData.getLinkedin());
            profile.setPersonalWebsite(cvData.getWebsite());
            profile.setSummary(cvData.getSummary());

            // Save skills
            if (cvData.getSkills() != null && !cvData.getSkills().isEmpty()) {
                cvData.getSkills().forEach(skillName -> {
                    if (skillName != null && !skillName.trim().isEmpty()) {
                        skillRepository.save(CandidateSkill.builder()
                                .candidateProfile(profile)
                                .skillName(skillName.trim())
                                .proficiencyLevel(com.smarthire.backend.shared.enums.ProficiencyLevel.BEGINNER) // default
                                .build());
                    }
                });
            }

            // Save experiences
            if (cvData.getExperience() != null && !cvData.getExperience().isEmpty()) {
                cvData.getExperience().forEach(exp -> {
                    experienceRepository.save(com.smarthire.backend.features.candidate.entity.CandidateExperience.builder()
                            .candidateProfile(profile)
                            .companyName(exp.getCompany() != null ? exp.getCompany() : "")
                            .title(exp.getTitle() != null ? exp.getTitle() : "")
                            .startDate(exp.getStartDate() != null ? exp.getStartDate() : "")
                            .endDate(exp.getEndDate() != null ? exp.getEndDate() : "")
                            .description(exp.getDescription() != null ? exp.getDescription() : "")
                            .build());
                });
            }

            // Save educations
            if (cvData.getEducation() != null && !cvData.getEducation().isEmpty()) {
                cvData.getEducation().forEach(edu -> {
                    educationRepository.save(com.smarthire.backend.features.candidate.entity.CandidateEducation.builder()
                            .candidateProfile(profile)
                            .institution(edu.getSchool() != null ? edu.getSchool() : "")
                            .degree(edu.getDegree() != null ? edu.getDegree() : "")
                            .fieldOfStudy(edu.getMajor() != null ? edu.getMajor() : "")
                            .startDate(edu.getStartDate() != null ? edu.getStartDate() : "")
                            .endDate(edu.getEndDate() != null ? edu.getEndDate() : "")
                            .build());
                });
            }
        }

        // 3. Update job level and role
        try {
            JobLevel jobLevel = JobLevel.valueOf(request.getExperienceLevel().toUpperCase());
            profile.setJobLevel(jobLevel);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid experience level provided: {}", request.getExperienceLevel());
        }

        profile.setHeadline(request.getRoleId());

        profileRepository.save(profile);

        // Mark user as onboarded
        currentUser.setIsOnboarded(true);
        userRepository.save(currentUser);

        log.info("Onboarding completed for user: {}", currentUser.getEmail());
    }
}