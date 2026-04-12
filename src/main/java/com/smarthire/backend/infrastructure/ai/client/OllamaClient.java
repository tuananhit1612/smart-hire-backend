package com.smarthire.backend.infrastructure.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class OllamaClient {

    private final String ollamaUrl;
    private final String model;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OllamaClient(
            @Value("${ai.ollama.url:http://localhost:11434}") String ollamaUrl,
            @Value("${ai.ollama.model:llama3}") String model) {
        this.ollamaUrl = ollamaUrl;
        this.model = model;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public String chat(String prompt) {
        log.info("📤 Ollama request — model={}, prompt length={}", model, prompt.length());
        String generateUrl = ollamaUrl + "/api/generate";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("prompt", prompt);
            body.put("stream", false);
            body.put("format", "json");

            // Use low temperature for accurate JSON structure and structured data extraction
            Map<String, Object> options = new HashMap<>();
            options.put("temperature", 0.1);
            body.put("options", options);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(generateUrl, request, String.class);

            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                JsonNode rootNode = objectMapper.readTree(responseEntity.getBody());
                String response = rootNode.path("response").asText();
                log.info("📥 Ollama response — length={}", response != null ? response.length() : 0);
                return response;
            } else {
                log.error("❌ Ollama request failed with status: {}", responseEntity.getStatusCode());
                throw new RuntimeException("Ollama API failed with status " + responseEntity.getStatusCode());
            }

        } catch (Exception e) {
            log.error("❌ Ollama request failed: {}", e.getMessage(), e);
            throw new RuntimeException("Ollama AI request failed: " + e.getMessage(), e);
        }
    }
}
