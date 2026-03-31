package com.moveon.task.entity;

/**
 * 任务状态枚举
 */
public enum TaskStatus {
    /**
     * 待处理 - 新创建的任务
     */
    PENDING,

    /**
     * 进行中 - 任务正在执行
     */
    IN_PROGRESS,

    /**
     * 已完成 - 任务已完成
     */
    COMPLETED,

    /**
     * 已取消 - 任务被取消
     */
    CANCELLED
}
