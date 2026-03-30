package com.moveon.document.repository;

import com.moveon.document.entity.Document;
import com.moveon.document.entity.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档数据访问层
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * 分页查询用户文档列表
     */
    Page<Document> findByUserId(Long userId, Pageable pageable);

    /**
     * 查询用户的所有文档（按创建时间倒序）
     */
    List<Document> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 查询用户指定状态的文档（分页）
     */
    Page<Document> findByUserIdAndStatus(Long userId, DocumentStatus status, Pageable pageable);

    /**
     * 查询用户文档数量
     */
    long countByUserId(Long userId);

    /**
     * 检查用户是否存在指定文件名的文档
     */
    boolean existsByUserIdAndOriginalFilename(Long userId, String originalFilename);

    /**
     * 根据存储路径查询文档
     */
    @Query("SELECT d FROM Document d WHERE d.storagePath = :storagePath")
    Document findByStoragePath(@Param("storagePath") String storagePath);
}
