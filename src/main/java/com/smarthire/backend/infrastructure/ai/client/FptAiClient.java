package com.smarthire.backend.infrastructure.ai.client;

import com.smarthire.backend.infrastructure.ai.dto.FptAiIdrResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Path;

@Component
@Slf4j
public class FptAiClient {

    private final String apiKey;
    private final String idrUrl;
    private final RestTemplate restTemplate;

    public FptAiClient(
            @Value("${fpt.ai.api-key}") String apiKey,
            @Value("${fpt.ai.idr-url}") String idrUrl) {
        this.apiKey = apiKey;
        this.idrUrl = idrUrl;
        this.restTemplate = new RestTemplate();
    }

    public FptAiIdrResponse recognizeIdCard(Path imagePath) {
        log.info("📤 FPT AI IDR Request — file={}", imagePath.getFileName());
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("api-key", apiKey);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image", new FileSystemResource(imagePath.toFile()));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            FptAiIdrResponse response = restTemplate.postForObject(idrUrl, requestEntity, FptAiIdrResponse.class);
            
            if (response != null) {
                log.info("📥 FPT AI IDR Response — errorCode={}, data size={}", 
                        response.getErrorCode(), 
                        response.getData() != null ? response.getData().size() : 0);
            }
            
            return response;
        } catch (Exception e) {
            log.error("❌ FPT AI IDR Request failed: {}", e.getMessage(), e);
            throw new RuntimeException("FPT AI IDR Request failed: " + e.getMessage(), e);
        }
    }
}
