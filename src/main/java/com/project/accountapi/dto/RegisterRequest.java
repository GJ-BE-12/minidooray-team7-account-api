package com.project.accountapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Account-Api 서비스에서 사용하는 DTO(Data Transfer Object) 정의.
 * API 요청(Request) 및 응답(Response) 시 데이터의 유효성 검증 및 전송을 담당합니다.
 */

// --- Request DTOs ---

@Data
@NoArgsConstructor
public class RegisterRequest {
    // 요구사항: 회원가입시 계정정보(id,email,password) 입력
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    @NotBlank
    @Email
    private String email;
}
