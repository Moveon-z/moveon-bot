package com.moveon.notification.job;

import com.moveon.notification.service.TaskReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 任务提醒定时任务
 * 定期扫描即将到期和已过期的任务，生成提醒记录
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskReminderJob {

    private final TaskReminderService taskReminderService;

    /**
     * 每 5 分钟执行一次提醒检查
     * 扫描即将到期（1小时内）和已过期的任务
     */
    @Scheduled(fixedRate = 300000)
    public void checkTaskReminders() {
        try {
            log.debug("Starting task reminder check...");
            int created = taskReminderService.checkAndCreateReminders();
            int processed = taskReminderService.processPendingReminders();
            log.debug("Task reminder check completed: created={}, processed={}", created, processed);
        } catch (Exception e) {
            log.error("Task reminder check failed: {}", e.getMessage(), e);
        }
    }
}
