package com.moveon.infra.config;

import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 模型配置
 * 配置 LangChain4j Embedding 模型（通过 OpenAI 兼容接口对接阿里云千问）
 */
@Slf4j
@Configuration
public class AiConfig {

    @Bean
    @ConditionalOnExpression("!'${ai.api-key:}'.isEmpty()")
    public OpenAiEmbeddingModel embeddingModel(
            @Value("${ai.api-key}") String apiKey,
            @Value("${ai.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}") String baseUrl,
            @Value("${ai.embedding-model:text-embedding-v2}") String modelName) {
        log.info("Initializing Embedding model: provider=aliyun, model={}, baseUrl={}", modelName, baseUrl);
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .build();
    }
}
