package com.hospital.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIResponse {

    @JsonProperty("candidates")
    private List<Candidate> candidates;

    @JsonProperty("usageMetadata")
    private UsageMetadata usageMetadata;

    /**
     * 응답 텍스트 추출
     */
    public String getContent() {
        if (candidates != null && !candidates.isEmpty()) {
            Candidate candidate = candidates.get(0);
            if (candidate.getContent() != null 
                && candidate.getContent().getParts() != null 
                && !candidate.getContent().getParts().isEmpty()) {
                return candidate.getContent().getParts().get(0).getText();
            }
        }
        return null;
    }

    /**
     * 응답이 있는지 확인
     */
    public boolean hasContent() {
        return getContent() != null && !getContent().isEmpty();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Candidate {
        @JsonProperty("content")
        private Content content;

        @JsonProperty("finishReason")
        private String finishReason;

        @JsonProperty("index")
        private Integer index;

        @JsonProperty("safetyRatings")
        private List<SafetyRating> safetyRatings;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        @JsonProperty("parts")
        private List<Part> parts;

        @JsonProperty("role")
        private String role;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        @JsonProperty("text")
        private String text;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SafetyRating {
        @JsonProperty("category")
        private String category;

        @JsonProperty("probability")
        private String probability;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsageMetadata {
        @JsonProperty("promptTokenCount")
        private Integer promptTokenCount;

        @JsonProperty("candidatesTokenCount")
        private Integer candidatesTokenCount;

        @JsonProperty("totalTokenCount")
        private Integer totalTokenCount;
    }
}