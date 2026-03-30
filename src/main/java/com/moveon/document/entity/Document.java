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
 * 文档实体类
 * 用于存储文档元数据信息
 */
@Entity
@Table(name = "documents", indexes = {
    @Index(name = "idx_documents_user_id", columnList = "userId"),
    @Index(name = "idx_documents_status", columnList = "status"),
    @Index(name = "idx_documents_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    /**
     * 文档 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 归属用户 ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 文件名
     */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /**
     * 原始文件名（用户上传时的文件名）
     */
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    /**
     * 文件类型
     */
    @Column(name = "file_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DocumentType fileType;

    /**
     * MIME 类型
     */
    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    /**
     * 文件大小（字节）
     */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /**
     * MinIO 存储路径
     */
    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    /**
     * 处理状态
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.PENDING;

    /**
     * 失败原因（处理失败时记录）
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /**
     * 文档摘要
     */
    @Column(name = "summary", length = 2000)
    private String summary;

    /**
     * 解析完成时间
     */
    @Column(name = "parsed_at")
    private LocalDateTime parsedAt;

    /**
     * 向量化完成时间
     */
    @Column(name = "embedded_at")
    private LocalDateTime embeddedAt;

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
