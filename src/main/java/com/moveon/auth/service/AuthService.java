package com.moveon.auth.service;

import com.moveon.auth.entity.User;
import com.moveon.auth.entity.UserRole;
import com.moveon.auth.entity.UserStatus;
import com.moveon.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 用户认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param rawPassword 明文密码
     * @return 登录响应（包含访问令牌和刷新令牌）
     */
    @Transactional(readOnly = true)
    public com.moveon.auth.dto.LoginResponse login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        // 验证密码
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        // 检查用户状态（不泄露具体原因，统一提示）
        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Login attempt for inactive/locked user: {}", username);
            throw new IllegalArgumentException("Invalid username or password");
        }

        // 生成令牌
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return com.moveon.auth.dto.LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }

    /**
     * 刷新访问令牌
     *
     * @param refreshToken 刷新令牌
     * @return 新的访问令牌
     */
    @Transactional(readOnly = true)
    public String refreshAccessToken(String refreshToken) {
        try {
            // 验证刷新令牌
            String username = jwtService.extractUsername(refreshToken);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

            // 验证令牌是否有效
            if (!jwtService.validateToken(refreshToken, user)) {
                throw new IllegalArgumentException("Invalid or expired refresh token");
            }

            // 生成新的访问令牌
            return jwtService.generateAccessToken(user);
        } catch (Exception e) {
            log.error("Failed to refresh access token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }
    }

    /**
     * 创建用户（管理员专属）
     */
    @Transactional
    public User createUser(String username, String rawPassword, UserRole role) {
        // 检查用户名是否存在
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        // 创建用户
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .status(UserStatus.ACTIVE)
                .role(role != null ? role : UserRole.USER)
                .build();

        return userRepository.save(user);
    }

    /**
     * 根据 ID 获取用户
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * 根据用户名获取用户
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
