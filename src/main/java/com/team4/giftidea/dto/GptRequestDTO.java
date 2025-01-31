package com.team4.giftidea.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GptRequestDTO {
    private String model;
    private List<MessageDTO> messages;

    public GptRequestDTO(String model, String prompt) {
        this.model = model;
        this.messages = new ArrayList<>();
        this.messages.add(new MessageDTO("user", prompt));
    }
}
