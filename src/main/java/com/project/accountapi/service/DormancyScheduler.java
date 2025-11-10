package com.project.accountapi.service;

import com.project.accountapi.domain.User;
import com.project.accountapi.domain.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DormancyScheduler {

    // UserService를 통해 비즈니스 로직을 수행합니다.
    private final UserService userService; 
    
    /**
     * 🔥 [자동 휴면 전환] 
     * 매일 새벽 4시 0분 0초에 실행됩니다. (cron = "초 분 시 일 월 요일")
     * 1년 이상 미접속한 (lastLoginAt이 1년 전보다 오래된) 사용자를 휴면 상태로 전환합니다.
     */
    @Scheduled(cron = "0 0 4 * * *") 
    public void runDormancyCheck() {
        System.out.println("--- [Scheduler] 휴면 계정 전환 작업 시작: " + LocalDateTime.now() + " ---");
        
        // 1. 휴면 기준 정의 (현재 시간으로부터 1년 전)
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1); 
        
        // 2. 휴면 대상 사용자 조회 (lastLoginAt 기준 1년 초과, 상태 REGISTERED인 사용자)
        List<User> dormancyTargets = userService.findDormancyTargets(oneYearAgo, UserStatus.REGISTERED);
        
        if (dormancyTargets.isEmpty()) {
            System.out.println("휴면 처리 대상자가 없습니다.");
            return;
        }

        // 3. 대상 사용자들을 DORMANT로 전환
        for (User user : dormancyTargets) {
            // UserService의 상태 변경 로직을 사용하여 DORMANT로 변경
            userService.convertToDormant(user); 
            System.out.println("ID: " + user.getUserId() + ", " + user.getUsername() + " 계정을 휴면 처리했습니다.");
        }
        
        System.out.println("--- [Scheduler] 휴면 계정 전환 작업 완료: " + dormancyTargets.size() + "건 처리 ---");
    }
}