package com.team4.giftidea.service;

import com.team4.giftidea.dto.ItemDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

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

	public List<ItemDTO> searchItems(List<String> queries) {
		List<ItemDTO> itemList = new ArrayList<>();

		for (String query : queries) {
			try {
				// API 호출 후 응답을 JSON 형식으로 받음
				String response = webClient.get()
					.uri(uriBuilder -> uriBuilder
						.path("/v1/search/shop.json")
						.queryParam("query", query)
						.build())
					.retrieve()
					.bodyToMono(String.class)
					.block();

				// JSON 응답을 처리
				ObjectMapper objectMapper = new ObjectMapper();
				JsonNode rootNode = objectMapper.readTree(response);
				JsonNode itemsNode = rootNode.path("items");

				itemsNode.forEach(item -> {
					ItemDTO itemDTO = new ItemDTO();
					itemDTO.setTitle(item.path("title").asText());
					itemDTO.setLink(item.path("link").asText());
					itemDTO.setLprice(item.path("lprice").asText());
					itemDTO.setImage(item.path("image").asText());
					itemDTO.setMallName(item.path("mallName").asText());
					itemDTO.setProductId(item.path("productId").asText());
					itemDTO.setProductType(item.path("productType").asText());
					itemDTO.setBrand(item.path("brand").asText());
					itemDTO.setMaker(item.path("maker").asText());
					itemDTO.setCategory1(item.path("category1").asText());
					itemDTO.setCategory2(item.path("category2").asText());
					itemDTO.setCategory3(item.path("category3").asText());
					itemDTO.setCategory4(item.path("category4").asText());
					itemList.add(itemDTO);
				});
			} catch (WebClientResponseException e) {
				// API 호출 중 에러 처리
				throw new RuntimeException("Error fetching data from Naver API: " + e.getMessage());
			} catch (Exception e) {
				// 기타 예외 처리
				throw new RuntimeException("An unexpected error occurred: " + e.getMessage());
			}
		}

		return itemList;
	}
}