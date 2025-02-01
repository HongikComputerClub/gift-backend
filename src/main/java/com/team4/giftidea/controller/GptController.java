package com.team4.giftidea.controller;

import com.team4.giftidea.configuration.GptConfig;
import com.team4.giftidea.dto.GptRequestDTO;
import com.team4.giftidea.dto.GptResponseDTO;
import com.team4.giftidea.entity.Product;
import com.team4.giftidea.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Arrays;

/**
 * GPT API와 연동하여 선물 추천을 제공하는 컨트롤러
 */
@RestController
@RequestMapping("/api/gpt")
@Slf4j
public class GptController {

  private final RestTemplate restTemplate;
  private final GptConfig gptConfig;
  private final ProductService productService;

  @Autowired
  public GptController(RestTemplate restTemplate, GptConfig gptConfig, ProductService productService) {
    this.restTemplate = restTemplate;
    this.gptConfig = gptConfig;
    this.productService = productService;
  }

  /**
   * GPT API를 호출하여 선물 추천을 생성하고, 해당 카테고리로 DB에서 상품을 검색하여 반환합니다.
   *
   * @param filePath 대화 내용이 저장된 파일 경로
   * @param relation 사용자와의 관계 (예: couple, parent, friend)
   * @param sex      성별 (male, female)
   * @param theme    선물 테마 (birth, anniversary 등)
   * @return 검색된 상품 리스트
   */
  @GetMapping("/chat")
  public List<Product> chat(
      @RequestParam(name = "filePath") String filePath,
      @RequestParam(name = "relation") String relation,
      @RequestParam(name = "sex") String sex,
      @RequestParam(name = "theme") String theme) {

    String prompt = generatePrompt(filePath, relation, sex, theme);
    GptRequestDTO request = new GptRequestDTO(gptConfig.getModel(), prompt);
    GptResponseDTO response = restTemplate.postForObject(gptConfig.getApiUrl(), request, GptResponseDTO.class);

    if (response != null && !response.getChoices().isEmpty()) {
      // GPT 응답에서 카테고리 추출
      String categories = response.getChoices().get(0).getMessage().getContent();
      // 카테고리로 상품 검색
      List<String> keywords = Arrays.asList(categories.split(","));
      return productService.searchByKeywords(keywords, 0); // 첫 페이지의 상품 20개 검색
    }
    return List.of(); // 상품이 없거나 오류 발생 시 빈 리스트 반환
  }

  /**
   * 사용자의 관계, 성별, 테마에 맞는 GPT 프롬프트를 생성합니다.
   */
  private String generatePrompt(String filePath, String relation, String sex, String theme) {
    String message = readFile(filePath);

    if ("couple".equals(relation)) {
      if ("male".equals(sex)) {
        return extractKeywordsAndReasonsCoupleMan(theme, message);
      } else if ("female".equals(sex)) {
        return extractKeywordsAndReasonsCoupleWoman(theme, message);
      }
    } else if ("parent".equals(relation)) {
      return extractKeywordsAndReasonsParents(theme, message);
    } else if ("friend".equals(relation)) {
      return extractKeywordsAndReasonsFriend(theme, message);
    } else if ("housewarming".equals(theme)) {
      return extractKeywordsAndReasonsHousewarming(message);
    } else if ("valentine".equals(theme)) {
      return extractKeywordsAndReasonsSeasonal(theme, message);
    }

    return "조건에 맞는 선물 추천 기능이 없습니다.";
  }

  /**
   * GPT API를 호출하여 텍스트를 생성합니다.
   */
  private String generateText(String prompt) {
    GptRequestDTO request = new GptRequestDTO(gptConfig.getModel(), prompt);
    try {
      GptResponseDTO response = restTemplate.postForObject(gptConfig.getApiUrl(), request, GptResponseDTO.class);

      if (response != null && !response.getChoices().isEmpty()) {
        return response.getChoices().get(0).getMessage().getContent();
      }
      return "GPT 응답에 오류가 발생했습니다.";
    } catch (Exception e) {
      log.error("GPT 요청 중 오류 발생: ", e);
      return "GPT 요청 중 오류가 발생했습니다.";
    }
  }

  private String extractKeywordsAndReasonsCoupleMan(String theme, String message) {
    return generateText(String.format("""
            다음 텍스트를 참고하여 남자 애인이 %s에 선물로 받으면 좋아할 카테고리 3개와 판단 근거를 제공해주세요.
            카테고리: 지갑, 신발, 백팩, 토트백, 크로스백, 벨트, 선글라스, 향수, 헬스가방, 무선이어폰, 스마트워치, 셔츠
            텍스트: %s
            """, theme, message));
  }

  private String extractKeywordsAndReasonsCoupleWoman(String theme, String message) {
    return generateText(String.format("""
            다음 텍스트를 참고하여 여자 애인이 %s에 선물로 받으면 좋아할 카테고리 3개와 판단 근거를 제공해주세요.
            카테고리: 지갑, 신발, 숄더백, 토트백, 크로스백, 향수, 목걸이, 무선이어폰, 스마트워치, 가디건
            텍스트: %s
            """, theme, message));
  }

  private String extractKeywordsAndReasonsParents(String theme, String message) {
    return generateText(String.format("""
            다음 텍스트를 참고하여 부모님이 %s에 선물로 받으면 좋아할 카테고리 3개와 판단 근거를 제공해주세요.
            카테고리: 현금 박스, 안마기기, 신발, 건강식품, 여행
            텍스트: %s
            """, theme, message));
  }

  private String extractKeywordsAndReasonsFriend(String theme, String message) {
    return generateText(String.format("""
            다음 텍스트를 참고하여 친구가 %s에 선물로 받으면 좋아할 카테고리 3개와 판단 근거를 제공해주세요.
            제시된 카테고리에 없는 추천 선물이 있다면 포함해주세요.
            카테고리: 핸드크림, 텀블러, 립밤
            텍스트: %s
            """, theme, message));
  }

  private String extractKeywordsAndReasonsHousewarming(String message) {
    return generateText(String.format("""
            다음 텍스트를 참고하여 집들이에 선물로 받으면 좋아할 카테고리 3개와 판단 근거를 제공해주세요.
            카테고리: 조명, 핸드워시, 식기, 디퓨저, 꽃, 티세트, 휴지
            텍스트: %s
            """, message));
  }

  private String extractKeywordsAndReasonsSeasonal(String theme, String message) {
    return generateText(String.format("""
            다음 텍스트를 참고하여 %s에 선물로 받으면 좋아할 카테고리 3개와 판단 근거를 제공해주세요.
            카테고리: 초콜릿, 수제 초콜릿, 립밤, 파자마세트, 꽃
            텍스트: %s
            """, theme, message));
  }

  /**
   * 파일에서 텍스트를 읽어 반환합니다.
   */
  private String readFile(String filePath) {
    try {
      return new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
    } catch (IOException e) {
      log.error("파일 읽기 오류: ", e);
      return "파일을 읽을 수 없습니다.";
    }
  }
}