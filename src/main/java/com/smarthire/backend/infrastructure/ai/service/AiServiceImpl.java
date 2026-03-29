package com.smarthire.backend.infrastructure.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthire.backend.core.exception.ResourceNotFoundException;
import com.smarthire.backend.features.application.entity.Application;
import com.smarthire.backend.features.application.entity.ApplicationAiResult;
import com.smarthire.backend.features.candidate.entity.AiCvReview;
import com.smarthire.backend.features.candidate.entity.CvFile;
import com.smarthire.backend.features.candidate.repository.CvFileRepository;
import com.smarthire.backend.features.onboarding.dto.VerifiedCvData;
import com.smarthire.backend.infrastructure.ai.client.GeminiClient;
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

    private final GeminiClient geminiClient;
    private final CvFileRepository cvFileRepository;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public VerifiedCvData parseCvFile(Long cvFileId) {
        log.info("🤖 AI Parse CV — cvFileId={}", cvFileId);

        CvFile cvFile = cvFileRepository.findById(cvFileId)
                .orElseThrow(() -> new ResourceNotFoundException("CvFile", cvFileId));

        // Lấy path file thật trên disk
        Path filePath = fileStorageService.getFilePath(cvFile.getFilePath());
        String mimeType = getMimeType(cvFile.getFileName());

        // Gọi Gemini với file upload
        String aiResponse = geminiClient.chatWithFile(filePath, mimeType, PromptTemplates.CV_PARSE_PROMPT);

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

        // Gọi Gemini text chat
        String aiResponse = geminiClient.chat(prompt);

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

        String prompt = String.format(PromptTemplates.CV_REVIEW_PROMPT, rules);

        // Gọi Gemini với file upload
        String aiResponse = geminiClient.chatWithFile(filePath, mimeType, prompt);

        // Parse JSON → AiCvReview entity
        return parseReviewResponse(aiResponse, cvFile);
    }

    // ━━━━━━━━━━━━━━━━━━━━ Private helpers ━━━━━━━━━━━━━━━━━━━━

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
                // Upload file lên Gemini và extract text content
                Path filePath = fileStorageService.getFilePath(cvFile.getFilePath());
                String mimeType = getMimeType(cvFile.getFileName());
                return geminiClient.chatWithFile(filePath, mimeType,
                        "Extract all text content from this CV/Resume document. Return only the text content, no formatting.");
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
                    // Try to validate it using Enum parsing
                    Gender.valueOf(genderStr.toUpperCase());
                } catch (Exception ignored) {
                    genderStr = null; // invalid gender string
                }
            } else {
                genderStr = null;
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
                    .build();
        } catch (Exception e) {
            log.error("❌ Failed to parse CV AI response: {}", e.getMessage());
            log.debug("Raw AI response: {}", aiResponse);
            // Return empty data instead of crashing
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

            double rating = node.has("overallRating") ? node.get("overallRating").asDouble() : 5.0;
            String summary = getTextOrEmpty(node, "summary");

            // Store JSON objects/arrays as JSON strings for the database
            String issues = node.has("issues") ? objectMapper.writeValueAsString(node.get("issues")) : "[]";
            String suggestions = node.has("suggestions") ? objectMapper.writeValueAsString(node.get("suggestions")) : "[]";
            String strengths = node.has("strengths") ? objectMapper.writeValueAsString(node.get("strengths")) : "[]";
            String weaknesses = node.has("weaknesses") ? objectMapper.writeValueAsString(node.get("weaknesses")) : "[]";

            return AiCvReview.builder()
                    .cvFile(cvFile)
                    .summary(summary)
                    .issues(issues)
                    .suggestions(suggestions)
                    .strengths(strengths)
                    .weaknesses(weaknesses)
                    .overallRating(BigDecimal.valueOf(rating))
                    .build();
        } catch (Exception e) {
            log.error("❌ Failed to parse Review AI response: {}", e.getMessage());
            return AiCvReview.builder()
                    .cvFile(cvFile)
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
