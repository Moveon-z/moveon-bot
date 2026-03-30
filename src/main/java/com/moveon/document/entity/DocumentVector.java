package com.moveon.document.entity;

import com.moveon.infra.config.VectorType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 文档向量实体类
 * 存储文档片段的向量嵌入，用于语义检索
 */
@Entity
@Table(name = "document_vectors", indexes = {
    @Index(name = "idx_vectors_fragment_id", columnList = "fragmentId"),
    @Index(name = "idx_vectors_document_id", columnList = "documentId"),
    @Index(name = "idx_vectors_user_id", columnList = "userId"),
    @Index(name = "idx_vectors_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVector {

    /**
     * 向量记录 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联片段 ID
     */
    @Column(name = "fragment_id", nullable = false)
    private Long fragmentId;

    /**
     * 关联文档 ID
     */
    @Column(name = "document_id", nullable = false)
    private Long documentId;

    /**
     * 归属用户 ID（用于检索时的用户隔离）
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 向量嵌入（1536 维，对应阿里云 Text Embedding 模型）
     */
    /**
     * 向量嵌入（1024 维，对应 BAAI/bge-m3 模型）
     */
    @Type(VectorType.class)
    @Column(name = "embedding", columnDefinition = "vector(1024)")
    private float[] embedding;

    /**
     * 向量化状态
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DocumentVectorStatus status = DocumentVectorStatus.PENDING;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
