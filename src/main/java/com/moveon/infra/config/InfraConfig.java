package com.moveon.infra.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Infrastructure configuration for MinIO and Redis.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class InfraConfig {

    @Value("${minio.endpoint:http://localhost:9000}")
    private String minioEndpoint;

    @Value("${minio.access-key:minioadmin}")
    private String minioAccessKey;

    @Value("${minio.secret-key:minioadmin}")
    private String minioSecretKey;

    /**
     * MinIO client bean.
     */
    @Bean
    public MinioClient minioClient() {
        log.info("Initializing MinIO client with endpoint: {}", minioEndpoint);
        return MinioClient.builder()
                .endpoint(minioEndpoint)
                .credentials(minioAccessKey, minioSecretKey)
                .build();
    }

    /**
     * Redis template bean for string operations.
     */
    @Bean
    public StringRedisTemplate redisTemplate(RedisConnectionFactory connectionFactory) {
        log.info("Initializing Redis StringTemplate");
        return new StringRedisTemplate(connectionFactory);
    }
}
