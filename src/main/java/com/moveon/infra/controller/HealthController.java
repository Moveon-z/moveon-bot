package com.moveon.infra.controller;

import com.moveon.infra.dto.ApiResponse;
import com.moveon.infra.service.HealthCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller.
 */
@Slf4j
@RestController
@RequestMapping("/health")
@Tag(name = "Health", description = "Health check endpoints")
@RequiredArgsConstructor
public class HealthController {

    private final HealthCheckService healthCheckService;

    @GetMapping
    @Operation(summary = "Check application health status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now().toString());
        health.put("application", "moveon-bot");
        health.put("version", "0.0.1-SNAPSHOT");

        // Add dependency health checks
        Map<String, Object> dependencies = healthCheckService.checkHealth();
        health.put("dependencies", dependencies);

        // Calculate overall status
        boolean allUp = dependencies.values().stream()
                .allMatch(status -> {
                    if (status instanceof Map) {
                        return "UP".equals(((Map<?, ?>) status).get("status"));
                    }
                    return false;
                });

        if (!allUp) {
            health.put("status", "DEGRADED");
        }

        log.info("Health check completed: status={}", health.get("status"));

        return ResponseEntity.ok(ApiResponse.success(health));
    }

    @GetMapping("/detailed")
    @Operation(summary = "Get detailed health status with dependency checks")
    public ResponseEntity<ApiResponse<Map<String, Object>>> detailedHealth() {
        Map<String, Object> detailedHealth = new HashMap<>();
        detailedHealth.put("timestamp", Instant.now().toString());
        detailedHealth.put("application", "moveon-bot");
        detailedHealth.put("version", "0.0.1-SNAPSHOT");

        Map<String, Object> dependencies = healthCheckService.checkHealth();
        detailedHealth.put("dependencies", dependencies);

        // Determine overall status
        int upCount = 0;
        int downCount = 0;
        for (Object status : dependencies.values()) {
            if (status instanceof Map) {
                if ("UP".equals(((Map<?, ?>) status).get("status"))) {
                    upCount++;
                } else {
                    downCount++;
                }
            }
        }

        detailedHealth.put("summary", Map.of(
                "total", upCount + downCount,
                "up", upCount,
                "down", downCount
        ));

        if (downCount > 0) {
            detailedHealth.put("status", "DEGRADED");
        } else {
            detailedHealth.put("status", "HEALTHY");
        }

        log.info("Detailed health check completed: {}/{} dependencies UP", upCount, upCount + downCount);

        return ResponseEntity.ok(ApiResponse.success(detailedHealth));
    }
}
