package com.project.accountapi;

import com.project.accountapi.domain.User;
import com.project.accountapi.domain.UserStatus;
import com.project.accountapi.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UserRepository의 통합 테스트.
 * 실제 DB (In-memory H2 등)를 사용하여 JPA 쿼리 메서드의 동작을 검증합니다.
 */
@DataJpaTest // JPA 관련 설정만 로드하여 경량화된 테스트 환경 제공
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User activeUser;
    private User dormantUser;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 1: 정상 사용자 (최근 로그인)
        activeUser = User.builder()
                .userId("UUID-ACTIVE") // String ID
                .username("activeUser")
                .password("hashed_pw_1")
                .email("active@test.com")
                .status(UserStatus.REGISTERED)
                .lastLoginAt(LocalDateTime.now())
                .build();
        userRepository.save(activeUser);

        // 테스트 데이터 2: 휴면 대상 사용자 (오래 전 로그인)
        dormantUser = User.builder()
                .userId("UUID-DORMANT") // String ID
                .username("dormantUser")
                .password("hashed_pw_2")
                .email("dormant@test.com")
                .status(UserStatus.REGISTERED)
                .lastLoginAt(LocalDateTime.now().minusYears(2)) // 2년 전 로그인
                .build();
        userRepository.save(dormantUser);

        // 테스트 데이터 3: 이미 휴면 처리된 사용자
        User alreadyDormant = User.builder()
                .userId("UUID-ALREADY")
                .username("alreadyDormant")
                .password("hashed_pw_3")
                .email("already@test.com")
                .status(UserStatus.DORMANT) // 이미 DORMANT
                .lastLoginAt(LocalDateTime.now().minusYears(3))
                .build();
        userRepository.save(alreadyDormant);
    }

    @AfterEach
    void tearDown() {
        // 테스트 간 독립성을 보장하기 위해 데이터 초기화
        userRepository.deleteAll();
    }

    // --- 1. 기본 조회/존재 여부 테스트 (커버리지 필수 요소) ---

    @Test
    @DisplayName("1-1. findByUsername 성공 및 실패 테스트")
    void findByUsernameTest() {
        // 성공 케이스
        Optional<User> found = userRepository.findByUsername("activeUser");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("active@test.com");

        // 실패 (존재하지 않는 사용자) 케이스
        Optional<User> notFound = userRepository.findByUsername("nonExistent");
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("1-2. findByEmail 성공 및 실패 테스트")
    void findByEmailTest() {
        // 성공 케이스
        Optional<User> found = userRepository.findByEmail("dormant@test.com");
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("dormantUser");

        // 실패 (존재하지 않는 사용자) 케이스
        Optional<User> notFound = userRepository.findByEmail("none@test.com");
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("1-3. existsByUsername 및 existsByEmail 테스트 (Boolean 커버리지)")
    void existsByUsernameAndEmailTest() {
        assertAll(
                // Username 존재: true
                () -> assertTrue(userRepository.existsByUsername("activeUser")),
                // Username 미존재: false
                () -> assertFalse(userRepository.existsByUsername("nonUser")),
                // Email 존재: true
                () -> assertTrue(userRepository.existsByEmail("dormant@test.com")),
                // Email 미존재: false
                () -> assertFalse(userRepository.existsByEmail("nonEmail@test.com"))
        );
    }

    // --- 2. 휴면 처리 로직 핵심 테스트 (쿼리 메서드 브랜치 커버리지) ---

    @Test
    @DisplayName("2-1. findByLastLoginAtBeforeAndStatus_휴면_대상_조회_성공")
    void findDormancyTargets_SuccessTest() {
        // Given: 1년 전을 기준으로 설정
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);

        //  1년 전보다 로그인 시간이 이르고, 상태가 REGISTERED인 사용자 조회
        List<User> targets = userRepository.findByLastLoginAtBeforeAndStatus(oneYearAgo, UserStatus.REGISTERED);


        assertThat(targets).hasSize(1);
        assertThat(targets.get(0).getUsername()).isEqualTo("dormantUser");
        assertThat(targets).extracting(User::getUsername).doesNotContain("activeUser");
    }

    @Test
    @DisplayName("2-2. findByLastLoginAtBeforeAndStatus_최근_활동_사용자_제외_브랜치")
    void findDormancyTargets_ExcludeActiveUser() {
        // Given: 조회 기준 시간을 '1분 전'으로 설정합니다.
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);

        // When: 1분 전보다 로그인 시간이 이르고, 상태가 REGISTERED인 사용자 조회
        List<User> targets = userRepository.findByLastLoginAtBeforeAndStatus(oneMinuteAgo, UserStatus.REGISTERED);

        // 1. activeUser는 setUp에서 lastLoginAt이 now()로 설정되었기 때문에,
        //    '1분 전'보다 이후입니다. 따라서 제외

        assertThat(targets).hasSize(1);
        assertThat(targets).extracting(User::getUsername).containsExactly("dormantUser");
        assertThat(targets).extracting(User::getUsername).doesNotContain("activeUser");
    }

    @Test
    @DisplayName("2-3. findByLastLoginAtBeforeAndStatus_이미_DORMANT_상태인_사용자_제외_브랜치")
    void findDormancyTargets_ExcludeAlreadyDormantUser() {
        // Given: 1년 전을 기준으로 설정 (alreadyDormantUser는 3년 전 로그인)
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);

        // When: 상태가 REGISTERED인 사용자만 조회
        List<User> targets = userRepository.findByLastLoginAtBeforeAndStatus(oneYearAgo, UserStatus.REGISTERED);

        // Then
        // alreadyDormantUser는 REGISTERED가 아니므로 결과에 포함되지 않아야 함
        assertThat(targets).extracting(User::getUsername).doesNotContain("alreadyDormant");
    }
}