package com.moveon.document.service;

import com.moveon.document.entity.Document;
import com.moveon.document.entity.DocumentFragment;
import com.moveon.document.entity.DocumentStatus;
import com.moveon.document.repository.DocumentRepository;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档解析编排服务
 * 负责异步解析文档的完整流程：下载文件 → 提取文本 → 切分片段 → 更新状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentParsingService {

    private final DocumentRepository documentRepository;
    private final DocumentParserService parserService;
    private final MinioClient minioClient;
    private final DocumentEmbeddingService documentEmbeddingService;

    @Value("${minio.bucket:moveon-documents}")
    private String bucketName;

    /**
     * 异步解析文档
     * 状态流转：PENDING → PARSING → PARSED（或 FAILED）
     *
     * @param documentId 文档 ID
     */
    @Async
    public void parseDocumentAsync(Long documentId) {
        log.info("Starting async parsing for document {}", documentId);
        parseDocument(documentId);
    }

    /**
     * 同步解析文档（用于手动触发重新解析）
     *
     * @param documentId 文档 ID
     */
    @Transactional
    @Timed(value = "moveon.document.parsing", description = "Document parsing time")
    public void parseDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        // 检查状态：只有 PENDING 和 FAILED 状态可以触发解析
        if (document.getStatus() != DocumentStatus.PENDING
                && document.getStatus() != DocumentStatus.FAILED) {
            log.warn("Document {} is in status {}, skipping parsing", documentId, document.getStatus());
            return;
        }

        // 更新状态为 PARSING
        updateDocumentStatus(document, DocumentStatus.PARSING, null);

        try {
            // 1. 从 MinIO 下载文件
            byte[] fileData = downloadFileFromMinio(document.getStoragePath());
            log.info("Downloaded file for document {}: {} bytes", documentId, fileData.length);

            // 2. 提取文本
            String text = parserService.extractText(document, fileData);
            log.info("Extracted text from document {}: {} chars", documentId, text.length());

            // 3. 切分并保存片段
            List<DocumentFragment> fragments = parserService.splitAndSaveFragments(documentId, text);
            log.info("Document {} split into {} fragments", documentId, fragments.size());

            // 4. 更新状态为 PARSED
            updateDocumentStatus(document, DocumentStatus.PARSED, null);
            log.info("Document {} parsed successfully", documentId);

            // 5. 触发向量化
            documentEmbeddingService.embedDocumentAsync(documentId);

        } catch (Exception e) {
            log.error("Failed to parse document {}: {}", documentId, e.getMessage(), e);
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.length() > 500) {
                errorMessage = errorMessage.substring(0, 500);
            }
            updateDocumentStatus(document, DocumentStatus.FAILED, errorMessage);
        }
    }

    /**
     * 从 MinIO 下载文件
     */
    private byte[] downloadFileFromMinio(String storagePath) {
        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(storagePath)
                        .build())) {
            return inputStream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file from MinIO: " + storagePath, e);
        }
    }

    /**
     * 获取文档的所有片段
     */
    public List<DocumentFragment> getFragmentsByDocumentId(Long documentId) {
        return parserService.getFragmentsByDocumentId(documentId);
    }

    /**
     * 更新文档状态
     */
    private void updateDocumentStatus(Document document, DocumentStatus status, String errorMessage) {
        document.setStatus(status);
        document.setErrorMessage(errorMessage);
        if (status == DocumentStatus.PARSED) {
            document.setParsedAt(LocalDateTime.now());
        }
        documentRepository.save(document);
    }
}
