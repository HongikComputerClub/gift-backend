package com.team4.giftidea.controller;

import com.team4.giftidea.dto.ItemDTO;
import com.team4.giftidea.service.KreamApiService;
import com.team4.giftidea.service.NaverApiService;
import com.team4.giftidea.service.CoupangApiService;
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
	private final CoupangApiService coupangApiService;
	private final ExecutorService executorService = Executors.newFixedThreadPool(3);

	public SearchController(KreamApiService kreamApiService, NaverApiService naverApiService, CoupangApiService coupangApiService) {
		this.kreamApiService = kreamApiService;
		this.naverApiService = naverApiService;
		this.coupangApiService = coupangApiService;
	}

	@GetMapping
	public List<ItemDTO> search(@RequestParam("query") String query) {
		CompletableFuture<List<ItemDTO>> coupangFuture = CompletableFuture.supplyAsync(() ->
			coupangApiService.searchItems(query), executorService);

		CompletableFuture.allOf(coupangFuture).join();

		List<ItemDTO> combinedResults = new ArrayList<>();
		try {
			combinedResults.addAll(coupangFuture.get());
		} catch (Exception e) {
			throw new RuntimeException("Error merging search results: " + e.getMessage());
		}

		return combinedResults.stream()
			.sorted((a, b) -> b.getWeight() - a.getWeight())
			.collect(Collectors.toList());
	}
}