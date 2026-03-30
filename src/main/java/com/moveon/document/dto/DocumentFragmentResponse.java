package com.moveon.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档片段响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentFragmentResponse {

    /**
     * 片段 ID
     */
    private Long id;

    /**
     * 文档 ID
     */
    private Long documentId;

    /**
     * 片段序号
     */
    private Integer fragmentIndex;

    /**
     * 片段内容
     */
    private String content;

    /**
     * 字符数
     */
    private Integer charCount;
}
