package com.project.accountapi.repository;

import com.project.accountapi.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT \n" +
            "    CASE WHEN COUNT(u) > 0 THEN TRUE ELSE FALSE END \n" +
            "FROM \n" +
            "    User u \n" +
            "WHERE \n" +
            "    u.userId = :userId")
    boolean existsUserByUserId(String userId);

    User findUserByUserId(String userId);

    void deleteUserByUserId(String userId);
}
