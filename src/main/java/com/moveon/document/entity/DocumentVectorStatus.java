package com.moveon.document.entity;

/**
 * 向量记录状态枚举
 */
public enum DocumentVectorStatus {
    /**
     * 待生成 - 片段已解析，等待向量化
     */
    PENDING,

    /**
     * 已完成 - 向量已生成
     */
    COMPLETED,

    /**
     * 失败 - 向量生成失败
     */
    FAILED
}
