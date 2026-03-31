package com.moveon.notification.service;

import com.moveon.notification.entity.Reminder;
import com.moveon.notification.entity.ReminderStatus;
import com.moveon.notification.entity.ReminderType;
import com.moveon.notification.repository.ReminderRepository;
import com.moveon.task.entity.Task;
import com.moveon.task.entity.TaskStatus;
import com.moveon.task.repository.TaskRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 任务提醒服务
 * 扫描即将到期的任务并创建提醒记录
 */
@Slf4j
@Service
public class TaskReminderService {

    private final TaskRepository taskRepository;
    private final ReminderRepository reminderRepository;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    private static final int REMINDER_WINDOW_HOURS = 1;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public TaskReminderService(TaskRepository taskRepository, ReminderRepository reminderRepository) {
        this.taskRepository = taskRepository;
        this.reminderRepository = reminderRepository;
    }

    /**
     * 扫描即将到期的任务，创建提醒记录
     * 仅对状态为 PENDING 或 IN_PROGRESS 的任务生成提醒
     * 同一任务同一提醒类型不会重复创建
     */
    @Transactional
    public int checkAndCreateReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowEnd = now.plusHours(REMINDER_WINDOW_HOURS);

        List<TaskStatus> activeStatuses = List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS);

        int created = 0;

        // 检查即将到期的任务
        List<Task> upcomingTasks = taskRepository.findByStatusInAndDueDateBetween(activeStatuses, now, windowEnd);
        for (Task task : upcomingTasks) {
            if (!reminderRepository.existsByTaskIdAndRemindType(task.getId(), ReminderType.DUE_SOON)) {
                createReminder(task, ReminderType.DUE_SOON);
                created++;
            }
        }

        // 检查已过期任务
        List<Task> overdueTasks = taskRepository.findByStatusInAndDueDateBetween(activeStatuses,
                now.minusYears(1), now);
        for (Task task : overdueTasks) {
            if (!reminderRepository.existsByTaskIdAndRemindType(task.getId(), ReminderType.OVERDUE)) {
                createReminder(task, ReminderType.OVERDUE);
                created++;
            }
        }

        if (created > 0) {
            incrementCounter("moveon.reminders.created", created);
            log.info("Task reminder check completed: {} new reminders created", created);
        }
        return created;
    }

    /**
     * 处理待发送的提醒（将状态从 PENDING 更新为 SENT）
     * 当前为应用内记录方式，后续可扩展为邮件、推送等
     */
    @Transactional
    public int processPendingReminders() {
        List<Reminder> pendingReminders = reminderRepository.findByStatusAndRemindAtBefore(
                ReminderStatus.PENDING, LocalDateTime.now());

        for (Reminder reminder : pendingReminders) {
            reminder.setStatus(ReminderStatus.SENT);
            reminder.setSentAt(LocalDateTime.now());
            log.info("Reminder sent: taskId={}, userId={}, type={}, title={}",
                    reminder.getTaskId(), reminder.getUserId(),
                    reminder.getRemindType(), reminder.getTitle());
        }

        if (!pendingReminders.isEmpty()) {
            reminderRepository.saveAll(pendingReminders);
            log.info("Processed {} pending reminders", pendingReminders.size());
        }
        return pendingReminders.size();
    }

    private void createReminder(Task task, ReminderType type) {
        String message = buildMessage(task, type);

        Reminder reminder = Reminder.builder()
                .taskId(task.getId())
                .userId(task.getUserId())
                .title(task.getTitle())
                .message(message)
                .remindType(type)
                .status(ReminderStatus.PENDING)
                .remindAt(LocalDateTime.now())
                .build();

        reminderRepository.save(reminder);
        log.info("Reminder created: taskId={}, type={}, title={}", task.getId(), type, task.getTitle());
    }

    private String buildMessage(Task task, ReminderType type) {
        String dueDateStr = task.getDueDate() != null ? task.getDueDate().format(FORMATTER) : "无截止时间";
        return switch (type) {
            case DUE_SOON -> String.format("任务「%s」即将到期，截止时间：%s，请尽快处理。", task.getTitle(), dueDateStr);
            case OVERDUE -> String.format("任务「%s」已过期，截止时间：%s，请及时处理或调整截止时间。", task.getTitle(), dueDateStr);
        };
    }

    private void incrementCounter(String name, double amount) {
        if (meterRegistry != null) {
            meterRegistry.counter(name).increment(amount);
        }
    }
}
