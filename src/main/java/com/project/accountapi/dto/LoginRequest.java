package com.project.accountapi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    // 요구사항: ID/PW 인증
    @NotBlank
    private String username;
    @NotBlank
    private String password;
}