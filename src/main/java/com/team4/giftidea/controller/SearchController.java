package com.team4.giftidea.controller;

import com.team4.giftidea.dto.ItemDTO;
import com.team4.giftidea.service.KreamApiService;
import com.team4.giftidea.service.NaverApiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
public class SearchController {
	private final KreamApiService kreamApiService;
	private final NaverApiService naverApiService;
	private final ExecutorService executorService = Executors.newFixedThreadPool(2);

	public SearchController(KreamApiService kreamApiService, NaverApiService naverApiService) {
		this.kreamApiService = kreamApiService;
		this.naverApiService = naverApiService;
	}

	@GetMapping
	public List<ItemDTO> search(@RequestParam("query") String query) {
		CompletableFuture<List<ItemDTO>> naverFuture = CompletableFuture.supplyAsync(() ->
			naverApiService.searchItems(List.of(query)), executorService);

		CompletableFuture<List<ItemDTO>> kreamFuture = CompletableFuture.supplyAsync(() ->
			kreamApiService.searchItems(query), executorService);

		CompletableFuture.allOf(naverFuture, kreamFuture).join();

		List<ItemDTO> combinedResults = new ArrayList<>();
		try {
			combinedResults.addAll(naverFuture.get());
			combinedResults.addAll(kreamFuture.get());
		} catch (Exception e) {
			throw new RuntimeException("Error merging search results: " + e.getMessage());
		}

		return combinedResults.stream()
			.sorted((a, b) -> b.getWeight() - a.getWeight())
			.collect(Collectors.toList());
	}
}