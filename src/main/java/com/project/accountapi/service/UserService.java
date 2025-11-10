package com.project.accountapi.service;

import com.project.accountapi.domain.User;
import com.project.accountapi.domain.UserStatus;
import com.project.accountapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/**
 * Account-Api의 핵심 비즈니스 로직을 처리하는 서비스.
 * 회원가입, 인증, 회원정보 CRUD 및 상태 관리 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(); // 비밀번호 암호화

    // --- DTO (데이터 전송 객체)를 정의해야 하지만, 편의상 엔티티와 기본 타입을 사용합니다. ---

    /**
     * 1. 회원 가입 (ID/Email 중복 체크 포함)

     */
    @Transactional
    public String registerUser(String username, String password, String email) {
        // ID 중복 체크
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("이미 존재하는 사용자 ID입니다.");
        }
        // Email 중복 체크
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(password);

        User newUser = User.builder()
                .userId(UUID.randomUUID().toString())
                .username(username)
                .password(encodedPassword)
                .email(email)
                .status(UserStatus.REGISTERED)
                .build();

        return userRepository.save(newUser).getUserId();
    }

    /**
     * 2. ID/PW 인증 및 상태 확인 로직 (Gateway에서 호출)
     * 휴면 상태일 경우, 로그인 시 자동으로 정상 상태로 해제합니다.
     */
    @Transactional // 상태 변경 로직이 있으므로 트랜잭션 필요
    public User authenticate(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("사용자 ID를 찾을 수 없습니다."));

        // 1. 비밀번호 일치 여부 확인
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new NoSuchElementException("비밀번호가 일치하지 않습니다.");
        }

        // 2. 탈퇴 계정 확인
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new IllegalStateException("이미 탈퇴한 계정입니다.");
        }

        // 3. 🔥 핵심: 휴면 계정 확인 및 해제 (DORMANT -> REGISTERED)
        if (user.isDormant()) {
            user.reactivate(); // 휴면 해제 및 lastLoginAt 갱신
            System.out.println("✅ 휴면 계정 (" + username + ")이 성공적으로 해제되었습니다.");
        }

        // 4. 정상 계정인 경우, 마지막 로그인 시간만 갱신 (휴면 체크를 위한 데이터)
        else {
            user.updateLastLoginAt();
        }

        return user;
    }

    /**
     * 3. OAuth 인증 (Gateway에서 이메일로 사용자 조회)
     * @param email 사용자 이메일
     * @return Optional<User>
     */
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * 4. 회원 정보 조회 (Gateway/Task-Api에서 참조)
     */
    public User findUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("해당 ID의 사용자를 찾을 수 없습니다."));
    }

    /**
     * 5. 회원 상태 변경 (휴면/탈퇴)
     */
    @Transactional
    public void updateUserStatus(String userId, UserStatus newStatus) {
        User user = findUserById(userId);
        user.updateStatus(newStatus);
        // userRepository.save(user); // @Transactional 덕분에 자동 저장됨
    }

    /**
     * 🔥 [추가] 자동 휴면 처리를 위한 메서드 (DormancyScheduler에서 호출)
     * @param user 휴면 처리할 User 엔티티
     */
    @Transactional
    public void convertToDormant(User user) {
        // 이미 휴면 상태(DORMANT) 또는 탈퇴 상태(WITHDRAWN)가 아닌지 확인 후 진행
        if (user.getStatus() == UserStatus.REGISTERED) {
            user.updateStatus(UserStatus.DORMANT);
            // @Transactional 덕분에 별도의 save() 없이도 DB에 반영됩니다.
        } else {
            // 이미 휴면 상태이거나 탈퇴 상태인 경우, 로그를 남기거나 예외 처리를 할 수 있습니다.
            System.out.println("경고: 이미 DORMANT 또는 WITHDRAWN 상태인 사용자 ID: " + user.getUserId() + "에 대해 휴면 전환 시도가 있었습니다.");
        }
    }

    /**
     * 🔥 [추가] 자동 휴면 처리를 위해, 특정 시간 이전에 로그인했고, 상태가 REGISTERED인 사용자 목록 조회
     */
    public List<User> findDormancyTargets(LocalDateTime thresholdDate, UserStatus status) {
        return userRepository.findByLastLoginAtBeforeAndStatus(thresholdDate, status);
    }
}
