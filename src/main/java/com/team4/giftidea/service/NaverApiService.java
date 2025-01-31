package com.team4.giftidea.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team4.giftidea.entity.Product;

import java.util.ArrayList;
import java.util.List;

@Service
public class NaverApiService {

	private final WebClient webClient;

	/**
	 * NaverApiService 생성자
	 * WebClient를 설정하여 Naver API를 호출할 준비를 합니다.
	 *
	 * @param baseUrl     기본 URL
	 * @param clientId    클라이언트 ID
	 * @param clientSecret 클라이언트 비밀번호
	 */
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

	/**
	 * 주어진 키워드 목록에 대해 네이버 API를 호출하여 상품 정보를 검색하고, 결과를 리스트로 반환합니다.
	 *
	 * @param queries 검색할 키워드 목록
	 * @return 상품 리스트
	 */
	public List<Product> searchItems(List<String> queries) {
		List<Product> productList = new ArrayList<>();

		// 각 키워드에 대해 네이버 API 호출
		for (String query : queries) {
			try {
				// 네이버 쇼핑 API 호출
				String response = webClient.get()
					.uri(uriBuilder -> uriBuilder
						.path("/v1/search/shop.json")
						.queryParam("query", query)
						.queryParam("display", 100)  // 최대 100개 상품 검색
						.build())
					.retrieve()
					.bodyToMono(String.class)
					.block();

				// 응답 데이터를 파싱하여 상품 리스트 생성
				ObjectMapper objectMapper = new ObjectMapper();
				JsonNode rootNode = objectMapper.readTree(response);
				JsonNode itemsNode = rootNode.path("items");

				// 각 상품에 대해 처리
				itemsNode.forEach(item -> {
					Product productEntity = new Product();
					productEntity.setTitle(item.path("title").asText());
					productEntity.setLink(item.path("link").asText());
					productEntity.setPrice(item.path("lprice").asInt());
					productEntity.setImage(item.path("image").asText());
					productEntity.setMallName(item.path("mallName").asText());
					productEntity.setProductId(item.path("productId").asText());
					productEntity.setBrand(item.path("brand").asText());
					productEntity.setCategory(item.path("category1").asText());
					productEntity.setKeyword(query);  // 키워드 설정

					// 상품 리스트에 추가
					productList.add(productEntity);
				});

			} catch (WebClientResponseException e) {
				// API 호출 중 오류 발생 시 처리
				throw new RuntimeException("Error fetching data from Naver API: " + e.getMessage());
			} catch (Exception e) {
				// 일반적인 예외 처리
				throw new RuntimeException("An unexpected error occurred: " + e.getMessage());
			}
		}

		return productList;
	}
}