package com.project.accountapi.service;

import com.project.accountapi.domain.User;
import com.project.accountapi.domain.UserStatus;
import com.project.accountapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Optional;

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
     * @param username 계정 ID
     * @param password 비밀번호 (암호화 필요)
     * @param email 이메일
     * @return 저장된 User의 ID
     * @throws IllegalArgumentException 중복된 ID 또는 이메일이 존재할 경우 발생
     */
    @Transactional
    public Long registerUser(String username, String password, String email) {
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
                .username(username)
                .password(encodedPassword)
                .email(email)
                .status(UserStatus.REGISTERED)
                .build();

        return userRepository.save(newUser).getUserId();
    }

    /**
     * 2. ID/PW 인증 (Gateway에서 호출)
     * @param username 계정 ID
     * @param rawPassword 평문 비밀번호
     * @return 인증에 성공한 User 엔티티
     * @throws NoSuchElementException 사용자가 없거나, 비밀번호가 일치하지 않을 경우 발생
     */
    public User authenticate(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("사용자 ID를 찾을 수 없습니다."));

        // 비밀번호 일치 여부 확인
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new NoSuchElementException("비밀번호가 일치하지 않습니다.");
        }

        // 추가: 계정 상태가 활성(REGISTERED)인지 확인하는 로직 필요

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
     * @param userId 사용자 PK
     * @return User 엔티티
     * @throws NoSuchElementException 해당 ID의 사용자가 없을 경우 발생
     */
    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("해당 ID의 사용자를 찾을 수 없습니다."));
    }

    /**
     * 5. 회원 상태 변경 (휴면/탈퇴)
     * @param userId 사용자 PK
     * @param newStatus 변경할 상태 (휴면 또는 탈퇴)
     */
    @Transactional
    public void updateUserStatus(Long userId, UserStatus newStatus) {
        User user = findUserById(userId);
        user.updateStatus(newStatus);
        // userRepository.save(user); // @Transactional 덕분에 자동 저장됨
    }
}
