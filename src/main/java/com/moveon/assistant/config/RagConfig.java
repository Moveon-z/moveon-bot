package com.moveon.assistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "rag")
public class RagConfig {

    private int defaultTopK = 5;
    private int maxTopK = 20;
    private int maxQuestionLength = 1000;
    private int maxAnswerLength = 4000;
    private double minScoreThreshold = 0.3;
}
