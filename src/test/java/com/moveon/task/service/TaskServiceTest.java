package com.moveon.task.service;

import com.moveon.infra.exception.BusinessException;
import com.moveon.infra.exception.ResourceNotFoundException;
import com.moveon.task.dto.TaskCreateRequest;
import com.moveon.task.dto.TaskUpdateRequest;
import com.moveon.task.entity.Task;
import com.moveon.task.entity.TaskPriority;
import com.moveon.task.entity.TaskStatus;
import com.moveon.task.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 任务服务测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository);
    }

    // ========== 创建任务 ==========

    @Test
    void createTask_Success() {
        Task savedTask = Task.builder()
                .id(1L)
                .userId(1L)
                .title("测试任务")
                .description("任务描述")
                .priority(TaskPriority.HIGH)
                .status(TaskStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        TaskCreateRequest request = TaskCreateRequest.builder()
                .title("测试任务")
                .description("任务描述")
                .priority(TaskPriority.HIGH)
                .build();

        Task result = taskService.createTask(1L, request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("测试任务", result.getTitle());
        assertEquals(TaskPriority.HIGH, result.getPriority());
        assertEquals(TaskStatus.PENDING, result.getStatus());
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void createTask_WithDueDate() {
        LocalDateTime dueDate = LocalDateTime.now().plusDays(7);
        Task savedTask = Task.builder()
                .id(1L)
                .userId(1L)
                .title("带截止日期的任务")
                .priority(TaskPriority.MEDIUM)
                .status(TaskStatus.PENDING)
                .dueDate(dueDate)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        TaskCreateRequest request = TaskCreateRequest.builder()
                .title("带截止日期的任务")
                .dueDate(dueDate)
                .build();

        Task result = taskService.createTask(1L, request);

        assertNotNull(result.getDueDate());
        assertEquals(dueDate, result.getDueDate());
    }

    @Test
    void createTask_DefaultPriority() {
        Task savedTask = Task.builder()
                .id(1L)
                .userId(1L)
                .title("默认优先级任务")
                .priority(TaskPriority.MEDIUM)
                .status(TaskStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        TaskCreateRequest request = TaskCreateRequest.builder()
                .title("默认优先级任务")
                .build();

        Task result = taskService.createTask(1L, request);

        assertEquals(TaskPriority.MEDIUM, result.getPriority());
    }

    // ========== 获取任务 ==========

    @Test
    void getTaskById_Found() {
        Task task = Task.builder()
                .id(1L).userId(1L).title("测试任务")
                .status(TaskStatus.PENDING).build();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        Task result = taskService.getTaskById(1L);
        assertEquals("测试任务", result.getTitle());
    }

    @Test
    void getTaskById_NotFound_ShouldThrow() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.getTaskById(999L));
    }

    @Test
    void getTaskByIdAndUserId_CorrectUser() {
        Task task = Task.builder().id(1L).userId(1L).title("测试").build();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        Task result = taskService.getTaskByIdAndUserId(1L, 1L);
        assertEquals(1L, result.getId());
    }

    @Test
    void getTaskByIdAndUserId_WrongUser_ShouldThrow() {
        Task task = Task.builder().id(1L).userId(1L).title("测试").build();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThrows(BusinessException.class, () -> taskService.getTaskByIdAndUserId(1L, 2L));
    }

    // ========== 更新任务 ==========

    @Test
    void updateTask_Success() {
        Task task = Task.builder()
                .id(1L).userId(1L).title("原标题")
                .description("原描述").priority(TaskPriority.MEDIUM)
                .status(TaskStatus.PENDING).build();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskUpdateRequest request = TaskUpdateRequest.builder()
                .title("新标题")
                .description("新描述")
                .priority(TaskPriority.HIGH)
                .build();

        Task result = taskService.updateTask(1L, 1L, request);

        assertEquals("新标题", result.getTitle());
        assertEquals("新描述", result.getDescription());
        assertEquals(TaskPriority.HIGH, result.getPriority());
    }

    @Test
    void updateTask_PartialUpdate() {
        Task task = Task.builder()
                .id(1L).userId(1L).title("原标题")
                .description("原描述").priority(TaskPriority.MEDIUM)
                .status(TaskStatus.PENDING).build();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskUpdateRequest request = TaskUpdateRequest.builder()
                .title("新标题")
                .build();

        Task result = taskService.updateTask(1L, 1L, request);

        assertEquals("新标题", result.getTitle());
        assertEquals("原描述", result.getDescription());
        assertEquals(TaskPriority.MEDIUM, result.getPriority());
    }

    @Test
    void updateTask_WrongUser_ShouldThrow() {
        Task task = Task.builder().id(1L).userId(1L).title("测试").build();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        TaskUpdateRequest request = TaskUpdateRequest.builder().title("新标题").build();
        assertThrows(BusinessException.class, () -> taskService.updateTask(1L, 2L, request));
    }

    // ========== 完成任务 ==========

    @Test
    void completeTask_Success() {
        Task task = Task.builder()
                .id(1L).userId(1L).title("测试")
                .status(TaskStatus.PENDING).build();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.completeTask(1L, 1L);

        assertEquals(TaskStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getCompletedAt());
    }

    @Test
    void completeTask_AlreadyCompleted_ShouldThrow() {
        Task task = Task.builder()
                .id(1L).userId(1L).title("测试")
                .status(TaskStatus.COMPLETED)
                .completedAt(LocalDateTime.now())
                .build();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThrows(BusinessException.class, () -> taskService.completeTask(1L, 1L));
    }

    // ========== 取消任务 ==========

    @Test
    void cancelTask_Success() {
        Task task = Task.builder()
                .id(1L).userId(1L).title("测试")
                .status(TaskStatus.PENDING).build();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.cancelTask(1L, 1L);

        assertEquals(TaskStatus.CANCELLED, result.getStatus());
    }

    @Test
    void cancelTask_AlreadyCompleted_ShouldThrow() {
        Task task = Task.builder()
                .id(1L).userId(1L).title("测试")
                .status(TaskStatus.COMPLETED)
                .completedAt(LocalDateTime.now())
                .build();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThrows(BusinessException.class, () -> taskService.cancelTask(1L, 1L));
    }

    // ========== 删除任务 ==========

    @Test
    void deleteTask_Success() {
        Task task = Task.builder().id(1L).userId(1L).title("测试").build();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        taskService.deleteTask(1L, 1L);

        verify(taskRepository).delete(task);
    }

    @Test
    void deleteTask_WrongUser_ShouldThrow() {
        Task task = Task.builder().id(1L).userId(1L).title("测试").build();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThrows(BusinessException.class, () -> taskService.deleteTask(1L, 2L));
        verify(taskRepository, never()).delete(any());
    }
}
