package com.smarthire.backend.infrastructure.ai.config;

import com.google.genai.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gemini AI Configuration.
 * Tạo Client bean từ API key trong .env.
 */
@Configuration
public class GeminiConfig {

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    @Bean
    public Client googleGenAiClient() {
        return Client.builder()
                .apiKey(apiKey)
                .build();
    }

    @Bean
    public String geminiModel() {
        return model;
    }
}
