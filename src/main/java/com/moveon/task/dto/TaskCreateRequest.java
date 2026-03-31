package com.moveon.task.dto;

import com.moveon.task.entity.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 创建任务请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreateRequest {

    /**
     * 任务标题
     */
    @NotBlank(message = "任务标题不能为空")
    @Size(max = 200, message = "任务标题不能超过200个字符")
    private String title;

    /**
     * 任务描述
     */
    @Size(max = 2000, message = "任务描述不能超过2000个字符")
    private String description;

    /**
     * 优先级（默认 MEDIUM）
     */
    @Builder.Default
    private TaskPriority priority = TaskPriority.MEDIUM;

    /**
     * 截止时间
     */
    private LocalDateTime dueDate;
}
