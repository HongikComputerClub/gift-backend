package com.team4.giftidea.controller;

import com.team4.giftidea.entity.Product;
import com.team4.giftidea.service.CoupangApiService;
import com.team4.giftidea.service.KreamApiService;
import com.team4.giftidea.service.NaverApiService;
import com.team4.giftidea.service.ProductService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

	private final NaverApiService naverApiService;
	private final CoupangApiService coupangApiService;
	private final ProductService productService;
	private final KreamApiService kreamApiService;

	public ProductController(
		NaverApiService naverApiService,
		CoupangApiService coupangApiService,
		KreamApiService kreamApiService,
		ProductService productService) {
		this.naverApiService = naverApiService;
		this.coupangApiService = coupangApiService;
		this.kreamApiService = kreamApiService;
		this.productService = productService;
	}

	@GetMapping("/crawl")
	public void crawlAndStoreData() {
		// 네이버 키워드 목록
		List<String> naverKeywords = List.of("현금 박스", "부모님 신발", "건강식품", "헬스가방");

		// 쿠팡 키워드 목록
		List<String> coupangKeywords = List.of("안마기기", "무선이어폰", "스마트워치");

		// Kream 키워드 목록
		List<String> kreamKeywords = List.of(
			"남성 지갑", "남성 스니커즈", "백팩", "토트백", "크로스백", "벨트",
			"선글라스", "향수", "여성 지갑", "여성 스니커즈", "숄더백", "크로스백", "목걸이"
		);

		// 네이버 크롤링
		naverKeywords.forEach(keyword -> {
			List<Product> naverProducts = naverApiService.searchItems(List.of(keyword));
			productService.saveItems(naverProducts, keyword);  // DB에 저장
		});

		// 쿠팡 크롤링
		coupangKeywords.forEach(keyword -> {
			List<Product> coupangProducts = coupangApiService.searchItems(keyword);
			productService.saveItems(coupangProducts, keyword);  // DB에 저장
		});

		// Kream 크롤링
		kreamKeywords.forEach(keyword -> {
			List<Product> kreamProducts = kreamApiService.searchItems(keyword);
			productService.saveItems(kreamProducts, keyword);  // DB에 저장
		});
	}

	@Scheduled(cron = "0 12 20 * * *")
	public void scheduleCrawl() {
		crawlAndStoreData();
	}
}