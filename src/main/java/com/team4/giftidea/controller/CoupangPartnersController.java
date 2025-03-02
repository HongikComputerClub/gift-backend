package com.team4.giftidea.controller;

import com.team4.giftidea.service.CoupangPartnersService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupang")
public class CoupangPartnersController {

	private final CoupangPartnersService coupangPartnersService;

	public CoupangPartnersController(CoupangPartnersService coupangPartnersService) {
		this.coupangPartnersService = coupangPartnersService;
	}

	/**
	 * DB의 모든 쿠팡 상품을 조회하여 파트너스 링크로 업데이트하는 API
	 */
	@PostMapping("/update-all")
	public ResponseEntity<String> updateAllCoupangProductLinks() {
		int updatedCount = coupangPartnersService.updateAllCoupangProductLinks();
		return ResponseEntity.ok(updatedCount + "개의 쿠팡 상품이 업데이트되었습니다.");
	}
}