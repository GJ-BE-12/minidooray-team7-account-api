//package com.project.accountapi;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.project.accountapi.controller.UserController;
//import com.project.accountapi.domain.User;
//import com.project.accountapi.domain.UserStatus;
//import com.project.accountapi.dto.UserRequest;
//import com.project.accountapi.dto.RegisterRequest;
//import com.project.accountapi.dto.UpdateStatusRequest;
//import com.project.accountapi.service.UserService;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.http.MediaType;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.util.NoSuchElementException;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.doNothing;
//import static org.mockito.Mockito.doThrow;
//import static org.mockito.Mockito.when;
//import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
///**
// * UserController의 API 테스트.
// * @WebMvcTest를 사용하여 Controller 레이어만 테스트하며, UserService는 Mocking합니다.
// */
//@WebMvcTest(UserController.class)
//public class UserControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc; // HTTP 요청 시뮬레이션 도구
//
//    @Autowired
//    private ObjectMapper objectMapper; // JSON 직렬화/역직렬화
//
//    @MockBean
//    private UserService userService; // Controller가 의존하는 Service는 MockBean으로 대체
//
//    private final String BASE_URL = "/users";
//
//    private User getMockUser(Long userId) {
//        return User.builder()
//                .userId(userId)
//                .username("testuser")
//                .email("test@example.com")
//                .status(UserStatus.REGISTERED)
//                .build();
//    }
//
//// --- 1. 회원 가입 API 테스트 (/users/register) ---
//
//    @Test
//    @WithMockUser
//    @DisplayName("회원 가입 성공 - HTTP 201 CREATED")
//    void register_Success() throws Exception {
//        // Given
//        RegisterRequest request = new RegisterRequest("newuser", "pass123", "new@mail.com");
//        when(userService.registerUser(any(), any(), any())).thenReturn(10L); // 성공 시 ID 반환
//
//        // When & Then
//        mockMvc.perform(post(BASE_URL + "/register").with(csrf())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated()) // 기대: 201 Created
//                .andExpect(content().string("10"));
//    }
//
//    @Test
//    @WithMockUser
//    @DisplayName("회원 가입 실패 - 중복 ID/Email - HTTP 400 BAD_REQUEST")
//    void register_Fail_Duplicate() throws Exception {
//        // Given
//        RegisterRequest request = new RegisterRequest("duplicate", "pass123", "dup@mail.com");
//        // 서비스에서 중복으로 인해 IllegalArgumentException 발생 시뮬레이션
//        when(userService.registerUser(any(), any(), any()))
//                .thenThrow(new IllegalArgumentException("중복"));
//
//        // When & Then
//        mockMvc.perform(post(BASE_URL + "/register").with(csrf())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isBadRequest()); // 기대: 400 Bad Request
//    }
//
//    // --- 2. 로그인 인증 API 테스트 (/users/authenticate) ---
//
//    @Test
//    @WithMockUser
//    @DisplayName("로그인 인증 성공 - HTTP 200 OK")
//    void authenticate_Success() throws Exception {
//        // Given
//        UserRequest request = new UserRequest("user", "pass");
//        User mockUser = getMockUser(1L);
//        when(userService.authenticate(any(), any())).thenReturn(mockUser);
//
//        // When & Then
//        mockMvc.perform(post(BASE_URL + "/authenticate").with(csrf())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.userId").value(1L))
//                .andExpect(jsonPath("$.username").value("testuser"));
//    }
//
//    @Test
//    @WithMockUser
//    @DisplayName("로그인 인증 실패 - 사용자 없음/PW 불일치 - HTTP 401 UNAUTHORIZED")
//    void authenticate_Fail() throws Exception {
//        // Given
//        UserRequest request = new UserRequest("unknown", "pass");
//        // 사용자 없거나 비밀번호 불일치 시 NoSuchElementException 발생 시뮬레이션
//        when(userService.authenticate(any(), any()))
//                .thenThrow(new NoSuchElementException("인증 실패"));
//
//        // When & Then
//        mockMvc.perform(post(BASE_URL + "/authenticate").with(csrf())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isUnauthorized()); // 기대: 401 Unauthorized
//    }
//
//    // --- 3. 회원 정보 조회 API 테스트 (/users/{userId}) ---
//
//    @Test
//    @WithMockUser
//    @DisplayName("회원 정보 조회 성공 - HTTP 200 OK")
//    void getUserProfile_Success() throws Exception {
//        // Given
//        Long userId = 5L;
//        User mockUser = getMockUser(userId);
//        when(userService.findUserById(userId)).thenReturn(mockUser);
//
//        // When & Then
//        mockMvc.perform(get(BASE_URL + "/" + userId))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.email").value("test@example.com"));
//    }
//
//    @Test
//    @WithMockUser
//    @DisplayName("회원 정보 조회 실패 - 사용자 없음 - HTTP 404 NOT_FOUND")
//    void getUserProfile_Fail_NotFound() throws Exception {
//        // Given
//        Long userId = 999L;
//        when(userService.findUserById(userId))
//                .thenThrow(new NoSuchElementException());
//
//        // When & Then
//        mockMvc.perform(get(BASE_URL + "/" + userId))
//                .andExpect(status().isNotFound()); // 기대: 404 Not Found
//    }
//
//    // --- 4. 상태 변경 API 테스트 (/users/{userId}/status) ---
//
//    @Test
//    @WithMockUser
//    @DisplayName("상태 변경 성공 - HTTP 200 OK")
//    void updateStatus_Success() throws Exception {
//        // Given
//        Long userId = 1L;
//        UpdateStatusRequest request = new UpdateStatusRequest("DORMANT");
//        doNothing().when(userService).updateUserStatus(eq(userId), eq(UserStatus.DORMANT));
//
//        // When & Then
//        mockMvc.perform(patch(BASE_URL + "/" + userId + "/status").with(csrf())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk());
//    }
//
//    // 이메일로 사용자 조회 API 테스트 추가 (Optional<User> 반환에 대한 테스트)
//    @Test
//    @WithMockUser
//    @DisplayName("이메일로 사용자 조회 성공 - HTTP 200 OK")
//    void findByEmail_Success() throws Exception {
//        // Given
//        String email = "find@email.com";
//        User mockUser = getMockUser(1L);
//        when(userService.findUserByEmail(email)).thenReturn(java.util.Optional.of(mockUser));
//
//        // When & Then
//        mockMvc.perform(get(BASE_URL + "/email")
//                        .param("email", email))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.email").value("test@example.com"));
//    }
//
//    @Test
//    @WithMockUser
//    @DisplayName("이메일로 사용자 조회 실패 - HTTP 404 NOT_FOUND")
//    void findByEmail_Fail() throws Exception {
//        // Given
//        String email = "notfound@email.com";
//        when(userService.findUserByEmail(email)).thenReturn(java.util.Optional.empty());
//
//        // When & Then
//        mockMvc.perform(get(BASE_URL + "/email")
//                        .param("email", email))
//                .andExpect(status().isNotFound());
//    }
//}