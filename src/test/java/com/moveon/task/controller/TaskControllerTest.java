package com.moveon.task.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveon.auth.entity.User;
import com.moveon.auth.entity.UserRole;
import com.moveon.auth.entity.UserStatus;
import com.moveon.infra.exception.BusinessException;
import com.moveon.infra.exception.ResourceNotFoundException;
import com.moveon.task.dto.TaskCreateRequest;
import com.moveon.task.dto.TaskUpdateRequest;
import com.moveon.task.entity.Task;
import com.moveon.task.entity.TaskPriority;
import com.moveon.task.entity.TaskStatus;
import com.moveon.task.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 任务控制器测试
 */
@WebMvcTest(
    controllers = TaskController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {com.moveon.infra.config.SecurityConfig.class, com.moveon.auth.config.JwtAuthenticationFilter.class})
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    // ========== 创建任务 ==========

    @Test
    void createTask_Success() throws Exception {
        Task task = Task.builder()
                .id(1L)
                .userId(1L)
                .title("测试任务")
                .description("任务描述")
                .priority(TaskPriority.HIGH)
                .status(TaskStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(taskService.createTask(eq(1L), any(TaskCreateRequest.class))).thenReturn(task);

        TaskCreateRequest request = TaskCreateRequest.builder()
                .title("测试任务")
                .description("任务描述")
                .priority(TaskPriority.HIGH)
                .build();

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr("user", createTestUser(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("测试任务"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void createTask_TitleBlank_ShouldReturn400() throws Exception {
        TaskCreateRequest request = TaskCreateRequest.builder()
                .title("")
                .build();

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr("user", createTestUser(1L)))
                .andExpect(status().isBadRequest());
    }

    // ========== 获取任务列表 ==========

    @Test
    void getTaskList_Success() throws Exception {
        Task task = Task.builder()
                .id(1L).userId(1L).title("任务1")
                .priority(TaskPriority.MEDIUM).status(TaskStatus.PENDING)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        Page<Task> page = new PageImpl<>(List.of(task));

        when(taskService.getTasksByUserId(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/tasks")
                        .requestAttr("user", createTestUser(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].title").value("任务1"));
    }

    @Test
    void getTaskList_WithStatusFilter() throws Exception {
        Page<Task> page = new PageImpl<>(List.of());
        when(taskService.getTasksByUserIdAndStatus(eq(1L), eq(TaskStatus.PENDING), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/tasks")
                        .param("status", "PENDING")
                        .requestAttr("user", createTestUser(1L)))
                .andExpect(status().isOk());

        verify(taskService).getTasksByUserIdAndStatus(eq(1L), eq(TaskStatus.PENDING), any(Pageable.class));
    }

    @Test
    void getTaskList_WithPriorityFilter() throws Exception {
        Page<Task> page = new PageImpl<>(List.of());
        when(taskService.getTasksByUserIdAndPriority(eq(1L), eq(TaskPriority.HIGH), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/tasks")
                        .param("priority", "HIGH")
                        .requestAttr("user", createTestUser(1L)))
                .andExpect(status().isOk());

        verify(taskService).getTasksByUserIdAndPriority(eq(1L), eq(TaskPriority.HIGH), any(Pageable.class));
    }

    @Test
    void getTaskList_WithStatusAndPriorityFilter() throws Exception {
        Page<Task> page = new PageImpl<>(List.of());
        when(taskService.getTasksByUserIdAndStatusAndPriority(eq(1L), eq(TaskStatus.PENDING), eq(TaskPriority.HIGH), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/tasks")
                        .param("status", "PENDING")
                        .param("priority", "HIGH")
                        .requestAttr("user", createTestUser(1L)))
                .andExpect(status().isOk());

        verify(taskService).getTasksByUserIdAndStatusAndPriority(eq(1L), eq(TaskStatus.PENDING), eq(TaskPriority.HIGH), any(Pageable.class));
    }

    // ========== 获取任务详情 ==========

    @Test
    void getTask_Success() throws Exception {
        Task task = Task.builder()
                .id(1L).userId(1L).title("测试任务")
                .description("描述")
                .priority(TaskPriority.HIGH).status(TaskStatus.PENDING)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(taskService.getTaskByIdAndUserId(eq(1L), eq(1L))).thenReturn(task);

        mockMvc.perform(get("/tasks/1")
                        .requestAttr("user", createTestUser(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("测试任务"));
    }

    @Test
    void getTask_NotFound() throws Exception {
        when(taskService.getTaskByIdAndUserId(eq(999L), eq(1L)))
                .thenThrow(new ResourceNotFoundException("Task", "id", 999L));

        mockMvc.perform(get("/tasks/999")
                        .requestAttr("user", createTestUser(1L)))
                .andExpect(status().isNotFound());
    }

    // ========== 更新任务 ==========

    @Test
    void updateTask_Success() throws Exception {
        Task task = Task.builder()
                .id(1L).userId(1L).title("新标题")
                .description("新描述")
                .priority(TaskPriority.URGENT).status(TaskStatus.PENDING)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(taskService.updateTask(eq(1L), eq(1L), any(TaskUpdateRequest.class))).thenReturn(task);

        TaskUpdateRequest request = TaskUpdateRequest.builder()
                .title("新标题")
                .priority(TaskPriority.URGENT)
                .build();

        mockMvc.perform(put("/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr("user", createTestUser(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("新标题"))
                .andExpect(jsonPath("$.data.priority").value("URGENT"));
    }

    // ========== 完成任务 ==========

    @Test
    void completeTask_Success() throws Exception {
        Task task = Task.builder()
                .id(1L).userId(1L).title("测试")
                .status(TaskStatus.COMPLETED)
                .completedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(taskService.completeTask(eq(1L), eq(1L))).thenReturn(task);

        mockMvc.perform(post("/tasks/1/complete")
                        .requestAttr("user", createTestUser(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completedAt").exists());
    }

    // ========== 取消任务 ==========

    @Test
    void cancelTask_Success() throws Exception {
        Task task = Task.builder()
                .id(1L).userId(1L).title("测试")
                .status(TaskStatus.CANCELLED)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(taskService.cancelTask(eq(1L), eq(1L))).thenReturn(task);

        mockMvc.perform(post("/tasks/1/cancel")
                        .requestAttr("user", createTestUser(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    // ========== 删除任务 ==========

    @Test
    void deleteTask_Success() throws Exception {
        doNothing().when(taskService).deleteTask(eq(1L), eq(1L));

        mockMvc.perform(delete("/tasks/1")
                        .requestAttr("user", createTestUser(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(taskService).deleteTask(1L, 1L);
    }

    private User createTestUser(Long id) {
        return User.builder()
                .id(id)
                .username("testuser")
                .password("encoded")
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .build();
    }
}
