package com.moveon.infra.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import io.minio.MinioClient;
import io.minio.BucketExistsArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check service for checking database, Redis, and MinIO connectivity.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckService {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final MinioClient minioClient;

    @Value("${minio.bucket:moveon-documents}")
    private String bucketName;

    /**
     * Check health status of all dependencies.
     *
     * @return health status map with details for each component
     */
    public Map<String, Object> checkHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("database", checkDatabase());
        health.put("redis", checkRedis());
        health.put("minio", checkMinio());
        return health;
    }

    /**
     * Check database connectivity.
     */
    private Map<String, Object> checkDatabase() {
        Map<String, Object> status = new HashMap<>();
        try {
            Long result = jdbcTemplate.queryForObject("SELECT 1", Long.class);
            status.put("status", "UP");
            status.put("message", "Database connection successful");
            log.debug("Database health check passed");
        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("message", "Database connection failed: " + e.getMessage());
            log.warn("Database health check failed: {}", e.getMessage());
        }
        return status;
    }

    /**
     * Check Redis connectivity.
     */
    private Map<String, Object> checkRedis() {
        Map<String, Object> status = new HashMap<>();
        try {
            redisTemplate.opsForValue().set("health_check_key", "health_check_value", 1);
            String value = redisTemplate.opsForValue().get("health_check_key");
            if ("health_check_value".equals(value)) {
                status.put("status", "UP");
                status.put("message", "Redis connection successful");
                log.debug("Redis health check passed");
            } else {
                status.put("status", "DOWN");
                status.put("message", "Redis read/write mismatch");
                log.warn("Redis health check failed: read/write mismatch");
            }
        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("message", "Redis connection failed: " + e.getMessage());
            log.warn("Redis health check failed: {}", e.getMessage());
        } finally {
            // Clean up test key
            try {
                redisTemplate.delete("health_check_key");
            } catch (Exception e) {
                log.debug("Failed to clean up health check key: {}", e.getMessage());
            }
        }
        return status;
    }

    /**
     * Check MinIO connectivity.
     */
    private Map<String, Object> checkMinio() {
        Map<String, Object> status = new HashMap<>();
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            status.put("status", "UP");
            status.put("message", "MinIO connection successful");
            status.put("bucket", bucketName);
            status.put("bucketExists", exists);
            log.debug("MinIO health check passed, bucket '{}' exists: {}", bucketName, exists);
        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("message", "MinIO connection failed: " + e.getMessage());
            log.warn("MinIO health check failed: {}", e.getMessage());
        }
        return status;
    }
}
