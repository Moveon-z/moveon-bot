package com.moveon.auth.service;

import com.moveon.auth.entity.User;
import com.moveon.auth.entity.UserRole;
import com.moveon.auth.entity.UserStatus;
import com.moveon.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 用户初始化服务
 * 确保初始管理员账号存在
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 检查是否已存在管理员账号
        if (!userRepository.existsByUsername("moveon")) {
            // 创建初始管理员账号
            User adminUser = User.builder()
                    .username("moveon")
                    .password(passwordEncoder.encode("moveon123"))
                    .status(UserStatus.ACTIVE)
                    .role(UserRole.ADMIN)
                    .build();

            userRepository.save(adminUser);
            log.info("Initial admin user created: username=moveon, password=moveon123");
        } else {
            log.info("Admin user already exists");
        }
    }
}
