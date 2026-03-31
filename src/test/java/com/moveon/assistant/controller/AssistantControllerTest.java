package com.moveon.assistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveon.assistant.dto.AskRequest;
import com.moveon.assistant.dto.AskResponse;
import com.moveon.assistant.dto.Citation;
import com.moveon.assistant.service.RagService;
import com.moveon.auth.entity.User;
import com.moveon.auth.entity.UserRole;
import com.moveon.auth.entity.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AssistantController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {com.moveon.infra.config.SecurityConfig.class,
                        com.moveon.auth.config.JwtAuthenticationFilter.class})
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AssistantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RagService ragService;

    @Test
    void ask_success() throws Exception {
        AskResponse mockResponse = AskResponse.builder()
                .answer("这是测试答案")
                .citations(List.of(
                        Citation.builder().documentId(1L).fileName("test.pdf")
                                .fragmentIndex(0).score(0.9).build()
                ))
                .hitCount(1)
                .responseTimeMs(500L)
                .build();

        when(ragService.ask(eq(1L), eq("测试问题"), any())).thenReturn(mockResponse);

        AskRequest request = AskRequest.builder().question("测试问题").build();

        mockMvc.perform(post("/assistant/ask")
                        .requestAttr("user", createTestUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.answer").value("这是测试答案"))
                .andExpect(jsonPath("$.data.citations").isArray())
                .andExpect(jsonPath("$.data.citations[0].documentId").value(1))
                .andExpect(jsonPath("$.data.hitCount").value(1))
                .andExpect(jsonPath("$.data.responseTimeMs").value(500));
    }

    @Test
    void ask_emptyQuestion_returns400() throws Exception {
        AskRequest request = AskRequest.builder().question("").build();

        mockMvc.perform(post("/assistant/ask")
                        .requestAttr("user", createTestUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ask_tooLongQuestion_returns400() throws Exception {
        AskRequest request = AskRequest.builder()
                .question("a".repeat(1001))
                .build();

        mockMvc.perform(post("/assistant/ask")
                        .requestAttr("user", createTestUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private User createTestUser(Long id) {
        return User.builder()
                .id(id)
                .username("testuser")
                .password("encoded")
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .build();
    }
}
