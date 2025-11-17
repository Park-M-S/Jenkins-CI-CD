package com.hospital.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.io.IOException;
import java.util.Collections;

/**
 * 웹 MVC 설정
 * - JSP ViewResolver 설정
 * - RestTemplate Bean 설정 및 헤더 인터셉터 추가
 * - CORS 설정 (필요시)
 * - 정적 리소스 설정 (필요시)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	/**
	 * JSP ViewResolver 설정
	 */
	@Bean
	public InternalResourceViewResolver viewResolver() {
		InternalResourceViewResolver resolver = new InternalResourceViewResolver();
		resolver.setPrefix("/WEB-INF/views/");
		resolver.setSuffix(".jsp");
		resolver.setContentType("text/html; charset=UTF-8");
		System.out.println("✅ JSP ViewResolver 설정 완료");
		return resolver;
	}

	/**
	 * RestTemplate Bean 설정
	 * 외부 API 호출 시 HTTP 헤더를 자동으로 설정하는 인터셉터를 추가합니다.
	 */
	@Bean
	public RestTemplate restTemplate() {
		RestTemplate restTemplate = new RestTemplate();

		// HTTP 헤더 인터셉터 추가
		restTemplate.setInterceptors(Collections.singletonList(new ClientHttpRequestInterceptor() {
			@Override
			public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
					throws IOException {

				// User-Agent 헤더가 없는 경우, 기본값 설정
				if (!request.getHeaders().containsKey(HttpHeaders.USER_AGENT)) {
					request.getHeaders().set(HttpHeaders.USER_AGENT,
							"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
				}

				// Accept 헤더가 없는 경우, XML 형식으로 기본값 설정
				if (!request.getHeaders().containsKey(HttpHeaders.ACCEPT)) {
					request.getHeaders().setAccept(Collections.singletonList(MediaType.APPLICATION_XML));
				}

				return execution.execute(request, body);
			}
		}));
		System.out.println("✅ RestTemplate Bean 및 헤더 인터셉터 설정 완료");
		return restTemplate;
	}
}
