package com.rpa.management.repository;

import com.rpa.management.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> findByTaskId(String taskId);

    boolean existsByTaskId(String taskId);

    List<Task> findByStatus(String status);

    List<Task> findByStatus(String status, Pageable pageable);

    List<Task> findByUserIdOrderByCreateTimeDesc(Long userId);

    List<Task> findByRobotIdOrderByCreateTimeDesc(Long robotId);

    @Query("""
            SELECT t FROM Task t
            WHERE (:name IS NULL OR t.name LIKE %:name% OR t.taskId LIKE %:name%)
              AND (:type IS NULL OR t.type = :type)
              AND (:status IS NULL OR t.status = :status)
              AND (:priority IS NULL OR t.priority = :priority)
              AND (:userId IS NULL OR t.userId = :userId)
              AND (:robotId IS NULL OR t.robotId = :robotId)
              AND (:startTime IS NULL OR t.createTime >= :startTime)
              AND (:endTime IS NULL OR t.createTime <= :endTime)
            """)
    Page<Task> findByConditions(
            @Param("name") String name,
            @Param("type") String type,
            @Param("status") String status,
            @Param("priority") String priority,
            @Param("userId") Long userId,
            @Param("robotId") Long robotId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );

    @Query("SELECT COUNT(t) FROM Task t")
    Long countAll();

    @Query("SELECT COUNT(t) FROM Task t WHERE t.status = 'running'")
    Long countRunning();

    @Query("SELECT COUNT(t) FROM Task t WHERE t.status = 'completed'")
    Long countCompleted();

    @Query("SELECT COUNT(t) FROM Task t WHERE t.status = 'failed'")
    Long countFailed();
}
