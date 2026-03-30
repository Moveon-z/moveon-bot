package com.moveon.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveon.auth.dto.CreateUserRequest;
import com.moveon.auth.dto.LoginRequest;
import com.moveon.auth.dto.LoginResponse;
import com.moveon.auth.entity.User;
import com.moveon.auth.entity.UserRole;
import com.moveon.auth.entity.UserStatus;
import com.moveon.auth.repository.UserRepository;
import com.moveon.infra.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证控制器测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    @Test
    void testControllerExists() {
        // 验证 AuthController 是否存在于应用上下文中
        String[] beans = applicationContext.getBeanNamesForType(com.moveon.auth.controller.AuthController.class);
        assertTrue(beans.length > 0, "AuthController should be registered as a bean");
    }

    @Test
    void testLogin_DirectServiceCall() throws Exception {
        // 直接通过服务层创建用户
        User testUser = User.builder()
                .username("directuser")
                .password(passwordEncoder.encode("password123"))
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .build();
        userRepository.save(testUser);

        // 验证用户可以被查询
        assertTrue(userRepository.existsByUsername("directuser"));
    }

    @Test
    void testLogin_Success() throws Exception {
        // 创建测试用户
        User testUser = User.builder()
                .username("testuser")
                .password(passwordEncoder.encode("password123"))
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .build();
        userRepository.save(testUser);

        // 执行登录
        LoginRequest request = new LoginRequest("testuser", "password123");
        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        ApiResponse<LoginResponse> response = objectMapper.readValue(
                responseJson,
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, LoginResponse.class)
        );

        // 验证响应
        assertNotNull(response.getData());
        assertNotNull(response.getData().getAccessToken());
        assertNotNull(response.getData().getRefreshToken());
        assertEquals("testuser", response.getData().getUsername());
        assertEquals("USER", response.getData().getRole());
    }

    @Test
    void testLogin_InvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("nonexistent", "wrongpassword");
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void testCreateUser_AndLogin() throws Exception {
        // 先创建管理员用户
        User admin = User.builder()
                .username("admin_test")
                .password(passwordEncoder.encode("admin123"))
                .status(UserStatus.ACTIVE)
                .role(UserRole.ADMIN)
                .build();
        userRepository.save(admin);

        // 管理员登录
        LoginRequest loginRequest = new LoginRequest("admin_test", "admin123");
        String loginJson = objectMapper.writeValueAsString(loginRequest);

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponseJson = loginResult.getResponse().getContentAsString();
        ApiResponse<LoginResponse> loginResponse = objectMapper.readValue(
                loginResponseJson,
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, LoginResponse.class)
        );

        String adminToken = loginResponse.getData().getAccessToken();

        // 使用管理员令牌创建新用户
        CreateUserRequest createRequest = new CreateUserRequest("newuser", "password123", "USER");
        String createJson = objectMapper.writeValueAsString(createRequest);

        mockMvc.perform(post("/auth/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // 验证用户已创建
        assertTrue(userRepository.existsByUsername("newuser"));
    }

    @Test
    void testGetCurrentUser() throws Exception {
        // 创建用户并登录
        User testUser = User.builder()
                .username("user_me")
                .password(passwordEncoder.encode("password123"))
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .build();
        userRepository.save(testUser);

        LoginRequest loginRequest = new LoginRequest("user_me", "password123");
        String loginJson = objectMapper.writeValueAsString(loginRequest);

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponseJson = loginResult.getResponse().getContentAsString();
        ApiResponse<LoginResponse> loginResponse = objectMapper.readValue(
                loginResponseJson,
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, LoginResponse.class)
        );

        String token = loginResponse.getData().getAccessToken();

        // 获取当前用户信息
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void testUnauthorizedAccess() throws Exception {
        // 不带令牌访问受保护接口
        // 测试配置排除了 Security 自动配置，行为可能是 403 或 500
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().is4xxClientError());
    }
}
