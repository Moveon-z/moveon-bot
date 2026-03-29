package com.moveon.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 语义检索结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    /**
     * 片段 ID
     */
    private Long fragmentId;

    /**
     * 文档 ID
     */
    private Long documentId;

    /**
     * 文档原始文件名
     */
    private String fileName;

    /**
     * 片段序号
     */
    private Integer fragmentIndex;

    /**
     * 片段内容
     */
    private String content;

    /**
     * 相似度分数（0~1，越接近 1 越相似）
     */
    private Double score;
}
