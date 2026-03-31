package com.moveon.notification.service;

import com.moveon.notification.entity.Reminder;
import com.moveon.notification.entity.ReminderStatus;
import com.moveon.notification.entity.ReminderType;
import com.moveon.notification.repository.ReminderRepository;
import com.moveon.task.entity.Task;
import com.moveon.task.entity.TaskPriority;
import com.moveon.task.entity.TaskStatus;
import com.moveon.task.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 任务提醒服务测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskReminderServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ReminderRepository reminderRepository;

    private TaskReminderService taskReminderService;

    @BeforeEach
    void setUp() {
        taskReminderService = new TaskReminderService(taskRepository, reminderRepository);
    }

    // ========== 提醒创建 ==========

    @Test
    void checkAndCreateReminders_NoUpcomingTasks() {
        when(taskRepository.findByStatusInAndDueDateBetween(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        int result = taskReminderService.checkAndCreateReminders();

        assertEquals(0, result);
        verify(reminderRepository, never()).save(any());
    }

    @Test
    void checkAndCreateReminders_CreatesDueSoonReminder() {
        LocalDateTime now = LocalDateTime.now();
        Task upcomingTask = Task.builder()
                .id(1L)
                .userId(1L)
                .title("即将到期任务")
                .priority(TaskPriority.HIGH)
                .status(TaskStatus.PENDING)
                .dueDate(now.plusMinutes(30))
                .build();

        when(taskRepository.findByStatusInAndDueDateBetween(any(), any(), any()))
                .thenReturn(List.of(upcomingTask))
                .thenReturn(Collections.emptyList());

        when(reminderRepository.existsByTaskIdAndRemindType(1L, ReminderType.DUE_SOON))
                .thenReturn(false);
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(inv -> inv.getArgument(0));

        int result = taskReminderService.checkAndCreateReminders();

        assertEquals(1, result);
        ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderRepository).save(captor.capture());
        Reminder saved = captor.getValue();
        assertEquals(1L, saved.getTaskId());
        assertEquals(1L, saved.getUserId());
        assertEquals(ReminderType.DUE_SOON, saved.getRemindType());
        assertEquals(ReminderStatus.PENDING, saved.getStatus());
        assertTrue(saved.getMessage().contains("即将到期"));
    }

    @Test
    void checkAndCreateReminders_SkipsExistingReminder() {
        LocalDateTime now = LocalDateTime.now();
        Task upcomingTask = Task.builder()
                .id(1L)
                .userId(1L)
                .title("已提醒任务")
                .status(TaskStatus.PENDING)
                .dueDate(now.plusMinutes(30))
                .build();

        when(taskRepository.findByStatusInAndDueDateBetween(any(), any(), any()))
                .thenReturn(List.of(upcomingTask))
                .thenReturn(Collections.emptyList());

        when(reminderRepository.existsByTaskIdAndRemindType(1L, ReminderType.DUE_SOON))
                .thenReturn(true);

        int result = taskReminderService.checkAndCreateReminders();

        assertEquals(0, result);
        verify(reminderRepository, never()).save(any());
    }

    @Test
    void checkAndCreateReminders_CreatesOverdueReminder() {
        LocalDateTime now = LocalDateTime.now();
        Task overdueTask = Task.builder()
                .id(2L)
                .userId(1L)
                .title("已过期任务")
                .status(TaskStatus.IN_PROGRESS)
                .dueDate(now.minusHours(2))
                .build();

        when(taskRepository.findByStatusInAndDueDateBetween(any(), any(), any()))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(overdueTask));

        when(reminderRepository.existsByTaskIdAndRemindType(2L, ReminderType.OVERDUE))
                .thenReturn(false);
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(inv -> inv.getArgument(0));

        int result = taskReminderService.checkAndCreateReminders();

        assertEquals(1, result);
        ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderRepository).save(captor.capture());
        assertEquals(ReminderType.OVERDUE, captor.getValue().getRemindType());
        assertTrue(captor.getValue().getMessage().contains("已过期"));
    }

    // ========== 提醒处理 ==========

    @Test
    void processPendingReminders_NoPending() {
        when(reminderRepository.findByStatusAndRemindAtBefore(eq(ReminderStatus.PENDING), any()))
                .thenReturn(Collections.emptyList());

        int result = taskReminderService.processPendingReminders();

        assertEquals(0, result);
        verify(reminderRepository, never()).saveAll(any());
    }

    @Test
    void processPendingReminders_MarksAsSent() {
        Reminder pending = Reminder.builder()
                .id(1L)
                .taskId(1L)
                .userId(1L)
                .title("测试提醒")
                .message("测试消息")
                .remindType(ReminderType.DUE_SOON)
                .status(ReminderStatus.PENDING)
                .remindAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(reminderRepository.findByStatusAndRemindAtBefore(eq(ReminderStatus.PENDING), any()))
                .thenReturn(List.of(pending));

        int result = taskReminderService.processPendingReminders();

        assertEquals(1, result);
        assertEquals(ReminderStatus.SENT, pending.getStatus());
        assertNotNull(pending.getSentAt());
        verify(reminderRepository).saveAll(any());
    }
}
