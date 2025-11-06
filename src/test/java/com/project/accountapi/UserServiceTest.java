package com.project.accountapi;

import com.project.accountapi.domain.User;
import com.project.accountapi.domain.UserStatus;
import com.project.accountapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.project.accountapi.service.UserService;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserService의 단위 테스트.
 * UserRepository를 Mocking하여 순수하게 Service 계층의 비즈니스 로직만 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        // UserService 내부에 final로 선언된 passwordEncoder 필드를 Reflection으로 주입
        ReflectionTestUtils.setField(userService, "passwordEncoder", passwordEncoder);
    }

    private User createTestUser(Long userId, String username, String email) {
        return User.builder()
                .userId(userId)
                .username(username)
                .password(passwordEncoder.encode("testpassword"))
                .email(email)
                .status(UserStatus.REGISTERED)
                .build();
    }

    // --- 1. 회원 가입 테스트 ---

    @Test
    @DisplayName("회원 가입 성공 테스트")
    void registerUser_Success() {
        // Given
        String username = "testuser";
        String email = "test@example.com";
        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(false);

        // Mock save 시 반환할 User 객체 설정
        User savedUser = createTestUser(1L, username, email);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        Long userId = userService.registerUser(username, "password123", email);

        // Then
        assertNotNull(userId);
        assertEquals(1L, userId);
        verify(userRepository).save(any(User.class)); // save 호출 확인
    }

    @Test
    @DisplayName("회원 가입 실패 - ID 중복")
    void registerUser_Fail_DuplicateUsername() {
        // Given
        String username = "testuser";
        when(userRepository.existsByUsername(username)).thenReturn(true);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(username, "password123", "test@example.com");
        }, "이미 존재하는 사용자 ID입니다. 예외 발생 확인");
    }

    // --- 2. 인증 테스트 ---

    @Test
    @DisplayName("인증 성공 테스트")
    void authenticate_Success() {
        // Given
        String username = "authuser";
        String rawPassword = "validpassword";
        User user = createTestUser(2L, username, "auth@example.com");
        // 테스트를 위해 평문 비밀번호를 암호화하여 User 객체에 설정
        ReflectionTestUtils.setField(user, "password", passwordEncoder.encode(rawPassword));

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // When
        User authenticatedUser = userService.authenticate(username, rawPassword);

        // Then
        assertNotNull(authenticatedUser);
        assertEquals(username, authenticatedUser.getUsername());
    }

    @Test
    @DisplayName("인증 실패 - 비밀번호 불일치")
    void authenticate_Fail_PasswordMismatch() {
        // Given
        String username = "authuser";
        String rawPassword = "wrongpassword";
        User user = createTestUser(2L, username, "auth@example.com");
        ReflectionTestUtils.setField(user, "password", passwordEncoder.encode("correctpassword"));

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // When & Then
        assertThrows(NoSuchElementException.class, () -> {
            userService.authenticate(username, rawPassword);
        }, "비밀번호가 일치하지 않습니다. 예외 발생 확인");
    }

    // --- 3. 상태 변경 테스트 ---

    @Test
    @DisplayName("회원 상태 변경 성공 - 휴면 처리")
    void updateUserStatus_Success() {
        // Given
        Long userId = 3L;
        User user = createTestUser(userId, "statususer", "status@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        userService.updateUserStatus(userId, UserStatus.DORMANT);

        // Then
        assertEquals(UserStatus.DORMANT, user.getStatus());
        verify(userRepository).findById(userId);
        // @Transactional에 의해 save는 묵시적으로 처리됨
    }
}
