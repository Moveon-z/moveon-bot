package com.moveon.infra.config;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 模型配置
 * - Embedding 模型：SiliconFlow BAAI/bge-m3（向量化）
 * - Chat 模型：可独立配置不同提供商（RAG 问答）
 */
@Slf4j
@Configuration
public class AiConfig {

    @Bean
    @ConditionalOnExpression("!'${ai.embedding.api-key:}'.isEmpty()")
    public OpenAiEmbeddingModel embeddingModel(
            @Value("${ai.embedding.api-key}") String apiKey,
            @Value("${ai.embedding.base-url:https://api.siliconflow.cn/v1}") String baseUrl,
            @Value("${ai.embedding.model:BAAI/bge-m3}") String modelName) {
        log.info("Initializing Embedding model: model={}, baseUrl={}", modelName, baseUrl);
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .build();
    }

    @Bean
    @ConditionalOnExpression("!'${ai.chat.api-key:}'.isEmpty()")
    public OpenAiChatModel chatModel(
            @Value("${ai.chat.api-key}") String apiKey,
            @Value("${ai.chat.base-url:https://api.siliconflow.cn/v1}") String baseUrl,
            @Value("${ai.chat.model:Qwen/Qwen3-8B}") String modelName) {
        log.info("Initializing Chat model: model={}, baseUrl={}", modelName, baseUrl);
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .build();
    }

    @Bean
    @ConditionalOnExpression("!'${ai.chat.api-key:}'.isEmpty()")
    public OpenAiStreamingChatModel streamingChatModel(
            @Value("${ai.chat.api-key}") String apiKey,
            @Value("${ai.chat.base-url:https://api.siliconflow.cn/v1}") String baseUrl,
            @Value("${ai.chat.model:Qwen/Qwen3-8B}") String modelName) {
        log.info("Initializing Streaming Chat model: model={}, baseUrl={}", modelName, baseUrl);
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .build();
    }
}
