package com.hospital.config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.hospital.websocket.ChatBotWebSocketHandler;
import com.hospital.websocket.EmergencyApiWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
	private final EmergencyApiWebSocketHandler emergencyApiWebSocketHandler;
	private final ChatBotWebSocketHandler chatBotWebSocketHandler;

	@Autowired
	public WebSocketConfig(EmergencyApiWebSocketHandler emergencyApiWebSocketHandler,
	                       ChatBotWebSocketHandler chatBotWebSocketHandler) {
	    this.emergencyApiWebSocketHandler = emergencyApiWebSocketHandler;
	    this.chatBotWebSocketHandler = chatBotWebSocketHandler;
	}
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(emergencyApiWebSocketHandler, "/emergency-websocket")
                .setAllowedOrigins("*");
        
     // 챗봇 웹소캣
        registry.addHandler(chatBotWebSocketHandler, "/chatbot-websocket")
                .setAllowedOrigins("*");
        
        System.out.println("✅ WebSocket 핸들러 등록 완료!");
    }
}