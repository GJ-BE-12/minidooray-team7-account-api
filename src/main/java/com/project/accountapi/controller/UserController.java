package com.project.accountapi.controller;

import com.project.accountapi.domain.User;
import com.project.accountapi.domain.UserStatus;
import com.project.accountapi.dto.LoginRequest;
import com.project.accountapi.dto.RegisterRequest;
import com.project.accountapi.dto.UpdateStatusRequest;
import com.project.accountapi.dto.UserResponse;
import com.project.accountapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

/**
 * Account-Api의 REST Controller.
 * Gateway의 RestTemplate 호출을 받아 회원 관리 및 인증 데이터 제공 API를 담당합니다.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // --- 1. 회원 가입 API ---
    // (RestApi) 회원의 상태(가입,탈퇴,휴면)를 관리(cud)합니다. -> C (Create)
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        try {
            String userId = userService.registerUser(
                    request.getUsername(),
                    request.getPassword(),
                    request.getEmail()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(userId);
        } catch (IllegalArgumentException e) {
            // ID 또는 Email 중복 시
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    // --- 2. 로그인 인증 API ---
    // 인증 데이터는 Account-Api를 사용합니다.
    @PostMapping("/authenticate")
    public ResponseEntity<UserResponse> authenticate(@RequestBody LoginRequest request) {
        try {
            // ID/PW 인증 수행
            User authenticatedUser = userService.authenticate(
                    request.getUsername(),
                    request.getPassword()
            );
            // 인증 성공 시 사용자 정보 반환
            return ResponseEntity.ok(UserResponse.from(authenticatedUser));
        } catch (NoSuchElementException e) {
            // 사용자 ID를 찾을 수 없거나 비밀번호 불일치
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    // --- 3. 이메일로 사용자 조회 API (OAuth 인증 시 Gateway에서 사용) ---
    @GetMapping("/email")
    public ResponseEntity<UserResponse> findByEmail(@RequestParam String email) {
        return userService.findUserByEmail(email)
                .map(UserResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // --- 4. 회원 정보 조회 API (Gateway/Task-Api에서 사용자 정보 조합 시 사용) ---
    // (RestApi)회원 정보를 제공합니다.
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserProfile(@PathVariable String userId) {
        try {
            User user = userService.findUserById(userId);
            return ResponseEntity.ok(UserResponse.from(user));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --- 5. 회원 상태 변경 API (휴면/탈퇴) ---
    // (RestApi)회원의 상태(가입,탈퇴,휴면)를 관리(cud)합니다. -> U (Update)
    @PatchMapping("/{userId}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable String userId,
                                             @RequestBody UpdateStatusRequest request) {
        try {
            UserStatus newStatus = UserStatus.valueOf(request.getNewStatus().toUpperCase());
            userService.updateUserStatus(userId, newStatus);
            return ResponseEntity.ok().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            // Enum 값(REGISTERED, WITHDRAWN, DORMANT)이 아닐 경우
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
