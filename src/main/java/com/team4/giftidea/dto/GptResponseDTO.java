package com.team4.giftidea.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GPT API의 응답 데이터를 담는 DTO 클래스
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GptResponseDTO {

    /**
     * GPT 응답의 선택지 리스트
     */
    private List<Choice> choices;

    /**
     * 개별 응답 선택지를 나타내는 내부 클래스
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {

        /**
         * 선택지 인덱스
         */
        private int index;

        /**
         * GPT가 생성한 메시지
         */
        private MessageDTO message;
    }
}