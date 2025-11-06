package com.project.accountapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequest {
    // 요구사항: 회원의 상태(가입,탈퇴,휴면) 관리
    private String newStatus; // Enum으로 변환하기 전의 상태 문자열
}