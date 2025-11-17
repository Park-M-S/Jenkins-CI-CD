package com.hospital.controller;

import com.hospital.dto.ChatbotResponse;
import com.hospital.service.ChatbotService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 챗봇 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/chatbot")
@CrossOrigin(origins = "*") // CORS 설정 (프론트엔드 연동 시)
public class ChatbotController {

    private final ChatbotService chatbotService;
    
    public ChatbotController (ChatbotService chatbotService) {
    	this.chatbotService = chatbotService;
    }

    /**
     * 챗봇 메시지 전송
     * POST /api/chatbot/chat
     */
    @PostMapping(value = "/chat", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatbotResponse> chat(@RequestBody ChatRequest request) {
        log.info("📨 챗봇 요청 수신: {}", request.getMessage());

        try {
            ChatbotResponse response = chatbotService.chat(request.getMessage());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ 챗봇 처리 중 오류", e);

            ChatbotResponse errorResponse = ChatbotResponse.builder()
                    .type("error")
                    .message("죄송합니다. 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
                    .timestamp(java.time.Instant.now().toString())
                    .build();

            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * 대화 히스토리 포함 챗봇 메시지 전송
     * POST /api/chatbot/chat-with-history
     */
    @PostMapping(value = "/chat-with-history", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatbotResponse> chatWithHistory(@RequestBody ChatWithHistoryRequest request) {
        log.info("📨 챗봇 요청 수신 (히스토리 포함): {}", request.getMessage());

        try {
            ChatbotResponse response = chatbotService.chatWithHistory(
                    request.getMessage(),
                    request.getHistory()
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ 챗봇 처리 중 오류 (히스토리 포함)", e);

            ChatbotResponse errorResponse = ChatbotResponse.builder()
                    .type("error")
                    .message("죄송합니다. 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
                    .timestamp(java.time.Instant.now().toString())
                    .build();

            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * 헬스체크
     * GET /api/chatbot/health
     */
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Chatbot API is running!");
    }

    // ===== 요청 DTO =====

    /**
     * 단순 챗봇 요청
     */
    @lombok.Data
    public static class ChatRequest {
        private String message;
    }

    /**
     * 히스토리 포함 챗봇 요청
     */
    @lombok.Data
    public static class ChatWithHistoryRequest {
        private String message;
        private String history;
    }
}