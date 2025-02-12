package com.team4.giftidea.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 및 CORS 설정을 담당하는 설정 클래스입니다.
 */
@Configuration
public class SecurityConfig {

    /**
     * HTTP 보안 설정을 구성하는 Bean입니다.
     * 
     * - CORS 설정 적용
     * - CSRF 보호 비활성화 (JWT 사용 시 필요)
     * - 특정 경로 보호 및 기본 요청 허용 설정
     *
     * @param http Spring Security의 HTTP 보안 설정 객체
     * @return SecurityFilterChain 보안 필터 체인
     * @throws Exception 설정 과정에서 발생할 수 있는 예외
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS 설정 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // CSRF 보호 비활성화 (JWT 인증을 사용하는 경우 필요)
                .csrf(csrf -> csrf.disable())

                // 접근 제어 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/**").authenticated() // "/admin/**" 경로는 인증 필요
                        .anyRequest().permitAll() // 나머지 요청은 인증 없이 허용
                );

        return http.build();
    }

    /**
     * CORS 설정을 구성하는 Bean입니다.
     * 
     * - 허용할 도메인(origin) 설정
     * - 허용할 HTTP 메서드(GET, POST 등) 지정
     * - 허용할 헤더 설정
     * - 쿠키 포함 요청 허용
     *
     * @return CorsConfigurationSource CORS 설정 객체
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 허용할 출처(Origin) 설정
        configuration.setAllowedOrigins(List.of("*"));
        
        // 허용할 HTTP 메서드 설정
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // 허용할 요청 헤더 설정
        configuration.setAllowedHeaders(List.of("*")); // 모든 헤더 허용

        // 쿠키 포함 요청 허용
        configuration.setAllowCredentials(true);

        // CORS 설정을 특정 경로에 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 모든 경로에 적용

        return source;
    }
}
