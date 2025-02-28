package com.team4.giftidea.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team4.giftidea.configuration.GptConfig;
import com.team4.giftidea.dto.GptRequestDTO;
import com.team4.giftidea.dto.GptResponseDTO;
import com.team4.giftidea.entity.Product;
import com.team4.giftidea.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "🎁 GPT 추천 API", description = "카카오톡 대화를 분석하여 GPT를 통해 추천 선물을 제공하는 API")
@RestController
@RequestMapping("/api/gpt")
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

  // GPT 모델의 입력 토큰 제한 (예: 11000)
  private static final int GPT_INPUT_LIMIT = 11000;

  @Operation(
      summary = "카톡 대화 분석 후 선물 추천",
      description = "카카오톡 대화 파일을 분석하여 GPT API를 이용해 키워드를 추출하고, 이에 맞는 추천 상품을 반환합니다."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "추천 상품 목록 반환"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
      @ApiResponse(responseCode = "415", description = "지원되지 않는 파일 형식"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류 발생")
  })
  @PostMapping(value = "/process", consumes = "multipart/form-data", produces = "application/json")
  public Map<String, Object> processFileAndRecommend(
      @RequestParam("file") @Parameter(description = "카카오톡 대화 파일 (.txt)", required = true) MultipartFile file,
      @RequestParam("targetName") @Parameter(description = "분석 대상 이름 (예: '여자친구')", required = true) String targetName,
      @RequestParam("relation") @Parameter(description = "대상과의 관계 (couple, friend, parent 등)", required = true) String relation,
      @RequestParam("sex") @Parameter(description = "대상 성별 (male 또는 female)", required = true) String sex,
      @RequestParam("theme") @Parameter(description = "선물 주제 (birthday, valentine 등)", required = true) String theme
  ) {
    // 1. 파일의 모든 줄 중 targetName이 포함된 줄을 수집
    List<String> allTargetLines = new ArrayList<>();
    int formatType = detectFormatType(file);
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.contains(targetName) && !line.trim().isEmpty()) {
          String formattedLine = formatLine(line, formatType, targetName);
          allTargetLines.add(formattedLine);
        }
      }
    } catch (IOException e) {
      log.error("파일 읽기 오류: ", e);
    }

    // 2. 파일의 아랫부분부터 역순으로 토큰을 누적하여 GPT_INPUT_LIMIT 이하 내용 선택
    int currentTokenCount = 0;
    List<String> selectedLines = new ArrayList<>();
    for (int i = allTargetLines.size() - 1; i >= 0; i--) {
      String currentLine = allTargetLines.get(i);
      int tokenCount = countTokens(currentLine);
      if (currentTokenCount + tokenCount > GPT_INPUT_LIMIT) {
        break;
      }
      selectedLines.add(currentLine);
      currentTokenCount += tokenCount;
    }
    Collections.reverse(selectedLines);
    StringBuilder finalChunk = new StringBuilder();
    for (String s : selectedLines) {
      finalChunk.append(s).append("\n");
    }
    List<String> processedMessages = new ArrayList<>();
    processedMessages.add(finalChunk.toString());

    // (옵션) 로컬 파일에 저장
    try {
      File outputFile = new File(System.getProperty("user.home"), "processed_kakaochat.txt");
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, false))) {
        writer.write(finalChunk.toString());
        writer.flush();
      }
      log.info("전처리 완료. 결과 파일 저장 위치: " + outputFile.getAbsolutePath());
    } catch (IOException e) {
      log.error("파일 저장 오류: ", e);
    }

    // 3. GPT API 호출: 전처리된 메시지(청크)로부터 키워드 및 근거 추출
    String gptResponse = generatePrompt(processedMessages, relation, sex, theme);

    // 4. GPT 응답 파싱
    // 예상 응답 예시:
    // "Categories: 향수, 무선이어폰, 목걸이\n- 향수: [첫번째 근거]\n- 향수: [마지막 근거]\n- 무선이어폰: [근거]\n- 목걸이: [근거]"
    String[] responseParts = gptResponse.split("\n", 2);
    String categoriesPart = responseParts[0].replace("Categories: ", "").trim();
    String reasonsPart = responseParts.length > 1 ? responseParts[1].trim() : "";

    List<String> keywords = Arrays.stream(categoriesPart.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toList());

    // 파싱: 각 reason 줄에서 키워드와 설명 추출하고, 같은 키워드는 마지막 설명으로 덮어씀.
    Map<String, String> reasonMap = new HashMap<>();
    String[] reasonLines = reasonsPart.split("\n");
    for (String line : reasonLines) {
      line = line.trim();
      if (line.startsWith("- ")) {
        int colonIdx = line.indexOf(":");
        if (colonIdx != -1) {
          String key = line.substring(2, colonIdx).trim();  // 예: "향수"
          String value = line.substring(colonIdx + 1).trim();
          reasonMap.put(key, value); // 마지막에 나온 설명이 덮어쓰기 됨
        }
      }
    }

    // 최종 reason 객체 리스트 생성: 각 키워드에 대해 reasonMap에서 설명 가져오기
    List<Map<String, String>> reasonList = new ArrayList<>();
    for (String keyword : keywords) {
      if (reasonMap.containsKey(keyword)) {
        Map<String, String> entry = new HashMap<>();
        entry.put("keyword", keyword);
        entry.put("reason", reasonMap.get(keyword));
        reasonList.add(entry);
      }
    }

    // 5. 데이터베이스에서 상품 검색 (키워드를 이용)
    List<Product> productsNoReason = productService.searchByKeywords(keywords);

    // 6. 최종 응답 구성 (JSON 객체)
    Map<String, Object> result = new HashMap<>();
    result.put("product", productsNoReason);
    result.put("reason", reasonList);
    return result;
  }

  private int detectFormatType(MultipartFile file) {
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
      String firstLine = reader.readLine();
      if (firstLine != null && firstLine.contains("님과 카카오톡 대화")) {
        return 1;
      } else {
        return 2;
      }
    } catch (IOException e) {
      log.error("파일 판별 오류: ", e);
    }
    return 0;
  }

  private String formatLine(String line, int formatType, String targetName) {
    if (formatType == 1) {
      return line.replaceAll("\\[.*?\\] \\[.*?\\] ", "")
          .replaceAll("[ㅎㅋ.]+", "").trim();
    } else if (formatType == 2) {
      return line.replaceAll("^" + targetName + " : ", "")
          .replaceAll("[ㅎㅋ.]+", "").trim();
    }
    return line;
  }

  private int countTokens(String text) {
    if (text == null || text.isEmpty()) {
      return 0;
    }
    return text.split("\\s+").length;
  }

  private String generatePrompt(List<String> processedMessages, String relation, String sex, String theme) {
    String combinedMessages = String.join("\n", processedMessages);
    if ("couple".equals(relation)) {
      if ("male".equals(sex)) {
        return extractKeywordsAndReasonsCoupleMan(theme, combinedMessages);
      } else if ("female".equals(sex)) {
        return extractKeywordsAndReasonsCoupleWoman(theme, combinedMessages);
      }
    } else if ("parent".equals(relation)) {
      if ("male".equals(sex)) {
        return extractKeywordsAndReasonsDad(theme, combinedMessages);
      } else if ("female".equals(sex)) {
        return extractKeywordsAndReasonsMom(theme, combinedMessages);
      }
    } else if ("friend".equals(relation)) {
      return extractKeywordsAndReasonsFriend(theme, combinedMessages);
    } else if ("housewarming".equals(theme)) {
      return extractKeywordsAndReasonsHousewarming(combinedMessages);
    } else if ("valentine".equals(theme)) {
      if ("male".equals(sex)) {
        return extractKeywordsAndReasonsSeasonalMan(theme, combinedMessages);
      } else if ("female".equals(sex)) {
        return extractKeywordsAndReasonsSeasonalWoman(theme, combinedMessages);
      }
    }
    return "조건에 맞는 선물 추천 기능이 없습니다.";
  }

  private String generateText(String prompt) {
    GptRequestDTO request = new GptRequestDTO(gptConfig.getModel(), prompt, 110);
    try {
      ObjectMapper mapper = new ObjectMapper();
      GptResponseDTO response = restTemplate.postForObject(gptConfig.getApiUrl(), request, GptResponseDTO.class);
      if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
        String content = response.getChoices().get(0).getMessage().getContent();
        log.debug("GPT 전체 응답: {}", content);

        // 응답 포맷: "1. [카테고리1,카테고리2,카테고리3]\n2.\n(이후 Reasons 내용)"
        if (content.contains("1.") && content.contains("2.")) {
          String[] parts = content.split("2\\.", 2);
          String part1 = parts[0].trim();
          String reasonsPart = parts[1].trim();

          if (part1.startsWith("1.")) {
            part1 = part1.substring(2).trim();
          }
          int startIdx = part1.indexOf("[");
          int endIdx = part1.indexOf("]");
          String categories = "";
          if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            categories = part1.substring(startIdx + 1, endIdx).trim();
          } else {
            log.warn("카테고리 부분 추출 실패, 전체 내용: {}", part1);
          }
          log.debug("추출된 카테고리: {}", categories);
          log.debug("추출된 Reasons: {}", reasonsPart);

          return "Categories: " + categories + "\n" + reasonsPart;
        } else {
          log.warn("응답 포맷이 예상과 다릅니다: {}", content);
        }
      } else {
        log.warn("GPT 응답이 null이거나 choices가 비어 있습니다.");
      }
      return "GPT 응답 오류 발생";
    } catch (Exception e) {
      log.error("GPT 요청 중 오류 발생: ", e);
      if (e.getCause() != null) {
        log.error("원인 예외: {}", e.getCause().getMessage());
      }
      return "GPT 요청 오류";
    }
  }

  private String extractKeywordsAndReasonsCoupleMan(String theme, String message) {
    String prompt = String.format("""
            다음 텍스트를 참고하여 남자 애인이 %s에 선물로 받으면 좋아할 카테고리 3개와 판단에 참고한 대화를 100토큰 내로 제공해주세요.
            카테고리: 남성 지갑, 남성 스니커즈, 백팩, 토트백, 크로스백, 벨트, 선글라스, 향수, 헬스가방, 무선이어폰, 스마트워치, 맨투맨, 마우스, 키보드, 전기면도기, 게임기

            텍스트: %s

            출력 형식:
            1. [카테고리1,카테고리2,카테고리3]
            2. 
               - 카테고리1: [근거1]
               - 카테고리2: [근거2]
               - 카테고리3: [근거3]
            """, theme, message);
    return generateText(prompt);
  }

  private String extractKeywordsAndReasonsCoupleWoman(String theme, String message) {
    String prompt = String.format("""
            다음 텍스트를 참고하여 여자 애인이 %s에 선물로 받으면 좋아할 카테고리 3개와 판단에 참고한 대화를 100토큰 내로 제공해주세요.
            카테고리: 여성 지갑, 여성 스니커즈, 숄더백, 토트백, 크로스백, 향수, 목걸이, 무선이어폰, 스마트워치, 에어랩

            텍스트: %s

            출력 형식:
            1. [카테고리1,카테고리2,카테고리3]
            2. 
               - 카테고리1: [근거1]
               - 카테고리2: [근거2]
               - 카테고리3: [근거3]
            """, theme, message);
    return generateText(prompt);
  }

  private String extractKeywordsAndReasonsDad(String theme, String message) {
    String prompt = String.format("""
            다음 텍스트를 참고하여 부모님이 %s에 선물로 받으면 좋아할 카테고리 3개와 판단에 참고한 대화를 100토큰 내로 제공해주세요.
            카테고리: 현금 박스, 안마기기, 아버지 신발, 시계

            텍스트: %s

            출력 형식:
            1. [카테고리1,카테고리2,카테고리3]
            2. 
               - 카테고리1: [근거1]
               - 카테고리2: [근거2]
               - 카테고리3: [근거3]
            """, theme, message);
    return generateText(prompt);
  }

  private String extractKeywordsAndReasonsMom(String theme, String message) {
    String prompt = String.format("""
            다음 텍스트를 참고하여 부모님이 %s에 선물로 받으면 좋아할 카테고리 3개와 판단에 참고한 대화를 100토큰 내로 제공해주세요.
            카테고리: 현금 박스, 안마기기, 어머니 신발, 건강식품, 스카프

            텍스트: %s

            출력 형식:
            1. [카테고리1,카테고리2,카테고리3]
            2. 
               - 카테고리1: [근거1]
               - 카테고리2: [근거2]
               - 카테고리3: [근거3]
            """, theme, message);
    return generateText(prompt);
  }

  private String extractKeywordsAndReasonsFriend(String theme, String message) {
    String prompt = String.format("""
            다음 텍스트를 참고하여 친구가 %s에 선물로 받으면 좋아할 카테고리 3개와 판단에 참고한 대화를 100토큰 내로 제공해주세요.
            제시된 카테고리에 없는 추천 선물이 있다면 3개에 포함해주세요.
            카테고리: 핸드크림, 텀블러, 립밤, 머플러, 비타민, 입욕제, 블루투스 스피커

            텍스트: %s

            출력 형식:
            1. [카테고리1,카테고리2,카테고리3]
            2. 
               - 카테고리1: [근거1]
               - 카테고리2: [근거2]
               - 카테고리3: [근거3]
            """, theme, message);
    return generateText(prompt);
  }

  private String extractKeywordsAndReasonsHousewarming(String message) {
    String prompt = String.format("""
            다음 텍스트를 참고하여 집들이에 선물로 받으면 좋아할 카테고리 3개와 판단에 참고한 대화를 100토큰 내로 제공해주세요.
            카테고리: 조명, 핸드워시, 식기, 디퓨저, 오설록 티세트, 휴지, 파자마세트, 무드등, 디퓨저, 수건, 전기포트, 에어프라이기

            텍스트: %s

            출력 형식:
            1. [카테고리1,카테고리2,카테고리3]
            2. 
               - 카테고리1: [근거1]
               - 카테고리2: [근거2]
               - 카테고리3: [근거3]
            """, message);
    return generateText(prompt);
  }

  private String extractKeywordsAndReasonsSeasonalMan(String theme, String message) {
    String prompt = String.format("""
            다음 텍스트를 참고하여 %s에 선물로 받으면 좋아할 카테고리 3개와 판단에 참고한 대화를 100토큰 내로 제공해주세요.
            카테고리: 초콜릿, 수제 초콜릿 키트, 파자마세트, 남자 화장품

            텍스트: %s

            출력 형식:
            1. [카테고리1,카테고리2,카테고리3]
            2. 
               - 카테고리1: [근거1]
               - 카테고리2: [근거2]
               - 카테고리3: [근거3]
            """, theme, message);
    return generateText(prompt);
  }

  private String extractKeywordsAndReasonsSeasonalWoman(String theme, String message) {
    String prompt = String.format("""
            다음 텍스트를 참고하여 %s에 선물로 받으면 좋아할 카테고리 3개와 판단에 참고한 대화를 100토큰 내로 제공해주세요.
            카테고리: 초콜릿, 수제 초콜릿 키트, 립밤, 파자마세트, 립스틱

            텍스트: %s

            출력 형식:
            1. [카테고리1,카테고리2,카테고리3]
            2. 
               - 카테고리1: [근거1]
               - 카테고리2: [근거2]
               - 카테고리3: [근거3]
            """, theme, message);
    return generateText(prompt);
  }
}