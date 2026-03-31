package com.moveon.task.controller;

import com.moveon.auth.entity.User;
import com.moveon.infra.dto.ApiResponse;
import com.moveon.infra.dto.PageResponse;
import com.moveon.task.dto.TaskCreateRequest;
import com.moveon.task.dto.TaskResponse;
import com.moveon.task.dto.TaskUpdateRequest;
import com.moveon.task.entity.Task;
import com.moveon.task.entity.TaskPriority;
import com.moveon.task.entity.TaskStatus;
import com.moveon.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 任务管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "任务管理", description = "任务创建、查询和管理接口")
public class TaskController {

    private final TaskService taskService;

    /**
     * 创建任务
     */
    @PostMapping
    @Operation(summary = "创建任务", description = "创建新的待办任务")
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @RequestAttribute("user") User user,
            @RequestBody @Valid TaskCreateRequest request) {

        Task task = taskService.createTask(user.getId(), request);
        TaskResponse response = toTaskResponse(task);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取任务列表
     */
    @GetMapping
    @Operation(summary = "获取任务列表", description = "获取当前用户的任务列表，支持分页、状态和优先级筛选")
    public ResponseEntity<ApiResponse<PageResponse<TaskResponse>>> getTaskList(
            @RequestAttribute("user") User user,
            @Parameter(description = "页码（从 0 开始）")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "排序字段")
            @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "排序方向")
            @RequestParam(defaultValue = "desc") String direction,
            @Parameter(description = "状态筛选")
            @RequestParam(required = false) TaskStatus status,
            @Parameter(description = "优先级筛选")
            @RequestParam(required = false) TaskPriority priority,
            @Parameter(description = "截止时间起始（格式：yyyy-MM-dd HH:mm:ss）")
            @RequestParam(required = false) String dueDateFrom,
            @Parameter(description = "截止时间结束（格式：yyyy-MM-dd HH:mm:ss）")
            @RequestParam(required = false) String dueDateTo) {

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        Page<Task> taskPage;

        if (dueDateFrom != null && dueDateTo != null) {
            LocalDateTime startDate = LocalDateTime.parse(dueDateFrom, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            LocalDateTime endDate = LocalDateTime.parse(dueDateTo, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            taskPage = taskService.getTasksByUserIdAndDueDateBetween(user.getId(), startDate, endDate, pageable);
        } else if (status != null && priority != null) {
            taskPage = taskService.getTasksByUserIdAndStatusAndPriority(user.getId(), status, priority, pageable);
        } else if (status != null) {
            taskPage = taskService.getTasksByUserIdAndStatus(user.getId(), status, pageable);
        } else if (priority != null) {
            taskPage = taskService.getTasksByUserIdAndPriority(user.getId(), priority, pageable);
        } else {
            taskPage = taskService.getTasksByUserId(user.getId(), pageable);
        }

        List<TaskResponse> content = taskPage.getContent().stream()
                .map(this::toTaskResponse)
                .toList();

        PageResponse<TaskResponse> pageResponse = PageResponse.of(
                content,
                taskPage.getNumber(),
                taskPage.getSize(),
                taskPage.getTotalElements()
        );

        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取任务详情", description = "根据任务 ID 获取任务详细信息")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(
            @RequestAttribute("user") User user,
            @Parameter(description = "任务 ID")
            @PathVariable("id") Long id) {

        Task task = taskService.getTaskByIdAndUserId(id, user.getId());
        TaskResponse response = toTaskResponse(task);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 更新任务
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新任务", description = "更新任务的标题、描述、优先级或截止时间")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @RequestAttribute("user") User user,
            @Parameter(description = "任务 ID")
            @PathVariable("id") Long id,
            @RequestBody @Valid TaskUpdateRequest request) {

        Task task = taskService.updateTask(id, user.getId(), request);
        TaskResponse response = toTaskResponse(task);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 标记任务完成
     */
    @PostMapping("/{id}/complete")
    @Operation(summary = "完成任务", description = "将任务标记为已完成")
    public ResponseEntity<ApiResponse<TaskResponse>> completeTask(
            @RequestAttribute("user") User user,
            @Parameter(description = "任务 ID")
            @PathVariable("id") Long id) {

        Task task = taskService.completeTask(id, user.getId());
        TaskResponse response = toTaskResponse(task);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 取消任务
     */
    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消任务", description = "将任务标记为已取消")
    public ResponseEntity<ApiResponse<TaskResponse>> cancelTask(
            @RequestAttribute("user") User user,
            @Parameter(description = "任务 ID")
            @PathVariable("id") Long id) {

        Task task = taskService.cancelTask(id, user.getId());
        TaskResponse response = toTaskResponse(task);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除任务", description = "删除指定任务")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @RequestAttribute("user") User user,
            @Parameter(description = "任务 ID")
            @PathVariable("id") Long id) {

        taskService.deleteTask(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private TaskResponse toTaskResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .priority(task.getPriority().name())
                .status(task.getStatus().name())
                .dueDate(task.getDueDate())
                .completedAt(task.getCompletedAt())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
