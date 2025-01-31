package com.team4.giftidea.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GPT API와의 메시지 전송을 위한 DTO 클래스
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {

    /**
     * 메시지의 역할 (예: "user", "assistant", "system")
     */
    private String role;

    /**
     * 메시지 내용
     */
    private String content;
}