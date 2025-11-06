package com.project.accountapi.domain;

/**
 * Account-Api의 핵심 엔티티인 User 정의.
 * 사용자 계정 정보(ID, PW, Email) 및 상태를 관리하며, Task-Api에서 FK로 참조하는 기준이 됩니다.
 * DDL 이미지와 요구사항을 반영하여 username, password, email, created_at 등을 포함합니다.
 */

/** 사용자 상태: 가입(REGISTERED), 탈퇴(WITHDRAWN), 휴면(DORMANT) */
public enum UserStatus {
    REGISTERED, WITHDRAWN, DORMANT
}
