package com.moveon.document.service;

import com.moveon.document.entity.Document;
import com.moveon.document.entity.DocumentFragment;
import com.moveon.document.entity.DocumentStatus;
import com.moveon.document.entity.DocumentVector;
import com.moveon.document.entity.DocumentVectorStatus;
import com.moveon.document.repository.DocumentFragmentRepository;
import com.moveon.document.repository.DocumentRepository;
import com.moveon.document.repository.DocumentVectorRepository;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档向量化编排服务
 * 负责异步向量化的完整流程：查询片段 → 生成向量 → 写入向量表 → 更新文档状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentEmbeddingService {

    private final DocumentRepository documentRepository;
    private final DocumentFragmentRepository fragmentRepository;
    private final DocumentVectorRepository documentVectorRepository;
    private final EmbeddingService embeddingService;

    /**
     * 异步向量化文档
     * 状态流转：PARSED → EMBEDDING → COMPLETED（或 FAILED）
     *
     * @param documentId 文档 ID
     */
    @Async
    public void embedDocumentAsync(Long documentId) {
        log.info("Starting async embedding for document {}", documentId);
        embedDocument(documentId);
    }

    /**
     * 同步向量化文档（用于手动触发重新向量化）
     */
    @Transactional
    @Timed(value = "moveon.document.embedding", description = "Document embedding time")
    public void embedDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        // 检查状态：只有 PARSED 状态可以触发向量化（重新向量化时允许 EMBEDDING/COMPLETED/FAILED）
        if (document.getStatus() == DocumentStatus.PENDING
                || document.getStatus() == DocumentStatus.PARSING) {
            log.warn("Document {} is in status {}, skipping embedding", documentId, document.getStatus());
            return;
        }

        // 检查 Embedding 模型是否可用
        if (!embeddingService.isAvailable()) {
            log.warn("Embedding model not available, skipping embedding for document {}", documentId);
            return;
        }

        // 更新状态为 EMBEDDING
        updateDocumentStatus(document, DocumentStatus.EMBEDDING, null);

        try {
            // 1. 获取文档的所有片段
            List<DocumentFragment> fragments = fragmentRepository.findByDocumentIdOrderByFragmentIndexAsc(documentId);
            if (fragments.isEmpty()) {
                throw new RuntimeException("No fragments found for document " + documentId);
            }
            log.info("Found {} fragments for document {}", fragments.size(), documentId);

            // 2. 清除旧的向量记录（重新向量化时）
            documentVectorRepository.deleteByDocumentId(documentId);

            // 3. 为每个片段生成向量
            int successCount = 0;
            for (DocumentFragment fragment : fragments) {
                try {
                    float[] embedding = embeddingService.embed(fragment.getContent());

                    DocumentVector vector = DocumentVector.builder()
                            .fragmentId(fragment.getId())
                            .documentId(documentId)
                            .userId(document.getUserId())
                            .embedding(embedding)
                            .status(DocumentVectorStatus.COMPLETED)
                            .build();
                    documentVectorRepository.save(vector);
                    successCount++;

                } catch (Exception e) {
                    log.error("Failed to embed fragment {} of document {}: {}",
                            fragment.getFragmentIndex(), documentId, e.getMessage());
                    // 记录失败的向量
                    DocumentVector failedVector = DocumentVector.builder()
                            .fragmentId(fragment.getId())
                            .documentId(documentId)
                            .userId(document.getUserId())
                            .status(DocumentVectorStatus.FAILED)
                            .build();
                    documentVectorRepository.save(failedVector);
                }
            }

            // 4. 更新文档状态
            long failedCount = fragments.size() - successCount;
            if (failedCount == 0) {
                updateDocumentStatus(document, DocumentStatus.COMPLETED, null);
                log.info("Document {} embedding completed: {}/{} fragments embedded",
                        documentId, successCount, fragments.size());
            } else {
                String errorMsg = String.format("部分片段向量化失败：成功 %d，失败 %d", successCount, failedCount);
                updateDocumentStatus(document, DocumentStatus.FAILED, errorMsg);
                log.warn("Document {} embedding partially failed: {}", documentId, errorMsg);
            }

        } catch (Exception e) {
            log.error("Failed to embed document {}: {}", documentId, e.getMessage(), e);
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.length() > 500) {
                errorMessage = errorMessage.substring(0, 500);
            }
            updateDocumentStatus(document, DocumentStatus.FAILED, errorMessage);
        }
    }

    /**
     * 更新文档状态
     */
    private void updateDocumentStatus(Document document, DocumentStatus status, String errorMessage) {
        document.setStatus(status);
        document.setErrorMessage(errorMessage);
        if (status == DocumentStatus.COMPLETED) {
            document.setEmbeddedAt(LocalDateTime.now());
        }
        documentRepository.save(document);
    }
}
