// src/main/java/com/project/accountapi/repository/UserRepository.java (수정된 코드)

package com.project.accountapi.repository;

import com.project.accountapi.domain.User;
import com.project.accountapi.domain.UserStatus; // UserStatus 임포트 필요
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime; // LocalDateTime 임포트 필요
import java.util.List; // List 임포트 필요
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * ID/PW 인증을 위해 사용자 ID(username)로 사용자를 조회합니다.
     */
    Optional<User> findByUsername(String username);

    /**
     * GitHub OAuth 인증 및 중복 회원 체크를 위해 이메일로 사용자를 조회합니다.
     */
    Optional<User> findByEmail(String email);

    /**
     * 회원 가입 시 ID 중복을 확인합니다.
     */
    boolean existsByUsername(String username);

    /**
     * 회원 가입 시 Email 중복을 확인합니다.
     */
    boolean existsByEmail(String email);

    /**
     * 자동 휴면 처리를 위해, 특정 시간 이전에 로그인했고, 
     * 상태가 REGISTERED인 사용자 목록을 조회합니다.
     * @return 휴면 대상 사용자 리스트
     */
    List<User> findByLastLoginAtBeforeAndStatus(LocalDateTime thresholdDate, UserStatus status);
}