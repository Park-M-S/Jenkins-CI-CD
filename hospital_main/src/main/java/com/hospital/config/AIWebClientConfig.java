package com.hospital.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Gemini API 통신을 위한 WebClient 설정
 */
@Configuration
public class AIWebClientConfig {

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Bean
    public WebClient AIWebClient() {
        // 커넥션 풀 설정
        ConnectionProvider connectionProvider = ConnectionProvider.builder("gemini-pool")
                .maxConnections(50)  // 최대 연결 수
                .maxIdleTime(Duration.ofSeconds(20))  // 유휴 연결 유지 시간
                .maxLifeTime(Duration.ofSeconds(60))  // 연결 최대 생존 시간
                .pendingAcquireTimeout(Duration.ofSeconds(60))  // 연결 대기 시간
                .evictInBackground(Duration.ofSeconds(120))  // 백그라운드 정리 주기
                .build();

        // HTTP 클라이언트 설정
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)  // 연결 타임아웃 5초
                .responseTimeout(Duration.ofSeconds(60))  // 응답 타임아웃 60초
                .doOnConnected(conn -> 
                    conn.addHandlerLast(new ReadTimeoutHandler(60, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(60, TimeUnit.SECONDS))
                );

        // 메모리 버퍼 크기 설정 (Gemini 응답이 클 수 있으므로 증가)
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();

        return WebClient.builder()
                .baseUrl(geminiApiUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(exchangeStrategies)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "Hospital-Chatbot/1.0")
                .build();
    }
}
