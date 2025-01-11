package com.team4.giftidea.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.team4.giftidea.service.NaverApiService;

@RestController
@RequestMapping("/api/naver") // 기본 경로 설정
public class NaverApiController {

	private final NaverApiService naverApiService;

	public NaverApiController(NaverApiService naverApiService) {
		this.naverApiService = naverApiService;
	}

	@GetMapping("/search")
	public String search(@RequestParam String query) {
		return naverApiService.searchItems(query);
	}
}