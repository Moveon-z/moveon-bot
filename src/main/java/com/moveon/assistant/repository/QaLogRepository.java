package com.moveon.assistant.repository;

import com.moveon.assistant.entity.QaLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QaLogRepository extends JpaRepository<QaLog, Long> {

    Page<QaLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
