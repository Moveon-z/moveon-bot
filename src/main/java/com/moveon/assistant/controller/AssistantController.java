package com.moveon.assistant.controller;

import com.moveon.assistant.dto.AskRequest;
import com.moveon.assistant.dto.AskResponse;
import com.moveon.assistant.service.RagService;
import com.moveon.infra.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/assistant")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "智能问答", description = "基于 RAG 的文档问答接口")
public class AssistantController {

    private final RagService ragService;

    @PostMapping("/ask")
    @Operation(summary = "文档问答", description = "基于已上传文档进行 RAG 问答，返回答案及引用来源")
    public ResponseEntity<ApiResponse<AskResponse>> ask(
            @RequestAttribute("user") com.moveon.auth.entity.User user,
            @Valid @RequestBody AskRequest request) {

        log.info("Ask question: userId={}, questionLength={}", user.getId(),
                request.getQuestion().length());

        AskResponse response = ragService.ask(user.getId(), request.getQuestion(), request.getTopK());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式文档问答", description = "基于已上传文档进行 RAG 问答，逐 token 流式返回答案")
    public SseEmitter askStream(
            @RequestAttribute("user") com.moveon.auth.entity.User user,
            @Valid @RequestBody AskRequest request) {

        log.info("Stream ask: userId={}, questionLength={}", user.getId(),
                request.getQuestion().length());

        return ragService.askStream(user.getId(), request.getQuestion(), request.getTopK());
    }
}
