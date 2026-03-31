package com.moveon.task.repository;

import com.moveon.task.entity.Task;
import com.moveon.task.entity.TaskPriority;
import com.moveon.task.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务数据访问层
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * 分页查询用户任务列表
     */
    Page<Task> findByUserId(Long userId, Pageable pageable);

    /**
     * 查询用户指定状态的任务（分页）
     */
    Page<Task> findByUserIdAndStatus(Long userId, TaskStatus status, Pageable pageable);

    /**
     * 查询用户指定优先级的任务（分页）
     */
    Page<Task> findByUserIdAndPriority(Long userId, TaskPriority priority, Pageable pageable);

    /**
     * 查询用户指定状态和优先级的任务（分页）
     */
    Page<Task> findByUserIdAndStatusAndPriority(Long userId, TaskStatus status, TaskPriority priority, Pageable pageable);

    /**
     * 查询用户指定时间范围内截止的任务
     */
    @Query("SELECT t FROM Task t WHERE t.userId = :userId AND t.dueDate BETWEEN :startDate AND :endDate ORDER BY t.dueDate ASC")
    Page<Task> findByUserIdAndDueDateBetween(@Param("userId") Long userId,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate,
                                              Pageable pageable);

    /**
     * 查询用户任务数量
     */
    long countByUserId(Long userId);

    /**
     * 查询指定状态且截止时间在指定范围内的任务（用于提醒检查）
     */
    @Query("SELECT t FROM Task t WHERE t.status IN :statuses AND t.dueDate BETWEEN :from AND :to")
    List<Task> findByStatusInAndDueDateBetween(@Param("statuses") List<TaskStatus> statuses,
                                                @Param("from") LocalDateTime from,
                                                @Param("to") LocalDateTime to);
}
