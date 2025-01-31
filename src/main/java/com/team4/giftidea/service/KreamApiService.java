//package com.team4.giftidea.service;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.team4.giftidea.dto.ItemDTO;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.web.reactive.function.client.WebClient;
//import org.springframework.web.reactive.function.client.WebClientResponseException;
//
//import java.util.ArrayList;
//import java.util.List;
//
//@Service
//public class KreamApiService {
//	private final WebClient webClient;
//
//	public KreamApiService(
//		@Value("https://api.kream.co.kr") String baseUrl,
//		@Value("${kream.api.session-cookie}") String sessionCookie) {
//		this.webClient = WebClient.builder()
//			.baseUrl(baseUrl)
//			.defaultHeader("Cookie", sessionCookie)  // 쿠키 추가
//			.defaultHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)")
//			.defaultHeader("Referer", "https://kream.co.kr")
//			.build();
//	}
//
//	public List<ItemDTO> searchItems(String keyword) {
//		List<ItemDTO> itemList = new ArrayList<>();
//		try {
//			// KREAM API 호출
//			String response = webClient.get()
//				.uri(uriBuilder -> uriBuilder
//					.path("/api/se/suggest")
//					.queryParam("per_page", 10)
//					.queryParam("keyword", keyword)
//					.build())
//				.retrieve()
//				.bodyToMono(String.class)
//				.block();
//
//			// JSON 응답 처리
//			ObjectMapper objectMapper = new ObjectMapper();
//			JsonNode rootNode = objectMapper.readTree(response);
//			JsonNode productsNode = rootNode.path("products");
//
//			productsNode.forEach(product -> {
//				ItemDTO item = new ItemDTO();
//				item.setTitle(product.path("name").asText());
//				item.setLink("https://kream.co.kr/products/" + product.path("id").asText());
//				item.setPrice(product.path("price").asInt());
//				item.setImage(product.path("image_url").asText());
//				item.setMallName("KREAM");
//				item.setProductId(product.path("id").asText());
//				item.setBrand(product.path("brand").asText());
//				item.setCategory(product.path("category").asText());
//				itemList.add(item);
//			});
//		} catch (WebClientResponseException e) {
//			throw new RuntimeException("Error fetching data from KREAM API: " + e.getMessage());
//		} catch (Exception e) {
//			throw new RuntimeException("An unexpected error occurred: " + e.getMessage());
//		}
//		return itemList;
//	}
//
//}