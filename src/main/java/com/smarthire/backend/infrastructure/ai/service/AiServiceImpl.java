package com.smarthire.backend.infrastructure.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthire.backend.core.exception.ResourceNotFoundException;
import com.smarthire.backend.features.application.entity.Application;
import com.smarthire.backend.features.application.entity.ApplicationAiResult;
import com.smarthire.backend.features.candidate.entity.AiCvReview;
import com.smarthire.backend.features.candidate.entity.CvFile;
import com.smarthire.backend.features.candidate.repository.CvFileRepository;
import com.smarthire.backend.features.job.entity.Job;
import com.smarthire.backend.features.onboarding.dto.OnboardingAiVerificationResult;
import com.smarthire.backend.features.onboarding.dto.VerifiedCvData;
import com.smarthire.backend.infrastructure.ai.client.OllamaClient;
import com.smarthire.backend.infrastructure.ai.client.CvTextExtractor;
import com.smarthire.backend.infrastructure.ai.client.FptAiClient;
import com.smarthire.backend.infrastructure.ai.client.GeminiClient;
import com.smarthire.backend.infrastructure.ai.dto.FptAiIdrResponse;
import com.smarthire.backend.infrastructure.ai.prompts.PromptTemplates;
import com.smarthire.backend.infrastructure.storage.FileStorageService;
import com.smarthire.backend.shared.enums.Gender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * AI Service Implementation.
 * Sử dụng GeminiClient để gọi Gemini API cho 3 chức năng AI.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiServiceImpl implements AiService {

    private final OllamaClient ollamaClient;
    private final CvTextExtractor cvTextExtractor;
    private final GeminiClient geminiClient;
    private final FptAiClient fptAiClient;
    private final CvFileRepository cvFileRepository;
    private final FileStorageService fileStorageService;
    private final com.smarthire.backend.features.candidate.repository.AiCvReviewRepository aiCvReviewRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public VerifiedCvData parseCvFile(Long cvFileId) {
        log.info("🤖 AI Parse CV — cvFileId={}", cvFileId);

        CvFile cvFile = cvFileRepository.findById(cvFileId)
                .orElseThrow(() -> new ResourceNotFoundException("CvFile", cvFileId));

        // Lấy path file thật trên disk
        Path filePath = fileStorageService.getFilePath(cvFile.getFilePath());
        String mimeType = getMimeType(cvFile.getFileName());

        // Extract raw text locally
        String cvText = cvTextExtractor.extractText(filePath, mimeType);

        // Gọi Ollama
        String prompt = String.format(PromptTemplates.CV_PARSE_PROMPT, cvText);
        String aiResponse = ollamaClient.chat(prompt);

        // Parse JSON response → VerifiedCvData
        return parseCvResponse(aiResponse, cvFileId);
    }

    @Override
    public ApplicationAiResult matchCvWithJob(Application application) {
        log.info("🤖 AI Match CV-JD — applicationId={}", application.getId());

        // Lấy nội dung CV (parsed data hoặc file name as fallback)
        String cvContent = extractCvContent(application);

        // Lấy JD từ Job
        var job = application.getJob();
        String skills = job.getSkills() != null
                ? job.getSkills().stream()
                    .map(s -> s.getSkillName())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("N/A")
                : "N/A";

        // Build prompt với CV + JD
        String prompt = String.format(PromptTemplates.CV_JOB_MATCH_PROMPT,
                cvContent,
                job.getTitle(),
                job.getDescription() != null ? job.getDescription() : "N/A",
                job.getRequirements() != null ? job.getRequirements() : "N/A",
                skills,
                job.getJobLevel() != null ? job.getJobLevel().name() : "N/A"
        );

        // Gọi Ollama text chat
        String aiResponse = ollamaClient.chat(prompt);

        // Parse JSON → ApplicationAiResult
        return parseMatchResponse(aiResponse, application);
    }

    @Override
    public AiCvReview reviewCvFile(Long cvFileId) {
        log.info("🤖 AI Review CV — cvFileId={}", cvFileId);

        CvFile cvFile = cvFileRepository.findById(cvFileId)
                .orElseThrow(() -> new ResourceNotFoundException("CvFile", cvFileId));

        Path filePath = fileStorageService.getFilePath(cvFile.getFilePath());
        String mimeType = getMimeType(cvFile.getFileName());

        // Load rules
        String rules = "[]";
        try {
            org.springframework.core.io.Resource resource = new org.springframework.core.io.ClassPathResource("ai/cv-review-rules.json");
            if (resource.exists()) {
                rules = org.springframework.util.StreamUtils.copyToString(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.warn("Could not load cv-review-rules.json: {}", e.getMessage());
        }

        String cvText = cvTextExtractor.extractText(filePath, mimeType);

        String prompt = String.format(PromptTemplates.CV_REVIEW_PROMPT, rules, cvText);

        // Gọi Ollama với prompt (chứa text, rules)
        String aiResponse = ollamaClient.chat(prompt);

        // Parse JSON → AiCvReview entity
        return parseReviewResponse(aiResponse, cvFile);
    }

    @Override
    public String optimizeCv(Long cvFileId) {
        log.info("🤖 AI Optimize CV — cvFileId={}", cvFileId);

        CvFile cvFile = cvFileRepository.findById(cvFileId)
                .orElseThrow(() -> new ResourceNotFoundException("CvFile", cvFileId));

        Path filePath = fileStorageService.getFilePath(cvFile.getFilePath());
        String mimeType = getMimeType(cvFile.getFileName());

        // Get latest review to include in optimize prompt
        String reviewJson = "[]";
        try {
            var reviewOpt = aiCvReviewRepository.findTopByCvFileIdOrderByCreatedAtDesc(cvFileId);
            if (reviewOpt.isPresent()) {
                AiCvReview latestReview = reviewOpt.get();
                reviewJson = latestReview.getSectionScores() != null ? latestReview.getSectionScores() : "[]";
            }
        } catch (Exception e) {
            log.warn("Could not load latest review for optimization: {}", e.getMessage());
        }

        String cvText = cvTextExtractor.extractText(filePath, mimeType);

        String prompt = String.format(PromptTemplates.CV_OPTIMIZE_PROMPT, reviewJson, cvText);
        String aiResponse = ollamaClient.chat(prompt);

        return cleanJsonResponse(aiResponse);
    }

    @Override
    public String generateInterviewQuestions(Application application) {
        Job job = application.getJob();
        String cvText = extractCvContent(application);

        String prompt = String.format(
                PromptTemplates.GENERATE_INTERVIEW_QUESTIONS_PROMPT,
                cvText != null ? cvText : "Candidate CV content not available",
                job.getTitle(),
                job.getDescription() != null ? job.getDescription() : "N/A",
                job.getRequirements() != null ? job.getRequirements() : "N/A",
                job.getSkills() != null ? job.getSkills().stream().map(s -> s.getSkillName()).reduce((a, b) -> a + ", " + b).orElse("N/A") : "N/A",
                job.getJobLevel() != null ? job.getJobLevel().name() : "N/A"
        );

        String aiResponse = ollamaClient.chat(prompt);
        return cleanJsonResponse(aiResponse);
    }

    @Override
    public String evaluateVirtualInterviewAnswer(String jobTitle, String question, String answer) {
        String prompt = String.format(
                PromptTemplates.VIRTUAL_INTERVIEW_EVALUATION_PROMPT,
                jobTitle,
                question,
                answer
        );

        String aiResponse = ollamaClient.chat(prompt);
        return cleanJsonResponse(aiResponse);
    }

    @Override
    public OnboardingAiVerificationResult verifyIdCardImage(Path imagePath, String mimeType) {
        log.info("🤖 AI Verify ID Card Image (FPT AI) — filePath={}", imagePath.getFileName());

        try {
            // Call FPT AI IDR API
            FptAiIdrResponse response = fptAiClient.recognizeIdCard(imagePath);
            
            if (response == null || response.getErrorCode() != 0) {
                String errorMsg = response != null ? response.getErrorMessage() : "Không nhận được phản hồi từ FPT AI";
                log.error("❌ FPT AI IDR Error: {} (code={})", errorMsg, response != null ? response.getErrorCode() : -1);
                
                return OnboardingAiVerificationResult.builder()
                        .isValid(false)
                        .feedbackReason("Lỗi hệ thống AI (FPT): " + errorMsg)
                        .build();
            }

            if (response.getData() == null || response.getData().isEmpty()) {
                return OnboardingAiVerificationResult.builder()
                        .isValid(false)
                        .feedbackReason("Không tìm thấy dữ liệu trên ảnh CMND/CCCD. Vui lòng chụp rõ nét hơn.")
                        .build();
            }

            // Lấy kết quả đầu tiên (thường chỉ có 1 card trên 1 ảnh)
            FptAiIdrResponse.FptAiIdrData data = response.getData().get(0);
            
            // Log full response for debugging
            try {
                log.info("🔍 FPT AI Raw JSON for Type {}: {}", data.getType(), objectMapper.writeValueAsString(data));
            } catch (Exception ignored) {}

            FptAiIdrResponse.FptAiIdrInfo info = data.getInfo();
            
            // Extract fields flexibly from either 'info' or flat 'data'
            String finalName = info != null && info.getName() != null ? info.getName() : data.getName();
            String finalId = info != null && info.getId() != null ? info.getId() : data.getId();
            String finalDob = info != null && info.getDob() != null ? info.getDob() : data.getDob();

            // Check if it's the back side - we don't expect a name/dob here
            boolean isBackSide = data.getType() != null && data.getType().toLowerCase().contains("back");

            if (finalName == null && finalId == null && !isBackSide) {
                return OnboardingAiVerificationResult.builder()
                        .isValid(false)
                        .feedbackReason("Không thể trích xuất thông tin từ giấy tờ. Hình ảnh có thể bị mờ hoặc bị cắt góc.")
                        .build();
            }

            // Kiểm tra tính hợp lệ & giả mạo (Anti-spoofing)
            boolean isValid = true;
            StringBuilder feedback = new StringBuilder();

            // 1. Kiểm tra cảnh báo (warning) từ FPT AI
            String warning = data.getWarning();
            if (warning != null && !warning.isBlank()) {
                log.warn("⚠️ FPT AI Warning detected: {}", warning);
                isValid = false;
                feedback.append("Cảnh báo AI: ").append(translateWarning(warning)).append(". ");
            }

            // 2. Kiểm tra độ tin cậy (Probability) - Ngưỡng đề xuất 0.6
            // Determine which fields to check based on front/back
            if (!isBackSide) {
                Double nameProb = data.getProbability() != null ? data.getProbability().get("name") : data.getNameProb();
                Double idProb = data.getProbability() != null ? data.getProbability().get("id") : data.getIdProb();
                
                if ((nameProb != null && nameProb < 0.6) || (idProb != null && idProb < 0.6)) {
                    log.warn("⚠️ Low confidence detected (Front): name={}, id={}", nameProb, idProb);
                    isValid = false;
                    feedback.append("Ảnh mờ hoặc bị chói sáng, không thể xác minh rõ họ tên và số giấy tờ. ");
                }
            } else {
                Double issueProb = null;
                if (data.getProbability() != null) {
                    issueProb = data.getProbability().get("issue_date") != null 
                        ? data.getProbability().get("issue_date") 
                        : data.getProbability().get("issueDate");
                }
                
                if (issueProb != null && issueProb < 0.6) {
                    log.warn("⚠️ Low confidence detected (Back): issue_date={}", issueProb);
                    isValid = false;
                    feedback.append("Ảnh mặt sau mờ, không thể đọc ngày cấp rõ ràng. ");
                }
            }

            if (isValid) {
                feedback.append(isBackSide ? "Mặt sau CCCD hợp lệ." : "Hồ sơ CCCD hợp lệ và đã được xác thực qua FPT AI.");
            }

            return OnboardingAiVerificationResult.builder()
                    .isValid(isValid)
                    .feedbackReason(feedback.toString().trim())
                    .extractedName(finalName)
                    .extractedIdNumber(finalId)
                    .extractedDob(finalDob)
                    .build();

        } catch (Exception e) {
            log.error("❌ Failed to verify ID card via FPT AI: {}", e.getMessage(), e);
            return OnboardingAiVerificationResult.builder()
                    .isValid(false)
                    .feedbackReason("Lỗi kết nối hoặc xử lý API FPT AI: " + e.getMessage())
                    .build();
        }
    }

    private String translateWarning(String warning) {
        if (warning == null) return "";
        return switch (warning.toLowerCase()) {
            case "screen_shot" -> "Phát hiện ảnh chụp màn hình (không chấp nhận)";
            case "photocopy" -> "Phát hiện ảnh photocopy (vui lòng dùng ảnh gốc)";
            case "fake" -> "Phát hiện dấu hiệu giả mạo căn cước";
            case "edited" -> "Ảnh có dấu hiệu chỉnh sửa kỹ thuật số";
            default -> "Hình ảnh không đạt chuẩn xác minh (lỗi: " + warning + ")";
        };
    }

    // ==============================================================================================
    // PRIVATE HELPER METHODS
    // ==============================================================================================

    private String getMimeType(String fileName) {
        if (fileName == null) return "application/pdf";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".doc")) return "application/msword";
        return "application/pdf";
    }

    private String extractCvContent(Application application) {
        // Thử lấy parsed data từ AiCvParsed nếu có
        try {
            CvFile cvFile = application.getCvFile();
            if (cvFile != null) {
                // Extract text content locally
                Path filePath = fileStorageService.getFilePath(cvFile.getFilePath());
                String mimeType = getMimeType(cvFile.getFileName());
                return cvTextExtractor.extractText(filePath, mimeType);
            }
        } catch (Exception e) {
            log.warn("Could not extract CV content, using fallback: {}", e.getMessage());
        }
        return "No CV content available";
    }

    /**
     * Clean AI response — loại bỏ markdown code blocks nếu có.
     */
    private String cleanJsonResponse(String response) {
        if (response == null) return "{}";
        String cleaned = response.trim();
        // Remove ```json ... ``` wrapper
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            int lastBacktick = cleaned.lastIndexOf("```");
            if (firstNewline > 0 && lastBacktick > firstNewline) {
                cleaned = cleaned.substring(firstNewline + 1, lastBacktick).trim();
            }
        }
        return cleaned;
    }

    private VerifiedCvData parseCvResponse(String aiResponse, Long cvFileId) {
        try {
            String json = cleanJsonResponse(aiResponse);
            JsonNode node = objectMapper.readTree(json);

            String genderStr = getTextOrEmpty(node, "gender");
            if (!genderStr.isEmpty()) {
                try {
                    Gender.valueOf(genderStr.toUpperCase());
                } catch (Exception ignored) {
                    genderStr = null;
                }
            } else {
                genderStr = null;
            }
            // Parse skills array
            List<String> skills = jsonArrayToList(node, "skills");

            // Parse experience array
            List<VerifiedCvData.ExperienceData> experienceList = new ArrayList<>();
            if (node.has("experience") && node.get("experience").isArray()) {
                for (JsonNode expNode : node.get("experience")) {
                    experienceList.add(VerifiedCvData.ExperienceData.builder()
                            .company(getTextOrEmpty(expNode, "company"))
                            .title(getTextOrEmpty(expNode, "title"))
                            .startDate(getTextOrEmpty(expNode, "startDate"))
                            .endDate(getTextOrEmpty(expNode, "endDate"))
                            .description(getTextOrEmpty(expNode, "description"))
                            .build());
                }
            }

            // Parse education array
            List<VerifiedCvData.EducationData> educationList = new ArrayList<>();
            if (node.has("education") && node.get("education").isArray()) {
                for (JsonNode eduNode : node.get("education")) {
                    educationList.add(VerifiedCvData.EducationData.builder()
                            .school(getTextOrEmpty(eduNode, "school"))
                            .degree(getTextOrEmpty(eduNode, "degree"))
                            .major(getTextOrEmpty(eduNode, "major"))
                            .startDate(getTextOrEmpty(eduNode, "startDate"))
                            .endDate(getTextOrEmpty(eduNode, "endDate"))
                            .build());
                }
            }

            return VerifiedCvData.builder()
                    .cvFileId(cvFileId)
                    .firstName(getTextOrEmpty(node, "firstName"))
                    .lastName(getTextOrEmpty(node, "lastName"))
                    .phone(getTextOrEmpty(node, "phone"))
                    .email(getTextOrEmpty(node, "email"))
                    .linkedin(getTextOrEmpty(node, "linkedin"))
                    .website(getTextOrEmpty(node, "website"))
                    .country(getTextOrEmpty(node, "country"))
                    .state(getTextOrEmpty(node, "state"))
                    .city(getTextOrEmpty(node, "city"))
                    .gender(genderStr != null ? genderStr.toUpperCase() : null)
                    .summary(getTextOrEmpty(node, "summary"))
                    .skills(skills)
                    .experience(experienceList)
                    .education(educationList)
                    .build();
        } catch (Exception e) {
            log.error("❌ Failed to parse CV AI response: {}", e.getMessage());
            log.debug("Raw AI response: {}", aiResponse);
            return VerifiedCvData.builder().cvFileId(cvFileId).build();
        }
    }

    private ApplicationAiResult parseMatchResponse(String aiResponse, Application application) {
        try {
            String json = cleanJsonResponse(aiResponse);
            JsonNode node = objectMapper.readTree(json);

            int scoreTotal = node.has("scoreTotal") ? node.get("scoreTotal").asInt() : 70;
            int skillMatch = 0;
            int expMatch = 0;

            if (node.has("scoreBreakdown")) {
                JsonNode breakdown = node.get("scoreBreakdown");
                skillMatch = breakdown.has("skills_match") ? breakdown.get("skills_match").asInt() : scoreTotal;
                expMatch = breakdown.has("experience_match") ? breakdown.get("experience_match").asInt() : scoreTotal;
            }

            List<String> strengths = jsonArrayToList(node, "strengths");
            List<String> gaps = jsonArrayToList(node, "gaps");
            String explanation = getTextOrEmpty(node, "explanation");

            return ApplicationAiResult.builder()
                    .application(application)
                    .matchScore(scoreTotal)
                    .skillMatch(skillMatch)
                    .experienceMatch(expMatch)
                    .summary(explanation)
                    .strengths(strengths)
                    .gaps(gaps)
                    .build();
        } catch (Exception e) {
            log.error("❌ Failed to parse Match AI response: {}", e.getMessage());
            // Fallback to basic result
            return ApplicationAiResult.builder()
                    .application(application)
                    .matchScore(0)
                    .skillMatch(0)
                    .experienceMatch(0)
                    .summary("AI analysis could not be completed. Please try again.")
                    .strengths(List.of())
                    .gaps(List.of("AI parsing failed"))
                    .build();
        }
    }

    private AiCvReview parseReviewResponse(String aiResponse, CvFile cvFile) {
        try {
            String json = cleanJsonResponse(aiResponse);
            JsonNode node = objectMapper.readTree(json);

            // New structured fields
            int overallScore = node.has("overallScore") ? node.get("overallScore").asInt() : 50;
            int atsScore = node.has("atsScore") ? node.get("atsScore").asInt() : 50;
            String sectionScores = node.has("sections") ? objectMapper.writeValueAsString(node.get("sections")) : "[]";
            String topIssues = node.has("topIssues") ? objectMapper.writeValueAsString(node.get("topIssues")) : "[]";
            String dataCompleteness = node.has("dataCompleteness") ? objectMapper.writeValueAsString(node.get("dataCompleteness")) : null;

            // Legacy fields for backward compatibility
            String strengths = node.has("strengths") ? objectMapper.writeValueAsString(node.get("strengths")) : "[]";
            String weaknesses = node.has("weaknesses") ? objectMapper.writeValueAsString(node.get("weaknesses")) : "[]";
            String summary = getTextOrEmpty(node, "summary");

            // Convert overallScore (0-100) to overallRating (0-10) for legacy field
            double overallRating = overallScore / 10.0;

            // Build legacy issues from topIssues for backward compatibility
            String issues = topIssues;

            // Build legacy suggestions from sections items where action != KEEP
            String suggestions = "[]";
            try {
                List<java.util.Map<String, String>> suggestionList = new ArrayList<>();
                if (node.has("sections") && node.get("sections").isArray()) {
                    for (JsonNode section : node.get("sections")) {
                        String sectionName = getTextOrEmpty(section, "name");
                        if (section.has("items") && section.get("items").isArray()) {
                            for (JsonNode item : section.get("items")) {
                                String action = getTextOrEmpty(item, "action");
                                if (!"KEEP".equals(action) && !action.isEmpty()) {
                                    java.util.Map<String, String> sug = new java.util.HashMap<>();
                                    sug.put("section", sectionName);
                                    sug.put("suggestion", getTextOrEmpty(item, "reason"));
                                    suggestionList.add(sug);
                                }
                            }
                        }
                    }
                }
                suggestions = objectMapper.writeValueAsString(suggestionList);
            } catch (Exception e) {
                log.warn("Could not build legacy suggestions: {}", e.getMessage());
            }

            return AiCvReview.builder()
                    .cvFile(cvFile)
                    .overallScore(overallScore)
                    .atsScore(atsScore)
                    .sectionScores(sectionScores)
                    .topIssues(topIssues)
                    .dataCompleteness(dataCompleteness)
                    .summary(summary)
                    .issues(issues)
                    .suggestions(suggestions)
                    .strengths(strengths)
                    .weaknesses(weaknesses)
                    .overallRating(BigDecimal.valueOf(overallRating))
                    .build();
        } catch (Exception e) {
            log.error("❌ Failed to parse Review AI response: {}", e.getMessage());
            log.debug("Raw AI response: {}", aiResponse);
            return AiCvReview.builder()
                    .cvFile(cvFile)
                    .overallScore(0)
                    .atsScore(0)
                    .sectionScores("[]")
                    .topIssues("[]")
                    .summary("Failed to analyze CV.")
                    .issues("[]")
                    .suggestions("[]")
                    .strengths("[]")
                    .weaknesses("[]")
                    .overallRating(BigDecimal.ZERO)
                    .build();
        }
    }

    private String getTextOrEmpty(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : "";
    }

    private List<String> jsonArrayToList(JsonNode node, String field) {
        List<String> result = new ArrayList<>();
        if (node.has(field) && node.get(field).isArray()) {
            for (JsonNode item : node.get(field)) {
                result.add(item.asText());
            }
        }
        return result;
    }
}
