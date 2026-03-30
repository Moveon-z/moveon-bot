package com.moveon.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * 登录请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /**
     * 用户名
     */
    @NotBlank(message = "Username is required")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "Password is required")
    private String password;
}
