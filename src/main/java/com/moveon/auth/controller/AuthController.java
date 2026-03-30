package com.moveon.auth.controller;

import com.moveon.auth.dto.CreateUserRequest;
import com.moveon.auth.dto.LoginRequest;
import com.moveon.auth.dto.LoginResponse;
import com.moveon.auth.dto.RefreshTokenRequest;
import com.moveon.auth.dto.UserResponse;
import com.moveon.auth.entity.UserRole;
import com.moveon.auth.service.AuthService;
import com.moveon.auth.service.JwtService;
import com.moveon.infra.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "用户认证与授权 API")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用用户名和密码登录，返回访问令牌和刷新令牌")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 刷新访问令牌
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        String newAccessToken = authService.refreshAccessToken(request.getRefreshToken());

        // 从新令牌中提取用户信息
        String username = jwtService.extractUsername(newAccessToken);
        String role = authService.getUserByUsername(username)
                .map(user -> user.getRole().name())
                .orElse("USER");

        LoginResponse response = LoginResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .username(username)
                .role(role)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 创建用户（管理员专属）
     */
    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "创建用户", description = "管理员专属接口：创建新用户")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserRole role = null;
        if (request.getRole() != null && !request.getRole().isEmpty()) {
            try {
                role = UserRole.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid role: " + request.getRole());
            }
        }

        var user = authService.createUser(request.getUsername(), request.getPassword(), role);

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .status(user.getStatus())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();

        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "返回当前登录用户的信息")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@RequestAttribute("user") com.moveon.auth.entity.User user) {
        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .status(user.getStatus())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();

        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }
}
