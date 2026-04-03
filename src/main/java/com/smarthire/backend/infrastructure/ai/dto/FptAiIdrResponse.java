package com.smarthire.backend.infrastructure.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FptAiIdrResponse {
    private int errorCode;
    private String errorMessage;
    private List<FptAiIdrData> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FptAiIdrData {
        private String type;
        private FptAiIdrInfo info;
        private Map<String, Double> probability;
        private String warning;

        // Flat fields support (FPT AI returns these directly in some endpoints)
        private String id;
        private String name;
        private String dob;
        private String sex;
        private String nationality;
        private String home;
        private String address;
        private String doe;

        @com.fasterxml.jackson.annotation.JsonProperty("id_prob")
        private Double idProb;
        @com.fasterxml.jackson.annotation.JsonProperty("name_prob")
        private Double nameProb;
        @com.fasterxml.jackson.annotation.JsonProperty("dob_prob")
        private Double dobProb;

        @com.fasterxml.jackson.annotation.JsonProperty("issue_date")
        private String issueDate;
        @com.fasterxml.jackson.annotation.JsonProperty("issue_loc")
        private String issueLoc;
        private String mrz;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FptAiIdrInfo {
        private String id;
        private String name;
        private String dob;
        private String sex;
        private String nationality;
        private String home;
        private String address;
        private String doe;
        private String type;

        // Back side fields (Snake case support)
        @com.fasterxml.jackson.annotation.JsonProperty("issue_date")
        private String issueDate;
        @com.fasterxml.jackson.annotation.JsonProperty("issue_loc")
        private String issueLoc;
        private String mrz;
    }
}
