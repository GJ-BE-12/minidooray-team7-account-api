package com.project.accountapi.dto;

import com.project.accountapi.domain.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequest {
    private String userId;
    private UserStatus newStatus;
}