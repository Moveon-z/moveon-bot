package com.moveon.notification.repository;

import com.moveon.notification.entity.Reminder;
import com.moveon.notification.entity.ReminderStatus;
import com.moveon.notification.entity.ReminderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 提醒记录数据访问层
 */
@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    /**
     * 检查指定任务是否已存在某类型的提醒
     */
    boolean existsByTaskIdAndRemindType(Long taskId, ReminderType remindType);

    /**
     * 查询状态为待发送且提醒时间已过的记录
     */
    List<Reminder> findByStatusAndRemindAtBefore(ReminderStatus status, LocalDateTime before);
}
