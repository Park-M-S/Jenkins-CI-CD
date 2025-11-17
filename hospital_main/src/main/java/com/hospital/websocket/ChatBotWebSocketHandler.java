package com.hospital.websocket;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.dto.ChatbotResponse;
import com.hospital.service.ChatbotService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatBotWebSocketHandler extends TextWebSocketHandler {

    private static final int MAX_HISTORY_SIZE = 10; // 최근 10개의 대화만 유지

    private final Set<WebSocketSession> sessions = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, LinkedList<String>> conversationHistories = new ConcurrentHashMap<>();
    private final ChatbotService chatbotService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public ChatBotWebSocketHandler(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        conversationHistories.put(session.getId(), new LinkedList<>());
        System.out.println("ChatBot WebSocket 연결됨: " + session.getId() + ", 총 연결수: " + sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        System.out.println("========================================");
        System.out.println("📩 [수신] Raw Payload: " + message.getPayload());

        JsonNode node = objectMapper.readTree(message.getPayload());

        if (node.has("type") && "chat".equals(node.get("type").asText()) && node.has("message")) {
            String userMessage = node.get("message").asText();
            String sessionId = session.getId();

            System.out.println("💬 [사용자 메시지]: \"" + userMessage + "\"");
            System.out.println("🔑 [세션 ID]: " + sessionId);

            // 대화 이력 조회
            String conversationHistory = getConversationHistory(sessionId);
            System.out.println("📜 [대화 히스토리]: " + (conversationHistory.isEmpty() ? "(비어있음)" : "\n" + conversationHistory));

            // 서비스에서 검증 + AI 호출 (이력 포함)
            ChatbotResponse response = chatbotService.chatWithHistory(userMessage, conversationHistory);

            System.out.println("🤖 [AI 응답 타입]: " + response.getType());
            System.out.println("📤 [AI 응답 메시지]: " + response.getMessage());
            System.out.println("⏰ [타임스탬프]: " + response.getTimestamp());

            // 대화 이력에 추가
            addToHistory(sessionId, userMessage, response);

            // 응답 전송
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
            System.out.println("✅ [응답 전송 완료]");
        } else {
            // 알 수 없는 메시지는 서비스에서 처리하도록 로그만 남김
            System.out.println("⚠️ [알 수 없는 메시지 형식]: " + message.getPayload());
        }
        System.out.println("========================================");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        sessions.remove(session);
        conversationHistories.remove(session.getId());
        System.out.println("ChatBot WebSocket 연결 종료: " + session.getId());
    }

    /**
     * 대화 이력 조회
     */
    private String getConversationHistory(String sessionId) {
        LinkedList<String> history = conversationHistories.get(sessionId);
        if (history == null || history.isEmpty()) {
            return "";
        }
        return String.join("\n", history);
    }

    /**
     * 대화 이력에 추가 (최대 개수 제한)
     */
    private void addToHistory(String sessionId, String userMessage, ChatbotResponse response) {
        LinkedList<String> history = conversationHistories.get(sessionId);
        if (history == null) {
            history = new LinkedList<>();
            conversationHistories.put(sessionId, history);
        }

        String timestamp = response.getTimestamp();

        // 사용자 메시지 추가 (타임스탬프 포함)
        history.add("[" + timestamp + "] 사용자: " + userMessage);

        // AI 응답 추가 (타임스탬프 포함)
        history.add("[" + timestamp + "] AI: " + response.getMessage());

        // 최대 개수 초과 시 오래된 대화 삭제 (FIFO)
        while (history.size() > MAX_HISTORY_SIZE * 2) { // 사용자+AI 쌍이므로 *2
            history.removeFirst();
        }
    }
}
