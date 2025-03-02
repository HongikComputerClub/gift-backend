package com.team4.giftidea.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team4.giftidea.entity.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;

/**
 * 네이버 쇼핑 API를 호출하여 상품 정보를 가져오는 서비스 클래스(더 이상 사용하지 않음)
 */
@Service
@Slf4j
public class NaverApiService {

	private final WebClient webClient;
	private final ObjectMapper objectMapper;

	/**
	 * NaverApiService 생성자
	 * WebClient를 설정하여 Naver API를 호출할 준비를 합니다.
	 *
	 * @param baseUrl     네이버 API 기본 URL
	 * @param clientId    네이버 API 클라이언트 ID
	 * @param clientSecret 네이버 API 클라이언트 Secret
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

		this.objectMapper = new ObjectMapper();
	}

	/**
	 * 주어진 키워드 목록에 대해 네이버 API를 호출하여 상품 정보를 검색하고, 결과를 리스트로 반환합니다.
	 *
	 * @param queries 검색할 키워드 목록
	 * @return 검색된 상품 리스트
	 */
	public List<Product> searchItems(List<String> queries) {
		List<Product> productList = new ArrayList<>();

		for (String query : queries) {
			try {
				String response = fetchProductsFromNaver(query);
				List<Product> products = parseResponse(response, query);
				productList.addAll(products);

			} catch (WebClientResponseException e) {
				log.error("네이버 API 호출 오류: {}", e.getMessage());
			} catch (Exception e) {
				log.error("예기치 못한 오류 발생: {}", e.getMessage());
			}
		}
		return productList;
	}

	/**
	 * 네이버 쇼핑 API를 호출하여 검색 결과를 가져옵니다.
	 *
	 * @param query 검색어
	 * @return API 응답 JSON 문자열
	 */
	private String fetchProductsFromNaver(String query) {
		return webClient.get()
			.uri(uriBuilder -> uriBuilder
				.path("/v1/search/shop.json")
				.queryParam("query", query)
				.queryParam("display", 100)  // 최대 100개 검색
				.build())
			.retrieve()
			.bodyToMono(String.class)
			.block();
	}

	/**
	 * API 응답을 파싱하여 Product 리스트로 변환합니다.
	 *
	 * @param response JSON 형식의 API 응답
	 * @param query    검색 키워드
	 * @return 변환된 Product 리스트
	 */
	private List<Product> parseResponse(String response, String query) throws Exception {
		List<Product> products = new ArrayList<>();
		JsonNode rootNode = objectMapper.readTree(response);
		JsonNode itemsNode = rootNode.path("items");

		itemsNode.forEach(item -> {
			Product product = new Product();
			product.setTitle(item.path("title").asText());
			product.setLink(item.path("link").asText());
			product.setPrice(item.path("lprice").asInt());
			product.setImage(item.path("image").asText());
			product.setMallName(item.path("mallName").asText());
			product.setProductId(item.path("productId").asText());
			product.setBrand(item.path("brand").asText());
			product.setCategory(item.path("category1").asText());
			product.setKeyword(query);
			products.add(product);
		});

		return products;
	}
}