package com.moveon.auth;

import com.moveon.auth.entity.User;
import com.moveon.auth.entity.UserRole;
import com.moveon.auth.entity.UserStatus;
import com.moveon.auth.repository.UserRepository;
import com.moveon.auth.service.AuthService;
import com.moveon.auth.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 认证服务测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void testLoadUserByUsername_UserNotFound() {
        assertThrows(
                org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> authService.loadUserByUsername("nonexistent")
        );
    }

    @Test
    void testLogin_Success() {
        // 创建测试用户
        User testUser = User.builder()
                .username("logintest")
                .password(passwordEncoder.encode("password123"))
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .build();
        userRepository.save(testUser);

        // 执行登录
        var response = authService.login("logintest", "password123");

        // 验证响应
        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("logintest", response.getUsername());
        assertEquals("USER", response.getRole());
    }

    @Test
    void testLogin_InvalidPassword() {
        // 创建测试用户
        User testUser = User.builder()
                .username("badpwtest")
                .password(passwordEncoder.encode("correctpassword"))
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .build();
        userRepository.save(testUser);

        // 验证错误密码会抛出异常
        assertThrows(
                IllegalArgumentException.class,
                () -> authService.login("badpwtest", "wrongpassword")
        );
    }

    @Test
    void testLogin_NonExistentUser() {
        assertThrows(
                IllegalArgumentException.class,
                () -> authService.login("nonexistent", "password")
        );
    }

    @Test
    void testRefreshToken_Success() throws InterruptedException {
        // 创建测试用户
        User testUser = User.builder()
                .username("refreshtest")
                .password(passwordEncoder.encode("password123"))
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .build();
        userRepository.save(testUser);

        // 先登录获取刷新令牌
        var loginResponse = authService.login("refreshtest", "password123");
        String refreshToken = loginResponse.getRefreshToken();

        // 等待 10ms 确保时间戳不同
        Thread.sleep(10);

        // 刷新访问令牌
        String newAccessToken = authService.refreshAccessToken(refreshToken);

        // 验证新的访问令牌
        assertNotNull(newAccessToken);
        // 验证新令牌是有效的
        assertTrue(jwtService.validateToken(newAccessToken, testUser));
    }

    @Test
    void testCreateUser() {
        // 创建用户
        var user = authService.createUser("newusertest", "password123", UserRole.USER);

        // 验证用户已创建
        assertNotNull(user);
        assertEquals("newusertest", user.getUsername());
        assertEquals(UserRole.USER, user.getRole());
        assertTrue(passwordEncoder.matches("password123", user.getPassword()));
    }

    @Test
    void testJwtTokenGenerationAndValidation() {
        // 创建测试用户
        User testUser = User.builder()
                .username("jwttest")
                .password(passwordEncoder.encode("password123"))
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .build();
        userRepository.save(testUser);

        // 生成令牌
        String token = jwtService.generateAccessToken(testUser);

        // 验证令牌
        assertNotNull(token);
        assertEquals("jwttest", jwtService.extractUsername(token));
        assertTrue(jwtService.validateToken(token, testUser));
    }
}
