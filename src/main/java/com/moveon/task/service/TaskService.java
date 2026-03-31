package com.moveon.task.service;

import com.moveon.infra.exception.BusinessException;
import com.moveon.infra.exception.ResourceNotFoundException;
import com.moveon.task.dto.TaskCreateRequest;
import com.moveon.task.dto.TaskUpdateRequest;
import com.moveon.task.entity.Task;
import com.moveon.task.entity.TaskPriority;
import com.moveon.task.entity.TaskStatus;
import com.moveon.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 任务服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    /**
     * 创建任务
     *
     * @param userId  用户 ID
     * @param request 创建请求
     * @return 任务实体
     */
    @Transactional
    public Task createTask(Long userId, TaskCreateRequest request) {
        Task task = Task.builder()
                .userId(userId)
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM)
                .status(TaskStatus.PENDING)
                .dueDate(request.getDueDate())
                .build();

        Task saved = taskRepository.save(task);
        log.info("Task created: id={}, userId={}, title={}", saved.getId(), userId, saved.getTitle());
        return saved;
    }

    /**
     * 根据 ID 获取任务
     */
    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));
    }

    /**
     * 根据 ID 和用户 ID 获取任务（权限检查）
     */
    public Task getTaskByIdAndUserId(Long id, Long userId) {
        Task task = getTaskById(id);
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException("ACCESS_DENIED", "无权访问该任务");
        }
        return task;
    }

    /**
     * 分页查询用户任务列表
     */
    public Page<Task> getTasksByUserId(Long userId, Pageable pageable) {
        return taskRepository.findByUserId(userId, pageable);
    }

    /**
     * 按状态筛选任务
     */
    public Page<Task> getTasksByUserIdAndStatus(Long userId, TaskStatus status, Pageable pageable) {
        return taskRepository.findByUserIdAndStatus(userId, status, pageable);
    }

    /**
     * 按优先级筛选任务
     */
    public Page<Task> getTasksByUserIdAndPriority(Long userId, TaskPriority priority, Pageable pageable) {
        return taskRepository.findByUserIdAndPriority(userId, priority, pageable);
    }

    /**
     * 按状态和优先级筛选任务
     */
    public Page<Task> getTasksByUserIdAndStatusAndPriority(Long userId, TaskStatus status, TaskPriority priority, Pageable pageable) {
        return taskRepository.findByUserIdAndStatusAndPriority(userId, status, priority, pageable);
    }

    /**
     * 按截止时间范围筛选任务
     */
    public Page<Task> getTasksByUserIdAndDueDateBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return taskRepository.findByUserIdAndDueDateBetween(userId, startDate, endDate, pageable);
    }

    /**
     * 更新任务
     */
    @Transactional
    public Task updateTask(Long id, Long userId, TaskUpdateRequest request) {
        Task task = getTaskByIdAndUserId(id, userId);

        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }

        Task saved = taskRepository.save(task);
        log.info("Task updated: id={}, userId={}", saved.getId(), userId);
        return saved;
    }

    /**
     * 标记任务完成
     */
    @Transactional
    public Task completeTask(Long id, Long userId) {
        Task task = getTaskByIdAndUserId(id, userId);

        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new BusinessException("TASK_ALREADY_COMPLETED", "任务已完成");
        }

        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());

        Task saved = taskRepository.save(task);
        log.info("Task completed: id={}, userId={}", saved.getId(), userId);
        return saved;
    }

    /**
     * 取消任务
     */
    @Transactional
    public Task cancelTask(Long id, Long userId) {
        Task task = getTaskByIdAndUserId(id, userId);

        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new BusinessException("TASK_ALREADY_COMPLETED", "已完成的任务不能取消");
        }

        task.setStatus(TaskStatus.CANCELLED);

        Task saved = taskRepository.save(task);
        log.info("Task cancelled: id={}, userId={}", saved.getId(), userId);
        return saved;
    }

    /**
     * 删除任务
     */
    @Transactional
    public void deleteTask(Long id, Long userId) {
        Task task = getTaskByIdAndUserId(id, userId);
        taskRepository.delete(task);
        log.info("Task deleted: id={}, userId={}", id, userId);
    }
}
