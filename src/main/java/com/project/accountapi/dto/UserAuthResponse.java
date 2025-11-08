package com.project.accountapi.dto;

import com.project.accountapi.domain.User;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 게이트웨이의 CustomUserDetailsService로 인증 정보를 전송하기 위한 DTO
 */
@Data
@AllArgsConstructor
public class UserAuthResponse {
    private Long memberId;
    private String username;
    private String password;
    private String email;
    private String authority; // "ROLE_USER"

    public static UserAuthResponse from(User user) {
        return new UserAuthResponse(
                user.getUserId(),
                user.getUsername(),
                user.getPassword(),
                user.getEmail(),
                "ROLE_USER" // 간단한 권한 부여
        );
    }
}