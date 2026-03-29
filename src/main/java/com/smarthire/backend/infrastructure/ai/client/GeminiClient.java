package com.smarthire.backend.infrastructure.ai.client;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.google.genai.types.UploadFileConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

/**
 * Low-level Gemini API client.
 * Cung cấp 2 method chính:
 * - chat(prompt)       → text-only generation
 * - chatWithFile(...)  → upload file (PDF) + prompt
 */
@Service
@Slf4j
public class GeminiClient {

    private final Client client;
    private final String model;

    public GeminiClient(Client googleGenAiClient, @Qualifier("geminiModel") String geminiModel) {
        this.client = googleGenAiClient;
        this.model = geminiModel;
    }

    /**
     * Text-only chat — gửi prompt, nhận response text.
     */
    public String chat(String prompt) {
        log.info("📤 Gemini text request — model={}, prompt length={}", model, prompt.length());
        try {
            GenerateContentResponse response = client.models.generateContent(model, prompt, null);
            String result = response.text();
            log.info("📥 Gemini response — length={}", result != null ? result.length() : 0);
            return result;
        } catch (Exception e) {
            log.error("❌ Gemini text request failed: {}", e.getMessage(), e);
            throw new RuntimeException("Gemini AI request failed: " + e.getMessage(), e);
        }
    }

    /**
     * File + prompt — upload file (PDF/DOCX) rồi kết hợp với prompt.
     * Sử dụng Gemini Files API để upload, sau đó tham chiếu trong request.
     */
    public String chatWithFile(Path filePath, String mimeType, String prompt) {
        log.info("📤 Gemini file request — model={}, file={}, prompt length={}",
                model, filePath.getFileName(), prompt.length());
        try {
            // 1. Upload file lên Gemini
            com.google.genai.types.File uploadedFile = client.files.upload(
                    filePath.toString(),
                    UploadFileConfig.builder()
                            .mimeType(mimeType)
                            .displayName(filePath.getFileName().toString())
                            .build()
            );
            String fileUri = uploadedFile.uri().orElseThrow(() -> 
                    new RuntimeException("File upload succeeded but no URI returned"));
            log.info("📁 File uploaded — uri={}", fileUri);

            // 2. Tạo request với file reference + text prompt
            Content content = Content.builder()
                    .role("user")
                    .parts(List.of(
                            Part.builder()
                                    .fileData(com.google.genai.types.FileData.builder()
                                            .fileUri(fileUri)
                                            .mimeType(mimeType)
                                            .build())
                                    .build(),
                            Part.builder()
                                    .text(prompt)
                                    .build()
                    ))
                    .build();

            GenerateContentResponse response = client.models.generateContent(
                    model,
                    List.of(content),
                    null
            );

            String result = response.text();
            log.info("📥 Gemini file response — length={}", result != null ? result.length() : 0);
            return result;
        } catch (Exception e) {
            log.error("❌ Gemini file request failed: {}", e.getMessage(), e);
            throw new RuntimeException("Gemini AI file request failed: " + e.getMessage(), e);
        }
    }
}
