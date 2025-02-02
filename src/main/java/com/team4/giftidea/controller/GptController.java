package com.team4.giftidea.controller;

import com.team4.giftidea.configuration.GptConfig;
import com.team4.giftidea.dto.GptRequestDTO;
import com.team4.giftidea.dto.GptResponseDTO;
import com.team4.giftidea.entity.Product;
import com.team4.giftidea.service.ProductService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

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

  @PostMapping("/process")
  public List<Product> processFileAndRecommend(
      @RequestParam("file") MultipartFile file,
      @RequestParam("targetName") String targetName,
      @RequestParam("relation") String relation,
      @RequestParam("sex") String sex,
      @RequestParam("theme") String theme) {

    // 1. 파일 전처리
    List<String> processedMessages = preprocessKakaoFile(file, targetName);

    // 2. GPT API 호출: 전처리된 메시지로 프롬프트 생성
    String prompt = generatePrompt(processedMessages, relation, sex, theme);
    GptRequestDTO request = new GptRequestDTO(gptConfig.getModel(), prompt);
    GptResponseDTO response = restTemplate.postForObject(gptConfig.getApiUrl(), request, GptResponseDTO.class);

    if (response != null && !response.getChoices().isEmpty()) {
      // 3. GPT 응답에서 추천된 카테고리 추출
      String categories = response.getChoices().get(0).getMessage().getContent();
      List<String> keywords = Arrays.asList(categories.split(","));

      // 4. 상품 검색 (DB에서 카테고리 기반으로 추천 상품 검색)
      return productService.searchByKeywords(keywords, 0);
    }

    return Collections.emptyList();  // 오류 발생 시 빈 리스트 반환
  }

  private List<String> preprocessKakaoFile(MultipartFile file, String targetName) {
    List<String> processedMessages = new ArrayList<>();
    int formatType = detectFormatType(file); // 양식 자동 판별
    File outputFile = null;

    try {
      // 파일을 임시로 저장
      outputFile = File.createTempFile("processed_kakaochat", ".txt");

      try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
           BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) { // 파일에 쓸 준비

        String line;

        while ((line = reader.readLine()) != null) {
          // 양식 1 (예시 1) 처리
          if (formatType == 1) {
            if (line.contains(targetName) && !line.trim().isEmpty()) {
              line = line.replaceAll("\\[.*?\\] \\[.*?\\] ", "").trim();  // 시간과 이름 제거
              line = line.replaceAll("[ㅎㅋ.]+", "").trim();  // 반복 문자 및 특수문자 제거
              processedMessages.add(line);
              writer.write(line);
              writer.newLine();  // 각 메시지 끝에 새 줄 추가
            }
          }
          // 양식 2 (예시 2) 처리
          else if (formatType == 2) {
            if (line.contains(targetName) && !line.trim().isEmpty()) {
              line = line.replaceAll("^" + targetName + " : ", "").trim();  // 대화자 이름 제거
              line = line.replaceAll("[ㅎㅋ.]+", "").trim();  // 반복 문자 및 특수문자 제거
              processedMessages.add(line);
              writer.write(line);
              writer.newLine();  // 각 메시지 끝에 새 줄 추가
            }
          }
        }
      }
    } catch (IOException e) {
      log.error("파일 처리 오류: ", e);
    }

    // 파일 삭제 (전처리 후 필요 없으므로 삭제)
    if (outputFile != null) {
      outputFile.delete();
    }

    return processedMessages;
  }

  private int detectFormatType(MultipartFile file) {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
      String firstLine = reader.readLine();

      if (firstLine != null && firstLine.contains("님과 카카오톡 대화")) {
        return 1; // 양식 1
      } else {
        return 2; // 양식 2
      }

    } catch (IOException e) {
      log.error("파일 판별 오류: ", e);
    }

    return 0; // 기본값: 알 수 없는 양식
  }

  private String generatePrompt(List<String> processedMessages, String relation, String sex, String theme) {
    String combinedMessages = String.join("\n", processedMessages);  // List<String>을 하나의 String으로 합침

    switch (relation) {
      case "couple":
        return sex.equals("male") ? extractKeywordsAndReasonsCoupleMan(theme, combinedMessages)
            : extractKeywordsAndReasonsCoupleWoman(theme, combinedMessages);
      case "parent":
        return extractKeywordsAndReasonsParents(theme, combinedMessages);
      case "friend":
        return extractKeywordsAndReasonsFriend(theme, combinedMessages);
      case "housewarming":
        return extractKeywordsAndReasonsHousewarming(combinedMessages);
      case "valentine":
        return extractKeywordsAndReasonsSeasonal(theme, combinedMessages);
      default:
        return "조건에 맞는 선물 추천이 없습니다.";
    }
  }

  private String generateText(String prompt) {
    GptRequestDTO request = new GptRequestDTO(gptConfig.getModel(), prompt);
    try {
      GptResponseDTO response = restTemplate.postForObject(gptConfig.getApiUrl(), request, GptResponseDTO.class);
      if (response != null && !response.getChoices().isEmpty()) {
        return response.getChoices().get(0).getMessage().getContent();
      }
      return "GPT 응답 오류 발생";
    } catch (Exception e) {
      log.error("GPT 요청 중 오류 발생: ", e);
      return "GPT 요청 오류";
    }
  }

  private String extractKeywordsAndReasonsCoupleMan(String theme, String message) {
    String prompt = String.format("""
    다음 텍스트를 참고하여 남자 애인이 %s에 선물로 받으면 좋아할 카테고리 3개와 판단에 참고한 대화를 제공해주세요. 
    카테고리: 지갑, 신발, 백팩, 토트백, 크로스백, 벨트, 선글라스, 향수, 헬스가방, 무선이어폰, 스마트워치, 셔츠

    텍스트: %s

    출력 형식:
    1. [카테고리1,카테고리2,카테고리3]
    2. 
       - 카테고리1: [근거1]
       - 카테고리2: [근거2]
       - 카테고리3: [근거3]
    """, theme, message);

    return generateText(prompt);  // GPT 모델 호출
  }

  private String extractKeywordsAndReasonsCoupleWoman(String theme, String message) {
    String prompt = String.format("""
    다음 텍스트를 참고하여 여자 애인이 %s에 선물로 받으면 좋아할 카테고리 3개와 판단에 참고한 대화를 제공해주세요. 
    카테고리: 지갑, 신발, 숄더백, 토트백, 크로스백, 향수, 목걸이, 무선이어폰, 스마트워치, 가디건

    텍스트: %s

    출력 형식:
    1. [카테고리1,카테고리2,카테고리3]
    2. 
       - 카테고리1: [근거1]
       - 카테고리2: [근거2]
       - 카테고리3: [근거3]
    """, theme, message);

    return generateText(prompt);  // GPT 모델 호출
  }

  private String extractKeywordsAndReasonsParents(String theme, String message) {
    String prompt = String.format("""
    다음 텍스트를 참고하여 부모님이 %s에 선물로 받으면 좋아할 카테고리 3개와 판단에 참고한 대화를 제공해주세요. 
    카테고리: 현금 박스, 안마기기, 신발, 건강식품, 여행

    텍스트: %s

    출력 형식:
    1. [카테고리1,카테고리2,카테고리3]
    2. 
       - 카테고리1: [근거1]
       - 카테고리2: [근거2]
       - 카테고리3: [근거3]
    """, theme, message);

    return generateText(prompt);  // GPT 모델 호출
  }

  private String extractKeywordsAndReasonsFriend(String theme, String message) {
    String prompt = String.format("""
    다음 텍스트를 참고하여 친구가 %s에 선물로 받으면 좋아할 카테고리 3개와 판단에 참고한 대화를 제공해주세요. 
    제시된 카테고리에 없는 추천 선물이 있다면 3개에 포함해주세요.
    카테고리: 핸드크림, 텀블러, 립밤

    텍스트: %s

    출력 형식:
    1. [카테고리1,카테고리2,카테고리3]
    2. 
       - 카테고리1: [근거1]
       - 카테고리2: [근거2]
       - 카테고리3: [근거3]
    """, theme, message);

    return generateText(prompt);  // GPT 모델 호출
  }

  private String extractKeywordsAndReasonsHousewarming(String message) {
    String prompt = String.format("""
    다음 텍스트를 참고하여 집들이에 선물로 받으면 좋아할 카테고리 3개와 판단에 참고한 대화를 제공해주세요. 
    카테고리: 조명, 핸드워시, 식기, 디퓨저, 꽃, 티세트, 휴지

    텍스트: %s

    출력 형식:
    1. [카테고리1,카테고리2,카테고리3]
    2. 
       - 카테고리1: [근거1]
       - 카테고리2: [근거2]
       - 카테고리3: [근거3]
    """, message);

    return generateText(prompt);  // GPT 모델 호출
  }

  private String extractKeywordsAndReasonsSeasonal(String theme, String message) {
    String prompt = String.format("""
    다음 텍스트를 참고하여 %s에 선물로 받으면 좋아할 카테고리 3개와 판단에 참고한 대화를 제공해주세요. 
    카테고리: 초콜릿, 수제 초콜릿, 립밤, 파자마세트, 꽃

    텍스트: %s

    출력 형식:
    1. [카테고리1,카테고리2,카테고리3]
    2. 
       - 카테고리1: [근거1]
       - 카테고리2: [근거2]
       - 카테고리3: [근거3]
    """, theme, message);

    return generateText(prompt);  // GPT 모델 호출
  }

  private String readFile(String filePath) {
    try {
      return new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
    } catch (IOException e) {
      e.printStackTrace();
      return "파일을 읽을 수 없습니다.";
    }
  }
}
