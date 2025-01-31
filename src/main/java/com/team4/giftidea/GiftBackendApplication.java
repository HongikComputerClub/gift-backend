package com.team4.giftidea;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * GiftBackendApplication - Spring Boot 메인 애플리케이션 클래스
 */
@SpringBootApplication
@EnableScheduling
public class GiftBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(GiftBackendApplication.class, args);
	}
}