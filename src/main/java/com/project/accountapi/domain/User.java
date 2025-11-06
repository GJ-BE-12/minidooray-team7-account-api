package com.project.accountapi.domain;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;


@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "USER") // 'USER'는 MySQL 예약어일 수 있으므로 'USER_ACCOUNT' 사용
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId; // DDL의 user_id (PK)에 해당

    @Column(nullable = false, unique = true, length = 50)
    private String username; // DDL의 user_name (UNIQUE)

    @Column(nullable = false, length = 255) // DDL과 같이 Not Null
    private String password; // DDL의 password (암호화로 인해 length는 255로 조정)

    @Column(nullable = false, unique = true, length = 100)
    private String email; // DDL의 email (UNIQUE)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.REGISTERED; // 요구사항: 회원의 상태 관리

    @CreationTimestamp
    private LocalDateTime createdAt; // DDL의 created_at (자동 생성)

    @UpdateTimestamp
    private LocalDateTime updatedAt; // DDL에 없지만, 관례적으로 추가

    // 비밀번호 업데이트 메소드 (비즈니스 로직에 필요)
    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }

    // 상태 업데이트 메소드 (비즈니스 로직에 필요)
    public void updateStatus(UserStatus newStatus) {
        this.status = newStatus;
    }
}
