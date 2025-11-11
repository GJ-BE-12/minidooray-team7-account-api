package com.project.accountapi.dto;
import com.project.accountapi.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    // Gateway가 사용자 프로필 정보를 조합하는 데 사용할 응답
    private String userId;
    private String username;
    private String email;
    private String status;

    // Entity -> DTO 변환을 위한 정적 팩토리 메서드
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .status(user.ge tStatus().name())
                .build();
    }
}