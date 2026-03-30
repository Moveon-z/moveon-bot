package com.moveon.document.repository;

import com.moveon.document.entity.DocumentFragment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档片段数据访问层
 */
@Repository
public interface DocumentFragmentRepository extends JpaRepository<DocumentFragment, Long> {

    /**
     * 按文档 ID 查询所有片段，按片段序号排序
     */
    List<DocumentFragment> findByDocumentIdOrderByFragmentIndexAsc(Long documentId);

    /**
     * 按文档 ID 删除所有片段
     */
    void deleteByDocumentId(Long documentId);

    /**
     * 统计文档的片段数量
     */
    long countByDocumentId(Long documentId);
}
