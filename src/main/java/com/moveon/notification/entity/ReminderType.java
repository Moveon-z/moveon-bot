package com.moveon.notification.entity;

/**
 * 提醒类型枚举
 */
public enum ReminderType {
    /**
     * 即将到期提醒（截止时间前 1 小时）
     */
    DUE_SOON,

    /**
     * 已到期提醒
     */
    OVERDUE
}
