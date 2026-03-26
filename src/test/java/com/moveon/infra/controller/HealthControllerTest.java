package com.moveon.infra.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveon.infra.service.HealthCheckService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for HealthController.
 */
@WebMvcTest(HealthController.class)
@ActiveProfiles("test")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HealthCheckService healthCheckService;

    @Test
    void testHealthEndpoint() throws Exception {
        // Mock the health check service response
        Map<String, Object> mockHealth = Map.of(
                "database", Map.of("status", "UP"),
                "redis", Map.of("status", "UP"),
                "minio", Map.of("status", "UP")
        );
        when(healthCheckService.checkHealth()).thenReturn(mockHealth);

        mockMvc.perform(get("/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.application").value("moveon-bot"));
    }

    @Test
    void testDetailedHealthEndpoint() throws Exception {
        // Mock the health check service response
        Map<String, Object> mockHealth = Map.of(
                "database", Map.of("status", "UP"),
                "redis", Map.of("status", "UP"),
                "minio", Map.of("status", "UP")
        );
        when(healthCheckService.checkHealth()).thenReturn(mockHealth);

        mockMvc.perform(get("/health/detailed")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("HEALTHY"));
    }
}
