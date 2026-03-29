package com.moveon.document.repository;

import com.moveon.document.entity.DocumentVector;
import com.moveon.document.entity.DocumentVectorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档向量数据访问层
 */
@Repository
public interface DocumentVectorRepository extends JpaRepository<DocumentVector, Long> {

    /**
     * 按片段 ID 查询向量记录
     */
    DocumentVector findByFragmentId(Long fragmentId);

    /**
     * 按文档 ID 查询所有向量记录
     */
    List<DocumentVector> findByDocumentId(Long documentId);

    /**
     * 按文档 ID 和状态查询向量记录
     */
    List<DocumentVector> findByDocumentIdAndStatus(Long documentId, DocumentVectorStatus status);

    /**
     * 按用户 ID 和状态查询向量记录
     */
    List<DocumentVector> findByUserIdAndStatus(Long userId, DocumentVectorStatus status);

    /**
     * 按文档 ID 删除所有向量记录
     */
    void deleteByDocumentId(Long documentId);

    /**
     * 统计文档的向量记录数量
     */
    long countByDocumentId(Long documentId);

    /**
     * 统计指定状态的向量记录数量
     */
    long countByDocumentIdAndStatus(Long documentId, DocumentVectorStatus status);

    /**
     * 按文档 ID 查询待处理的向量记录
     */
    List<DocumentVector> findByDocumentIdAndStatusIn(Long documentId, List<DocumentVectorStatus> statuses);
}
