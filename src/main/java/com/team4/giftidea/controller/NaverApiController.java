package com.team4.giftidea.controller;

import com.team4.giftidea.dto.ItemDTO;
import com.team4.giftidea.service.NaverApiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/naver")
public class NaverApiController {

	private final NaverApiService naverApiService;

	public NaverApiController(NaverApiService naverApiService) {
		this.naverApiService = naverApiService;
	}

	@GetMapping("/search")
	public List<ItemDTO> search(@RequestParam(value = "query") List<String> queries) {
		return naverApiService.searchItems(queries);
	}
}