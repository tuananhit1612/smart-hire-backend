package com.smarthire.backend.features.interview.controller;

import com.smarthire.backend.features.interview.dto.CreateInterviewRequest;
import com.smarthire.backend.features.interview.dto.InterviewResponse;
import com.smarthire.backend.features.interview.dto.UpdateInterviewRequest;
import com.smarthire.backend.features.interview.service.InterviewService;
import com.smarthire.backend.infrastructure.ai.service.AiService;
import com.smarthire.backend.features.application.repository.ApplicationRepository;
import com.smarthire.backend.shared.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;
    private final AiService aiService;
    private final ApplicationRepository applicationRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<InterviewResponse>> createInterview(
            @Valid @RequestBody CreateInterviewRequest request) {
        InterviewResponse response = interviewService.createInterview(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Interview scheduled successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InterviewResponse>> getInterviewById(@PathVariable Long id) {
        InterviewResponse response = interviewService.getInterviewById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/application/{applicationId}")
    public ResponseEntity<ApiResponse<List<InterviewResponse>>> getInterviewsByApplication(
            @PathVariable Long applicationId) {
        List<InterviewResponse> responses = interviewService.getInterviewsByApplication(applicationId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<InterviewResponse>>> getMyInterviews() {
        List<InterviewResponse> responses = interviewService.getMyInterviews();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InterviewResponse>> updateInterview(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInterviewRequest request) {
        InterviewResponse response = interviewService.updateInterview(id, request);
        return ResponseEntity.ok(ApiResponse.success("Interview updated successfully", response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<InterviewResponse>> changeStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        InterviewResponse response = interviewService.changeStatus(id, body.get("status"));
        return ResponseEntity.ok(ApiResponse.success("Interview status updated", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInterview(@PathVariable Long id) {
        interviewService.deleteInterview(id);
        return ResponseEntity.ok(ApiResponse.success("Interview deleted successfully", null));
    }

    // =========================================================================
    // M3.4 & M3.5 AI Endpoints
    // =========================================================================

    @GetMapping("/application/{applicationId}/generate-questions")
    public ResponseEntity<ApiResponse<Object>> generateQuestions(@PathVariable Long applicationId) {
        var application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        
        String jsonResult = aiService.generateInterviewQuestions(application);
        try {
            ObjectMapper mapper = new ObjectMapper();
            return ResponseEntity.ok(ApiResponse.success("Generated questions", mapper.readTree(jsonResult)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.success("Generated questions", jsonResult));
        }
    }

    @PostMapping("/evaluate-answer")
    public ResponseEntity<ApiResponse<Object>> evaluateAnswer(@RequestBody Map<String, String> body) {
        String jobTitle = body.getOrDefault("jobTitle", "Unknown Job");
        String question = body.get("question");
        String answer = body.get("answer");

        String jsonResult = aiService.evaluateVirtualInterviewAnswer(jobTitle, question, answer);
        try {
            ObjectMapper mapper = new ObjectMapper();
            return ResponseEntity.ok(ApiResponse.success("Evaluated answer", mapper.readTree(jsonResult)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.success("Evaluated answer", jsonResult));
        }
    }
}
