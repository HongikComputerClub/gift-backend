package com.team4.giftidea;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GiftBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(GiftBackendApplication.class, args);
	}

}
