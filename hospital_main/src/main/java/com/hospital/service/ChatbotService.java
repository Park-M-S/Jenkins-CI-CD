package com.hospital.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.caller.AIApiCaller;
import com.hospital.dto.ChatbotResponse;
import com.hospital.validator.ChatbotValidator;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * 챗봇 비즈니스 로직 서비스
 */
@Slf4j
@Service
public class ChatbotService {

	private final AIApiCaller aiApiCaller;
	private final ObjectMapper objectMapper;
	private final ChatbotValidator validator;

	@Value("${chatbot.system-prompt-file}")
	private Resource systemPromptFile;

	private String systemPrompt;

	public ChatbotService(AIApiCaller aiApiCaller, ObjectMapper objectMapper, ChatbotValidator validator) {
		this.aiApiCaller = aiApiCaller;
		this.objectMapper = objectMapper;
		this.validator = validator;
	}

	/**
	 * 시스템 프롬프트 로드 (서버 시작 시 1회)
	 */
	@PostConstruct
	public void init() {
		try {
			this.systemPrompt = systemPromptFile.getContentAsString(StandardCharsets.UTF_8);
			log.info("✅ 시스템 프롬프트 로드 완료: {} bytes", systemPrompt.length());
		} catch (IOException e) {
			log.error("❌ 시스템 프롬프트 로드 실패", e);
			throw new RuntimeException("시스템 프롬프트 파일을 읽을 수 없습니다", e);
		}
	}

	/**
	 * 챗봇 메시지 처리 (메인 메서드)
	 */
	public ChatbotResponse chat(String userMessage) {
		log.info("💬 챗봇 요청: {}", userMessage);

		// 입력 검증
		String validationError = validator.validateUserMessage(userMessage);
		if (validationError != null) {
			return createErrorResponse(validationError);
		}

		try {
			// 1. 시스템 프롬프트 + 사용자 메시지 결합
			String fullMessage = buildFullMessage(userMessage);

			// 2. Gemini API 호출
			String aiResponseText = aiApiCaller.generateContent(fullMessage);

			if (aiResponseText == null || aiResponseText.isEmpty()) {
				log.warn("⚠️ AI 응답이 비어있습니다");
				return createErrorResponse("응답을 생성할 수 없습니다.");
			}

			// 3. JSON 파싱 및 검증
			ChatbotResponse chatbotResponse = parseAndValidate(aiResponseText);

			// 4. 타임스탬프 설정
			chatbotResponse.setTimestamp(java.time.Instant.now().toString());

			log.info("✅ 챗봇 응답 완료: type={}", chatbotResponse.getType());
			return chatbotResponse;

		} catch (Exception e) {
			log.error("❌ 챗봇 처리 중 오류 발생", e);
			return createErrorResponse("일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
		}
	}

	/**
	 * 대화 히스토리 포함 채팅
	 */
	public ChatbotResponse chatWithHistory(String userMessage, String conversationHistory) {
		log.info("💬 챗봇 요청 (히스토리 포함): {}", userMessage);

		// 입력 검증
		String validationError = validator.validateUserMessage(userMessage);
		if (validationError != null) {
			return createErrorResponse(validationError);
		}

		try {
			String fullMessage = buildFullMessageWithHistory(userMessage, conversationHistory);
			String aiResponseText = aiApiCaller.generateContent(fullMessage);

			if (aiResponseText == null || aiResponseText.isEmpty()) {
				return createErrorResponse("응답을 생성할 수 없습니다.");
			}

			ChatbotResponse chatbotResponse = parseAndValidate(aiResponseText);

			// 타임스탬프 설정
			chatbotResponse.setTimestamp(java.time.Instant.now().toString());

			log.info("✅ 챗봇 응답 완료 (히스토리 포함): type={}", chatbotResponse.getType());
			return chatbotResponse;

		} catch (Exception e) {
			log.error("❌ 챗봇 처리 중 오류 발생 (히스토리 포함)", e);
			return createErrorResponse("일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
		}
	}

	/**
	 * AI 응답 파싱 및 검증
	 */
	private ChatbotResponse parseAndValidate(String responseText) {
		try {
			String jsonText = extractJson(responseText);
			log.info("📝 파싱할 JSON: {}", jsonText);
			
			ChatbotResponse response = objectMapper.readValue(jsonText, ChatbotResponse.class);
			
			// 검증
			String validationError = validator.validateResponse(response);
			if (validationError != null) {
				return createErrorResponse(validationError);
			}
			
			return response;

		} catch (JsonProcessingException e) {
			log.error("❌ JSON 파싱 실패: {}", responseText, e);
			return createErrorResponse("응답 처리 중 오류가 발생했습니다.");
		}
	}

	private String buildFullMessage(String userMessage) {
		return systemPrompt + "\n\n===사용자 메시지===\n" + userMessage;
	}

	private String buildFullMessageWithHistory(String userMessage, String history) {
		if (history == null || history.trim().isEmpty()) {
			// 히스토리가 없으면 일반 메시지와 동일하게 처리
			return buildFullMessage(userMessage);
		}
		return systemPrompt + "\n\n===이전 대화===\n" + history + "\n\n===사용자 메시지===\n" + userMessage;
	}

	private String extractJson(String text) {
	    if (text == null) {
	        return "";
	    }
	    
	    text = text.trim();
	    
	    // 1. 마크다운 코드 블록 제거
	    if (text.startsWith("```json")) {
	        text = text.substring(7).trim();
	    } else if (text.startsWith("```")) {
	        text = text.substring(3).trim();
	    }
	    
	    // 2. "json" 단어 제거
	    if (text.toLowerCase().startsWith("json")) {
	        text = text.substring(4).trim();
	    }
	    
	    // 3. 끝의 ``` 제거
	    if (text.endsWith("```")) {
	        text = text.substring(0, text.length() - 3).trim();
	    }
	    
	    // 4. 중복 따옴표 제거 (Gemini 버그 대응)
	    text = text.replaceAll("\"\"(\\w+)\"\\s*:", "\"$1\":");
	    
	    // 5. 혹시 모를 추가 정리
	    text = text.trim();
	    
	    log.debug("🔧 JSON 정제 완료: {}", text);
	    
	    return text;
	}

	private ChatbotResponse createErrorResponse(String errorMessage) {
		return ChatbotResponse.builder()
				.type("error")
				.message(errorMessage)
				.timestamp(java.time.Instant.now().toString())
				.build();
	}
}
