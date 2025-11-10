package com.project.accountapi;

import com.project.accountapi.domain.User;
import com.project.accountapi.domain.UserStatus;
import com.project.accountapi.repository.UserRepository;
import com.project.accountapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    private User createTestUser(String userId, String username, String email) {
        return User.builder()
                .userId(userId) // String 타입 사용
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
        String mockUserId = "UUID-001"; // String ID 사용

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(false);

        // Mock save 시 반환할 User 객체 설정
        User savedUser = createTestUser(mockUserId, username, email);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        // UserService의 registerUser 반환 타입이 String으로 변경되었다고 가정
        String userId = userService.registerUser(username, "password123", email);

        // Then
        assertNotNull(userId);
        assertEquals(mockUserId, userId); // String ID 검증
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
        User user = createTestUser("UUID-002", username, "auth@example.com"); // String ID 사용
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
        User user = createTestUser("UUID-002", username, "auth@example.com"); // String ID 사용
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
        String userId = "UUID-003"; // String ID 사용
        User user = createTestUser(userId, "statususer", "status@example.com");
        // UserRepository의 findById 인자가 String으로 변경되었다고 가정
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        userService.updateUserStatus(userId, UserStatus.DORMANT);

        // Then
        assertEquals(UserStatus.DORMANT, user.getStatus());
        verify(userRepository).findById(userId);
    }

    // --- 4. 휴면 해제(Reactivation) 테스트 ---

    @Test
    @DisplayName("인증 성공 - 휴면 계정 자동 해제")
    void authenticate_Success_ReactivateDormantUser() {
        // Given
        String username = "dormantuser";
        String rawPassword = "validpassword";
        User user = createTestUser("UUID-004", username, "dormant@example.com"); // String ID 사용

        // 1. User 객체를 DORMANT 상태로 변경
        user.updateStatus(UserStatus.DORMANT);
        // lastLoginAt 필드 초기화 (휴면 처리되었다고 가정)
        ReflectionTestUtils.setField(user, "lastLoginAt", java.time.LocalDateTime.now().minusYears(2));

        // 2. 비밀번호 설정
        ReflectionTestUtils.setField(user, "password", passwordEncoder.encode(rawPassword));

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // When
        User authenticatedUser = userService.authenticate(username, rawPassword);

        // Then
        assertNotNull(authenticatedUser);
        // 🔥 상태가 REGISTERED로 자동 변경되었는지 확인
        assertEquals(UserStatus.REGISTERED, authenticatedUser.getStatus(), "휴면 계정은 로그인 시 REGISTERED로 해제되어야 합니다.");
        // 🔥 lastLoginAt이 갱신되었는지 확인 (현재 시간과 거의 일치해야 함)
        assertTrue(authenticatedUser.getLastLoginAt().isAfter(java.time.LocalDateTime.now().minusSeconds(5)),
                "lastLoginAt이 갱신되어야 합니다.");
    }

    @Test
    @DisplayName("인증 실패 - 탈퇴 계정 접근")
    void authenticate_Fail_WithdrawnUser() {
        // Given
        String username = "withdrawnuser";
        String rawPassword = "validpassword";
        User user = createTestUser("UUID-005", username, "withdrawn@example.com"); // String ID 사용
        user.updateStatus(UserStatus.WITHDRAWN); // 탈퇴 상태로 변경

        ReflectionTestUtils.setField(user, "password", passwordEncoder.encode(rawPassword));

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            userService.authenticate(username, rawPassword);
        }, "탈퇴한 계정은 접근 시도 시 예외가 발생해야 합니다.");
    }

    // --- 5. 자동 휴면 대상 조회 테스트 (Scheduler 로직) ---

    @Test
    @DisplayName("휴면 대상 조회 성공")
    void findDormancyTargets_Success() {
        // Given
        java.time.LocalDateTime threshold = java.time.LocalDateTime.now().minusYears(1);

        // 1. Mock 데이터 생성: 2명의 휴면 대상 (REGISTERED 상태)
        User target1 = createTestUser("UUID-006", "olduser1", "old1@test.com"); // String ID 사용
        User target2 = createTestUser("UUID-007", "olduser2", "old2@test.com"); // String ID 사용

        List<User> mockTargets = List.of(target1, target2);

        // 2. Repository가 쿼리 조건에 맞는 데이터를 반환하도록 Mocking
        when(userRepository.findByLastLoginAtBeforeAndStatus(any(java.time.LocalDateTime.class), any(UserStatus.class)))
                .thenReturn(mockTargets);

        // When
        List<User> targets = userService.findDormancyTargets(threshold, UserStatus.REGISTERED);

        // Then
        // 쿼리 메서드가 올바른 인자(threshold, REGISTERED)로 호출되었는지 확인
        verify(userRepository).findByLastLoginAtBeforeAndStatus(any(java.time.LocalDateTime.class), eq(UserStatus.REGISTERED));
        // 반환된 리스트의 크기가 예상과 일치하는지 확인
        assertEquals(2, targets.size());
    }

    // --- 6. 강제 휴면 전환 (Scheduler) 테스트 ---

    @Test
    @DisplayName("휴면 전환 성공 - REGISTERED -> DORMANT")
    void convertToDormant_Success() {
        // Given
        User user = createTestUser("UUID-008", "toDormant", "toDormant@test.com"); // String ID 사용

        // When
        userService.convertToDormant(user);

        // Then
        assertEquals(UserStatus.DORMANT, user.getStatus());
    }

    @Test
    @DisplayName("휴면 전환 시도 실패 - 이미 WITHDRAWN 상태인 경우")
    void convertToDormant_Fail_AlreadyWithdrawn() {
        // Given
        User user = createTestUser("UUID-009", "alreadyWithdrawn", "withdrawn@test.com"); // String ID 사용
        user.updateStatus(UserStatus.WITHDRAWN); // 이미 탈퇴 상태

        // When
        userService.convertToDormant(user);

        // Then
        // 상태가 변경되지 않고 WITHDRAWN으로 유지되는지 확인
        assertEquals(UserStatus.WITHDRAWN, user.getStatus());
    }
}