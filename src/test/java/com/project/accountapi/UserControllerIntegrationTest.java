package com.project.accountapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.accountapi.domain.User;
import com.project.accountapi.domain.UserStatus;
import com.project.accountapi.dto.LoginRequest;
import com.project.accountapi.dto.RegisterRequest;
import com.project.accountapi.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserController의 통합 테스트.
 * Controller -> Service -> Repository -> DB (H2)까지 전체 흐름을 검증합니다.
 */
@SpringBootTest // 전체 애플리케이션 컨텍스트 로드
@AutoConfigureMockMvc // MockMvc 자동 구성
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository; // 실제 DB 접근 확인용

    private final String BASE_URL = "/users";

    @AfterEach
    void tearDown() {
        // 테스트 간 독립성을 위해 DB 데이터 정리
        userRepository.deleteAll();
    }

    // --- 1. 회원 가입 통합 테스트 ---

    @Test
    @DisplayName("통합 테스트: 회원 가입 성공 시 DB에 사용자 정보가 저장된다.")
    void register_Integration_Success() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest("integrationUser", "pass456", "integration@mail.com");

        // When
        mockMvc.perform(post(BASE_URL + "/register")
                        // 🔥 [추가] Mock User를 주입하여 Security 통과
                        .with(user("test").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()); // 기대: 201 Created

        // Then: DB에 실제로 저장되었는지 확인
        User savedUser = userRepository.findByUsername(request.getUsername()).orElseThrow();
        assertThat(savedUser.getEmail()).isEqualTo(request.getEmail());
        // ... (후략)

        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.REGISTERED);
        // 암호화된 비밀번호는 평문과 당연히 다름
        assertThat(savedUser.getPassword()).isNotEqualTo(request.getPassword()); 
    }
    
    // --- 2. 로그인 통합 테스트 (인증 및 휴면 해제 로직 포함) ---

    @Test
    @DisplayName("통합 테스트: 휴면 계정 로그인 시 자동 해제 및 인증 성공")
    void authenticate_Integration_ReactivateDormant() throws Exception {
        // Given: 휴면 상태 사용자 DB에 미리 저장 (실제 Repository 사용)
        User dormantUser = User.builder()
                .userId("UUID-DORMANT-TEST")
                .username("dormantUser")
                // 비밀번호는 실제 Service가 사용하는 인코더로 인코딩되어야 함
                .password(passwordEncoder.encode("testpassword"))
                .email("dormant@integration.com")
                .status(UserStatus.DORMANT) // 휴면 상태
                .lastLoginAt(java.time.LocalDateTime.now().minusYears(2))
                .build();
        userRepository.save(dormantUser);

        LoginRequest request = new LoginRequest("dormantUser", "testpassword"); // 실제 비밀번호 사용

        // When: 로그인 API 호출
        mockMvc.perform(post(BASE_URL + "/authenticate")
                        // 🔥 [추가] Mock User를 주입하여 Security 필터 우회
                        .with(user("test").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("dormantUser"));
                
        // Then: Service를 거쳐 DB 상태가 REGISTERED로 바뀌었는지 확인
        User reactivatedUser = userRepository.findByUsername("dormantUser").orElseThrow();
        assertThat(reactivatedUser.getStatus()).isEqualTo(UserStatus.REGISTERED); // 상태 해제 확인
        assertThat(reactivatedUser.getLastLoginAt()).isAfter(java.time.LocalDateTime.now().minusSeconds(5)); // 시간 갱신 확인
    }
}