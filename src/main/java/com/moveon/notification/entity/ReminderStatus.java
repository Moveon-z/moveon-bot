package com.moveon.notification.entity;

/**
 * 提醒状态枚举
 */
public enum ReminderStatus {
    /**
     * 待发送
     */
    PENDING,

    /**
     * 已发送
     */
    SENT,

    /**
     * 发送失败
     */
    FAILED
}
