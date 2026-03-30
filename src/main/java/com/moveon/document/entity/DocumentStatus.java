package com.moveon.document.entity;

/**
 * 文档处理状态枚举
 */
public enum DocumentStatus {
    /**
     * 待解析 - 文档已上传，等待解析
     */
    PENDING,

    /**
     * 解析中 - 正在解析文档内容
     */
    PARSING,

    /**
     * 已解析 - 文档内容已提取完成
     */
    PARSED,

    /**
     * 向量化中 - 正在生成向量
     */
    EMBEDDING,

    /**
     * 已完成 - 文档已完全处理（解析 + 向量化）
     */
    COMPLETED,

    /**
     * 失败 - 处理失败
     */
    FAILED
}
