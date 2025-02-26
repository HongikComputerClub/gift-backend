package com.team4.giftidea.service;

import com.team4.giftidea.entity.Product;
import com.team4.giftidea.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.utils.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
public class CoupangPartnersService {

	private final RestTemplate restTemplate = new RestTemplate();
	private final ProductRepository productRepository;

	@Value("${coupang.api.base-url}")
	private String baseUrl;

	@Value("${coupang.api.access-key}")
	private String accessKey;

	@Value("${coupang.api.secret-key}")
	private String secretKey;

	@Value("${coupang.api.partner-id}")
	private String partnerId;

	public CoupangPartnersService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	/**
	 * DB에서 모든 쿠팡 상품을 조회하고, 파트너스 링크로 업데이트
	 */
	@Transactional
	public int updateAllCoupangProductLinks() {
		log.info("🔍 [START] 쿠팡 상품의 파트너스 링크 업데이트 시작");

		List<Product> coupangProducts = productRepository.findByMallName("Coupang");
		log.info("📦 총 {}개의 쿠팡 상품을 찾음", coupangProducts.size());

		int updatedCount = 0;
		for (Product product : coupangProducts) {
			String originalUrl = product.getLink();
			log.info("🔗 상품 ID {}의 기존 URL: {}", product.getProductId(), originalUrl);

			String partnerLink = generatePartnerLink(originalUrl);
			if (partnerLink != null) {
				log.info("✅ 상품 ID {}의 변환된 파트너스 링크: {}", product.getProductId(), partnerLink);
				product.setLink(partnerLink);
				productRepository.save(product);
				updatedCount++;
			} else {
				log.warn("⚠️ 파트너스 링크 생성 실패 (상품 ID: {})", product.getProductId());
			}
		}
		log.info("🎯 [END] 총 {}개의 쿠팡 상품이 업데이트됨", updatedCount);
		return updatedCount;
	}

	/**
	 * 기존 쿠팡 상품 URL을 파트너스 트래킹 URL로 변환하기 위한 API 호출
	 */
	private String generatePartnerLink(String originalUrl) {
		try {
			// 엔드포인트 URI (baseUrl과 결합)
			String endpoint = "/v2/providers/affiliate_open_api/apis/openapi/v1/deeplink";
			String apiUrl = baseUrl + endpoint;
			log.info("📡 쿠팡 파트너스 API 호출: {}", apiUrl);

			// HMAC 기반 Authorization 헤더 생성
			String authorization = generateAuthorizationHeader("POST", endpoint);
			log.info("🔑 생성된 Authorization 헤더: {}", authorization);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("Authorization", authorization);
			String requestId = UUID.randomUUID().toString();
			headers.set("X-Request-Id", requestId);
			log.info("🆔 X-Request-Id: {}", requestId);

			// 요청 바디 구성 (문서 예시에 맞게 coupangUrls와 subId 포함)
			Map<String, Object> requestBody = new HashMap<>();
			requestBody.put("coupangUrls", Collections.singletonList(originalUrl));
			requestBody.put("subId", partnerId);
			log.info("🔍 요청 바디: {}", requestBody);
			log.debug("🔍 요청 헤더: {}", headers);

			long startTime = System.currentTimeMillis();
			HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
			ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, Map.class);
			long duration = System.currentTimeMillis() - startTime;
			log.info("⏱️ API 호출 소요 시간: {}ms", duration);

			log.info("🔍 API 응답 상태 코드: {}", response.getStatusCode());
			log.debug("🔍 API 응답 헤더: {}", response.getHeaders());
			log.info("📦 API 응답 바디: {}", response.getBody());

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				String rCode = (String) response.getBody().get("rCode");
				if ("0".equals(rCode)) {
					List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
					if (data != null && !data.isEmpty()) {
						return data.get(0).get("shortenUrl").toString();
					} else {
						log.warn("⚠️ 응답에 data 없음, API 응답: {}", response.getBody());
					}
				} else {
					log.warn("⚠️ API 호출 실패, rCode: {}, rMessage: {}", rCode, response.getBody().get("rMessage"));
				}
			} else {
				log.warn("⚠️ API 호출 실패, 상태 코드: {}", response.getStatusCode());
			}
		} catch (Exception e) {
			log.error("❌ 쿠팡 파트너스 링크 생성 중 오류 발생: {}", e.getMessage(), e);
		}
		return null;
	}

	/**
	 * HMAC 서명 기반의 Authorization 헤더 생성
	 *
	 * 메시지 형식: signedDate + method + path + query
	 * signedDate 포맷: "yyMMdd'T'HHmmss'Z'" (GMT 기준)
	 *
	 * 최종 형식:
	 * "CEA algorithm=HmacSHA256, access-key=ACCESS_KEY, signed-date=SIGNED_DATE, signature=SIGNATURE"
	 */
	private String generateAuthorizationHeader(String method, String uri) {
		// GMT 기준 날짜/시간 생성 (형식: yyMMdd'T'HHmmss'Z')
		SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyMMdd'T'HHmmss'Z'");
		dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		String signedDate = dateFormatGmt.format(new Date());

		// uri에서 path와 query 분리 (query가 없는 경우 빈 문자열 사용)
		String[] parts = uri.split("\\?", 2);
		String path = parts[0];
		String query = (parts.length == 2) ? parts[1] : "";

		// 메시지 생성
		String message = signedDate + method + path + query;
		log.debug("🔐 서명할 메시지: {}", message);

		String signature;
		try {
			SecretKeySpec signingKey = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(signingKey);
			byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
			signature = Hex.encodeHexString(rawHmac);
			log.debug("🔐 생성된 서명: {}", signature);
		} catch (Exception e) {
			throw new RuntimeException("HMAC 서명 생성 오류: " + e.getMessage(), e);
		}

		// 최종 Authorization 헤더 생성
		return String.format("CEA algorithm=%s, access-key=%s, signed-date=%s, signature=%s",
			"HmacSHA256", accessKey, signedDate, signature);
	}
}