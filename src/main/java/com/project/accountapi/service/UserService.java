package com.project.accountapi.service;

import com.project.accountapi.domain.User;
import com.project.accountapi.domain.UserStatus;
import com.project.accountapi.dto.RegisterRequest;
import com.project.accountapi.dto.UpdateStatusRequest;
import com.project.accountapi.dto.UserRequest;
import com.project.accountapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;

    public boolean exist(String userId){
        return userRepository.existsUserByUserId(userId);
    }

    @Transactional
    public String registerUser(RegisterRequest request) {
        // ID 중복 체크
        if (exist(request.getUserId())) {
            throw new IllegalArgumentException("이미 존재하는 사용자 ID입니다.");
        }

        User newUser = User.builder()
                .username(request.getUserId())
                .password(request.getPassword())
                .email(request.getEmail())
                .status(UserStatus.REGISTERED)
                .build();

        return userRepository.save(newUser).getUserId();
    }

    @Transactional
    public User getUser(String userId) {
        if(!exist(userId))
            throw new IllegalArgumentException("존재하지 않는 사용자 ID입니다.");
        return userRepository.findUserByUserId(userId);
    }


    @Transactional
    public void updateUserStatus(UpdateStatusRequest request) {
        User user = getUser(request.getUserId());
        user.setStatus(request.getNewStatus());
    }

    @Transactional
    public void deleteUser(String userId){
        if(!exist(userId))
            throw new IllegalArgumentException("존재하지 않는 사용자 ID입니다.");
        userRepository.deleteUserByUserId(userId);
    }
}
