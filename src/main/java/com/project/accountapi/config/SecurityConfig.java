// src/main/java/com/project/accountapi/config/SecurityConfig.java

package com.project.accountapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity; // Security 관련 설정을 위해 필요

// 🔥 이 클래스가 Spring 설정 클래스임을 명시합니다.
@Configuration 
@EnableWebSecurity // (선택) Spring Security를 활성화합니다.
public class SecurityConfig {

    /**
     * ✅ BCryptPasswordEncoder Bean 등록
     * 이 객체는 비밀번호 암호화 및 검증에 사용되며, UserService에서 @Autowired로 주입받아 사용됩니다.
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    // 이 외에 Spring Security의 필터 체인(인증/인가 규칙)을 정의하는 설정들이 추가됩니다.
    // (예: login 경로 permitAll 설정 등)
}