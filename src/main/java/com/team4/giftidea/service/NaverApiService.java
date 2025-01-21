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
				String response = webClient.get()
					.uri(uriBuilder -> uriBuilder
						.path("/v1/search/shop.json")
						.queryParam("query", query)
						.queryParam("display", 100)
						.build())
					.retrieve()
					.bodyToMono(String.class)
					.block();

				ObjectMapper objectMapper = new ObjectMapper();
				JsonNode rootNode = objectMapper.readTree(response);
				JsonNode itemsNode = rootNode.path("items");

				itemsNode.forEach(item -> {
					ItemDTO itemDTO = new ItemDTO();
					itemDTO.setTitle(item.path("title").asText());
					itemDTO.setLink(item.path("link").asText());
					itemDTO.setPrice(item.path("lprice").asInt());
					itemDTO.setImage(item.path("image").asText());
					itemDTO.setMallName(item.path("mallName").asText());
					itemDTO.setProductId(item.path("productId").asText());
					itemDTO.setBrand(item.path("brand").asText());
					itemDTO.setCategory(item.path("category1").asText());
					itemList.add(itemDTO);
				});
			} catch (WebClientResponseException e) {
				throw new RuntimeException("Error fetching data from Naver API: " + e.getMessage());
			} catch (Exception e) {
				throw new RuntimeException("An unexpected error occurred: " + e.getMessage());
			}
		}

		return itemList;
	}
}