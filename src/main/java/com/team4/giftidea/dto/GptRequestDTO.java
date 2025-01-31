package com.team4.giftidea.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * GPT API 요청 데이터를 담는 DTO 클래스
 */
@Data
public class GptRequestDTO {

    /**
     * 사용할 GPT 모델
     */
    private String model;

    /**
     * GPT에 전달할 메시지 리스트
     */
    private List<MessageDTO> messages;

    /**
     * 생성자 - 주어진 모델과 프롬프트를 기반으로 GPT 요청을 생성합니다.
     *
     * @param model  사용할 GPT 모델
     * @param prompt 사용자 입력 프롬프트
     */
    public GptRequestDTO(String model, String prompt) {
        this.model = model;
        this.messages = new ArrayList<>();
        this.messages.add(new MessageDTO("user", prompt));
    }
}