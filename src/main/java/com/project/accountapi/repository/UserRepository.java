package com.project.accountapi.repository;

import com.project.accountapi.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsUserByUserId(String userId);

    boolean existsUserByEmail(String email);

    User findUserByUserId(String userId);

    void deleteUserByUserId(String userId);
}
