package com.team4.giftidea.controller;

import com.team4.giftidea.dto.GptRequestDTO;
import com.team4.giftidea.dto.GptResponseDTO;
import com.team4.giftidea.configuration.GptConfig;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/gpt")
@Slf4j
public class GptController {

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private GptConfig gptConfig;

  @GetMapping("/chat")
  public String chat(@RequestParam(name="prompt")String prompt) {
    GptRequestDTO request=new GptRequestDTO(gptConfig.model,prompt);
    GptResponseDTO chatGPTResponse = restTemplate.postForObject(gptConfig.apiURL, request, GptResponseDTO.class);
    return chatGPTResponse.getChoices().get(0).getMessage().getFormat();
  }
}