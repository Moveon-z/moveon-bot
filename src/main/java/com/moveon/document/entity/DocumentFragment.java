package com.moveon.document.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 文档片段实体类
 * 用于存储文档解析后的分段内容
 */
@Entity
@Table(name = "document_fragments", indexes = {
    @Index(name = "idx_fragments_document_id", columnList = "documentId"),
    @Index(name = "idx_fragments_document_id_index", columnList = "documentId, fragmentIndex")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentFragment {

    /**
     * 片段 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联文档 ID
     */
    @Column(name = "document_id", nullable = false)
    private Long documentId;

    /**
     * 片段序号（从 0 开始，用于按顺序重组原文）
     */
    @Column(name = "fragment_index", nullable = false)
    private Integer fragmentIndex;

    /**
     * 片段文本内容
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 字符数
     */
    @Column(name = "char_count", nullable = false)
    private Integer charCount;

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
