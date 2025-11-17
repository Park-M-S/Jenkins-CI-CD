package com.hospital.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Gemini API 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIRequest {

    @JsonProperty("contents")
    private List<Content> contents;

    // 편의 생성자
    public static AIRequest of(String userMessage) {
        return AIRequest.builder()
                .contents(List.of(
                        Content.builder()
                                .parts(List.of(Part.builder().text(userMessage).build()))
                                .build()
                ))
                .build();
    }

    // Builder 패턴에서 사용할 편의 메서드
    public static class AIRequestBuilder {
        public AIRequestBuilder userMessage(String message) {
            this.contents = List.of(
                    Content.builder()
                            .parts(List.of(Part.builder().text(message).build()))
                            .build()
            );
            return this;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        @JsonProperty("parts")
        private List<Part> parts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        @JsonProperty("text")
        private String text;
    }
}