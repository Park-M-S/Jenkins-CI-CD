package com.hospital.caller;

import com.hospital.dto.AIRequest;
import com.hospital.dto.AIResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Gemini API 호출 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIApiCaller {

    private final WebClient AIWebClient;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model}")
    private String model;

    /**
     * Gemini API 호출 (동기 방식)
     */
    public String generateContent(String userMessage) {
        log.info("📤 Gemini API 호출 시작 - 모델: {}", model);
        
        try {
            AIRequest request = AIRequest.builder()
                    .userMessage(userMessage)
                    .build();

            AIResponse response = AIWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model))
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AIResponse.class)
                    .block();

            if (response != null && response.hasContent()) {
                String content = response.getContent();
                log.info("✅ Gemini 응답 성공 - 길이: {} 자", content.length());
                return content;
            }

            log.warn("⚠️ Gemini 응답이 비어있습니다.");
            return "응답을 생성할 수 없습니다.";

        } catch (Exception e) {
            log.error("❌ Gemini API 호출 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Gemini API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * Gemini API 호출 (비동기 방식)
     */
    public Mono<String> generateContentAsync(String userMessage) {
        log.info("📤 Gemini API 비동기 호출 시작 - 모델: {}", model);

        AIRequest request = AIRequest.builder()
                .userMessage(userMessage)
                .build();

        return AIWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/{model}:generateContent")
                        .queryParam("key", apiKey)
                        .build(model))
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AIResponse.class)
                .map(response -> {
                    if (response != null && response.hasContent()) {
                        log.info("✅ Gemini 비동기 응답 성공");
                        return response.getContent();
                    }
                    return "응답을 생성할 수 없습니다.";
                })
                .doOnError(e -> log.error("❌ Gemini API 비동기 호출 실패: {}", e.getMessage()))
                .onErrorReturn("죄송합니다. 응답 생성 중 오류가 발생했습니다.");
    }

    /**
     * 대화 히스토리를 포함한 API 호출
     */
    public String generateContentWithHistory(String userMessage, String conversationHistory) {
        log.info("📤 Gemini API 호출 (히스토리 포함) - 모델: {}", model);

        try {
            String combinedMessage = conversationHistory + "\n\n사용자: " + userMessage;
            
            AIRequest request = AIRequest.builder()
                    .userMessage(combinedMessage)
                    .build();

            AIResponse response = AIWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model))
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AIResponse.class)
                    .block();

            if (response != null && response.hasContent()) {
                log.info("✅ Gemini 응답 성공 (히스토리 포함)");
                return response.getContent();
            }

            return "응답을 생성할 수 없습니다.";

        } catch (Exception e) {
            log.error("❌ Gemini API 호출 실패 (히스토리 포함): {}", e.getMessage(), e);
            throw new RuntimeException("Gemini API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}