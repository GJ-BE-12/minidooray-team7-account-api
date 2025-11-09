package com.project.accountapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Account-Api 서비스에서 사용하는 DTO(Data Transfer Object) 정의.
 * API 요청(Request) 및 응답(Response) 시 데이터의 유효성 검증 및 전송을 담당합니다.
 */

// --- Request DTOs ---

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String userId;
    private String username;
    private String password;
    private String email;
}
