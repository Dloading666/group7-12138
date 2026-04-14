package com.rpa.management.repository;

import com.rpa.management.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 任务Repository
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    /**
     * 根据任务编号查询
     */
    Optional<Task> findByTaskId(String taskId);
    
    /**
     * 检查任务编号是否存在
     */
    boolean existsByTaskId(String taskId);
    
    /**
     * 根据状态查询任务
     */
    List<Task> findByStatus(String status);
    
    /**
     * 根据状态查询任务（分页）
     */
    List<Task> findByStatus(String status, Pageable pageable);
    
    /**
     * 根据用户ID查询任务
     */
    List<Task> findByUserIdOrderByCreateTimeDesc(Long userId);
    
    /**
     * 根据机器人ID查询任务
     */
    List<Task> findByRobotIdOrderByCreateTimeDesc(Long robotId);
    
    /**
     * 分页查询任务（支持条件筛选）
     */
    @Query("SELECT t FROM Task t WHERE " +
           "(:name IS NULL OR t.name LIKE %:name%) AND " +
           "(:type IS NULL OR t.type = :type) AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:priority IS NULL OR t.priority = :priority) AND " +
           "(:userId IS NULL OR t.userId = :userId) AND " +
           "(:robotId IS NULL OR t.robotId = :robotId) AND " +
           "(:startTime IS NULL OR t.createTime >= :startTime) AND " +
           "(:endTime IS NULL OR t.createTime <= :endTime)")
    Page<Task> findByConditions(
            @Param("name") String name,
            @Param("type") String type,
            @Param("status") String status,
            @Param("priority") String priority,
            @Param("userId") Long userId,
            @Param("robotId") Long robotId,
            @Param("startTime") java.time.LocalDateTime startTime,
            @Param("endTime") java.time.LocalDateTime endTime,
            Pageable pageable
    );
    
    /**
     * 统计各状态任务数量
     */
    @Query("SELECT t.status, COUNT(t) FROM Task t GROUP BY t.status")
    List<Object[]> countByStatus();
    
    /**
     * 统计总任务数
     */
    @Query("SELECT COUNT(t) FROM Task t")
    Long countAll();
    
    /**
     * 统计执行中任务数
     */
    @Query("SELECT COUNT(t) FROM Task t WHERE t.status = 'running'")
    Long countRunning();
    
    /**
     * 统计已完成任务数
     */
    @Query("SELECT COUNT(t) FROM Task t WHERE t.status = 'completed'")
    Long countCompleted();

    /**
     * 统计失败任务数
     */
    @Query("SELECT COUNT(t) FROM Task t WHERE t.status = 'failed'")
    Long countFailed();
}
