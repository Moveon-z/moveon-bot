package com.moveon.document.service;

import com.moveon.document.dto.SearchResult;
import com.moveon.document.entity.DocumentVector;
import com.moveon.document.entity.DocumentVectorStatus;
import com.moveon.document.repository.DocumentFragmentRepository;
import com.moveon.document.repository.DocumentVectorRepository;
import com.moveon.infra.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 语义检索服务
 * 基于 pgvector 的余弦相似度进行语义检索
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticSearchService {

    private final DocumentVectorRepository documentVectorRepository;
    private final DocumentFragmentRepository fragmentRepository;
    private final EmbeddingService embeddingService;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 语义检索：根据查询文本搜索最相关的文档片段
     *
     * @param query  查询文本
     * @param userId 用户 ID（用于用户隔离）
     * @param topK   返回最相关的 K 条结果
     * @return 检索结果列表，按相似度降序排列
     */
    public List<SearchResult> search(String query, Long userId, int topK) {
        if (query == null || query.isBlank()) {
            throw new BusinessException("INVALID_QUERY", "查询文本不能为空");
        }

        if (!embeddingService.isAvailable()) {
            throw new BusinessException("EMBEDDING_UNAVAILABLE", "向量检索服务暂不可用，请检查 AI 配置");
        }

        log.info("Semantic search: userId={}, query='{}', topK={}", userId,
                query.length() > 50 ? query.substring(0, 50) + "..." : query, topK);

        // 1. 将查询文本转为向量
        float[] queryVector = embeddingService.embed(query);

        // 2. 将向量转为 PGvector 格式的字符串
        StringBuilder vectorStr = new StringBuilder("[");
        for (int i = 0; i < queryVector.length; i++) {
            if (i > 0) vectorStr.append(",");
            vectorStr.append(queryVector[i]);
        }
        vectorStr.append("]");

        // 3. 使用原生 SQL 进行余弦距离检索
        String sql = """
                SELECT dv.fragment_id, dv.document_id, d.original_filename,
                       df.fragment_index, df.content,
                       1 - (dv.embedding <=> ?::vector) AS similarity
                FROM document_vectors dv
                JOIN document_fragments df ON dv.fragment_id = df.id
                JOIN documents d ON dv.document_id = d.id
                WHERE dv.user_id = ?
                  AND dv.status = 'COMPLETED'
                  AND dv.embedding IS NOT NULL
                ORDER BY dv.embedding <=> ?::vector
                LIMIT ?
                """;

        List<SearchResult> results = jdbcTemplate.query(sql,
                (rs, rowNum) -> SearchResult.builder()
                        .fragmentId(rs.getLong("fragment_id"))
                        .documentId(rs.getLong("document_id"))
                        .fileName(rs.getString("original_filename"))
                        .fragmentIndex(rs.getInt("fragment_index"))
                        .content(rs.getString("content"))
                        .score(rs.getDouble("similarity"))
                        .build(),
                vectorStr.toString(), userId, vectorStr.toString(), topK);

        log.info("Semantic search completed: found {} results for userId={}", results.size(), userId);
        return results;
    }

    /**
     * 检查用户是否有已向量化的文档
     */
    public boolean hasEmbeddedDocuments(Long userId) {
        long count = documentVectorRepository.findByUserIdAndStatus(userId, DocumentVectorStatus.COMPLETED).size();
        return count > 0;
    }
}
