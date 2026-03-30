package com.moveon.auth.dto;

import com.moveon.auth.entity.UserRole;
import com.moveon.auth.entity.UserStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 用户响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String username;
    private UserStatus status;
    private UserRole role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
