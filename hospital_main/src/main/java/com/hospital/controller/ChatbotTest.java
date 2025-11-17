package com.hospital.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("test")
public class ChatbotTest {
	
	@GetMapping("chatbot")
	public String chatbotTest() {
		return "chatbot-test";
		
		
	}

}
