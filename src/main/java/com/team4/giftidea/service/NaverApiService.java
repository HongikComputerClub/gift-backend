package com.team4.giftidea.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class NaverApiService {

	private final WebClient webClient;

	public NaverApiService(
		@Value("${naver.api.base-url}") String baseUrl,
		@Value("${naver.api.client-id}") String clientId,
		@Value("${naver.api.client-secret}") String clientSecret) {
		this.webClient = WebClient.builder()
			.baseUrl(baseUrl)
			.defaultHeader("X-Naver-Client-Id", clientId)
			.defaultHeader("X-Naver-Client-Secret", clientSecret)
			.build();
	}

	public String searchItems(String query) {
		try {
			if (query == null || query.isBlank()) {
				throw new IllegalArgumentException("Query parameter must not be null or blank.");
			}

			return webClient.get()
				.uri(uriBuilder -> uriBuilder
					.path("/v1/search/shop.json") // 정확한 경로 설정
					.queryParam("query", query)
					.build())
				.retrieve()
				.bodyToMono(String.class)
				.block();
		} catch (WebClientResponseException e) {
			// API 호출 중 에러 처리
			return String.format("Error: %s, Response Body: %s", e.getMessage(), e.getResponseBodyAsString());
		} catch (Exception e) {
			// 기타 예외 처리
			return String.format("An unexpected error occurred: %s", e.getMessage());
		}
	}
}