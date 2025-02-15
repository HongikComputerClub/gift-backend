package com.team4.giftidea.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/proxy")
public class ProxyController {

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/kream")
    public ResponseEntity<byte[]> proxyToKream(@RequestParam String url) {
        // 이미지의 바이트 배열을 가져옵니다.
        byte[] imageBytes = restTemplate.getForObject(url, byte[].class);
        
        // Content-Type을 적절히 설정 (필요에 따라 "image/jpeg" 또는 "image/png" 등으로 조정)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(imageBytes);
    }
}
