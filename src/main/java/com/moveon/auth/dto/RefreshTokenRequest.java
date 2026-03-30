package com.moveon.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * 刷新令牌请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {

    /**
     * 刷新令牌
     */
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
