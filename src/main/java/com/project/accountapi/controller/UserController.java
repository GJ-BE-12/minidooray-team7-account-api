package com.project.accountapi.controller;

import com.project.accountapi.domain.User;
import com.project.accountapi.dto.LoginRequest;
import com.project.accountapi.dto.RegisterRequest;
import com.project.accountapi.dto.UpdateStatusRequest;
import com.project.accountapi.dto.UserAuthResponse;
import com.project.accountapi.dto.UserResponse;
import com.project.accountapi.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody RegisterRequest registerRequest) {
        User user = userService.createUser(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @GetMapping("/{username}/auth")
    public ResponseEntity<UserAuthResponse> getUserAuthDetails(@PathVariable String username) {
        try {
            User user = userService.loadUserByUsernameForAuth(username);
            return ResponseEntity.ok(UserAuthResponse.from(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long userId) {
        UserResponse userResponse = userService.getUser(userId);
        return ResponseEntity.ok(userResponse);
    }

    @PutMapping("/{userId}/status")
    public ResponseEntity<Void> updateUserStatus(@PathVariable Long userId, @RequestBody UpdateStatusRequest updateStatusRequest) {
        userService.updateUserStatus(userId, updateStatusRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        User user = userService.login(loginRequest);
        return ResponseEntity.ok(UserResponse.from(user));
    }
}