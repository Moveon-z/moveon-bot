package com.moveon.document.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 向量嵌入服务
 * 封装 LangChain4j Embedding 模型调用
 */
@Slf4j
@Service
public class EmbeddingService {

    @Autowired(required = false)
    private OpenAiEmbeddingModel embeddingModel;

    /**
     * 判断 Embedding 模型是否可用
     */
    public boolean isAvailable() {
        return embeddingModel != null;
    }

    /**
     * 将文本转为向量
     *
     * @param text 输入文本
     * @return 向量数组（1536 维）
     */
    public float[] embed(String text) {
        if (embeddingModel == null) {
            throw new IllegalStateException("Embedding 模型未配置，请设置 ai.api-key 环境变量");
        }

        log.debug("Generating embedding for text (length={})", text.length());
        Response<Embedding> response = embeddingModel.embed(text);
        float[] vector = response.content().vector();
        log.debug("Generated embedding: dimensions={}", vector.length);
        return vector;
    }
}
