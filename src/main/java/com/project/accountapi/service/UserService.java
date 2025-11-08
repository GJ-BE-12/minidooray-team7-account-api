package com.project.accountapi.service;

import com.project.accountapi.domain.User;
import com.project.accountapi.domain.UserStatus;
import com.project.accountapi.dto.LoginRequest;
import com.project.accountapi.dto.RegisterRequest;
import com.project.accountapi.dto.UpdateStatusRequest;
import com.project.accountapi.dto.UserResponse;
import com.project.accountapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User createUser(RegisterRequest registerRequest) {

        // 게이트웨이와 동일한 BCrypt로 비밀번호를 암호화하여 저장
        String encodedPassword = passwordEncoder.encode(registerRequest.getPassword());

        User user = User.builder()
                .username(registerRequest.getUsername())
                .password(encodedPassword) // 암호화된 비밀번호 저장
                .email(registerRequest.getEmail())
                .status(UserStatus.REGISTERED)
                .createdAt(LocalDateTime.now())
                .build();
        return userRepository.save(user);
    }

    /**
     * 게이트웨이의 CustomUserDetailsService가 호출할 메소드
     */
    @Transactional(readOnly = true)
    public User loadUserByUsernameForAuth(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }


    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return UserResponse.from(user);
    }

    @Transactional
    public void updateUserStatus(Long userId, UpdateStatusRequest updateStatusRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.updateStatus(UserStatus.valueOf(updateStatusRequest.getNewStatus()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User login(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // DB에 암호화된 비밀번호와 평문 비밀번호를 비교합니다.
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid password");
        }
        return user;
    }
}