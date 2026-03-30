package com.moveon.assistant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 问答审计日志实体
 */
@Entity
@Table(name = "qa_logs", indexes = {
        @Index(name = "idx_qa_logs_user_id", columnList = "userId"),
        @Index(name = "idx_qa_logs_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QaLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "question", nullable = false, length = 1000)
    private String question;

    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;

    @Column(name = "answer_truncated", nullable = false)
    @Builder.Default
    private Boolean answerTruncated = false;

    @Column(name = "hit_document_ids", length = 500)
    private String hitDocumentIds;

    @Column(name = "hit_fragment_ids", length = 500)
    private String hitFragmentIds;

    @Column(name = "hit_count", nullable = false)
    @Builder.Default
    private Integer hitCount = 0;

    @Column(name = "top_score")
    private Double topScore;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "SUCCESS";

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
