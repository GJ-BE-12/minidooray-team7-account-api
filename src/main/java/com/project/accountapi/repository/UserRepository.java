package com.project.accountapi.repository;

import com.project.accountapi.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * ID/PW 인증을 위해 사용자 ID(username)로 사용자를 조회합니다.
     * @param username 사용자 ID (username)
     * @return Optional<User>
     */
    Optional<User> findByUsername(String username);

    /**
     * GitHub OAuth 인증 및 중복 회원 체크를 위해 이메일로 사용자를 조회합니다.
     * @param email 사용자 이메일
     * @return Optional<User>
     */
    Optional<User> findByEmail(String email);

    /**
     * 회원 가입 시 ID 중복을 확인합니다.
     * @param username 사용자 ID (username)
     * @return 중복 여부
     */
    boolean existsByUsername(String username);

    /**
     * 회원 가입 시 Email 중복을 확인합니다.
     * @param email 사용자 이메일
     * @return 중복 여부
     */
    boolean existsByEmail(String email);
}
