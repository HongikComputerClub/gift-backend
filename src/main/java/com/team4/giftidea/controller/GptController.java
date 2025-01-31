package com.team4.giftidea.controller;

import com.team4.giftidea.dto.GptRequestDTO;
import com.team4.giftidea.dto.GptResponseDTO;
import com.team4.giftidea.configuration.GptConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/gpt")
@Slf4j
public class GptController {

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private GptConfig gptConfig;

  @GetMapping("/chat")
  public String chat(@RequestParam(name="filePath") String filePath, @RequestParam(name="relation") String relation,
                     @RequestParam(name="sex") String sex, @RequestParam(name="theme") String theme) {

    String prompt = generatePrompt(filePath, relation, sex, theme);
    GptRequestDTO request = new GptRequestDTO(gptConfig.getModel(), prompt);
    GptResponseDTO chatGPTResponse = restTemplate.postForObject(gptConfig.getApiURL(), request, GptResponseDTO.class);
    return chatGPTResponse.getChoices().get(0).getMessage().getContent();
  }

  private String generatePrompt(String filePath, String relation, String sex, String theme) {
    // 파일에서 메시지 읽기
    String message = readFile(filePath);

    // 조건에 맞는 함수 선택
    if ("couple".equals(relation) && "male".equals(sex) && theme.matches("birth|anniversary|thank|celebration")) {
      return extractKeywordsAndReasonsCoupleMan(filePath, theme, message);
    } else if ("couple".equals(relation) && "female".equals(sex) && theme.matches("birth|anniversary|thank|celebration")) {
      return extractKeywordsAndReasonsCoupleWoman(filePath, theme, message);
    } else if ("parent".equals(relation) && theme.matches("birth|anniversary|thank|celebration")) {
      return extractKeywordsAndReasonsParents(filePath, theme, message);
    } else if ("friend".equals(relation) && theme.matches("birth|anniversary|thank|celebration")) {
      return extractKeywordsAndReasonsFriend(filePath, theme, message);
    } else if ("housewarming".equals(theme)) {
      return extractKeywordsAndReasonsHousewarming(filePath, message);
    } else if ("valentine".equals(theme)) {
      return extractKeywordsAndReasonsSeasonal(filePath, message);
    } else {
      return "조건에 맞는 선물 추천 기능이 없습니다.";
    }
  }

  private String generateText(String prompt) {
    GptRequestDTO request = new GptRequestDTO(gptConfig.getModel(), prompt);
    try {
      // API 요청 보내기
      GptResponseDTO response = restTemplate.postForObject(gptConfig.getApiURL(), request, GptResponseDTO.class);

      // API 응답에서 메시지 내용 추출
      if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
        return response.getChoices().get(0).getMessage().getContent();
      } else {
        return "GPT 응답에 오류가 있습니다.";
      }
    } catch (Exception e) {
      log.error("GPT 요청 중 오류 발생: ", e);
      return "GPT 요청 중 오류가 발생했습니다.";
    }
  }


  private String extractKeywordsAndReasonsCoupleMan(String filePath, String theme, String message) {
    // GPT 모델을 통해 관심 분야 및 근거 추출
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

  private String extractKeywordsAndReasonsCoupleWoman(String filePath, String theme, String message) {
    // GPT 모델을 통해 관심 분야 및 근거 추출
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

  private String extractKeywordsAndReasonsParents(String filePath, String theme, String message) {
    // GPT 모델을 통해 관심 분야 및 근거 추출
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

  private String extractKeywordsAndReasonsFriend(String filePath, String theme, String message) {
    // GPT 모델을 통해 관심 분야 및 근거 추출
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

  private String extractKeywordsAndReasonsHousewarming(String filePath, String message) {
    // GPT 모델을 통해 관심 분야 및 근거 추출
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

  private String extractKeywordsAndReasonsSeasonal(String filePath, String message) {
    // GPT 모델을 통해 관심 분야 및 근거 추출
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
    """, message);

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
