package com.rpa.management.repository;

import com.rpa.management.entity.ExecutionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 执行日志Repository
 */
@Repository
public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, Long> {
    
    /**
     * 根据任务ID查询日志
     */
    List<ExecutionLog> findByTaskIdOrderByCreateTimeDesc(Long taskId);

    List<ExecutionLog> findByTaskRunIdOrderByCreateTimeAsc(Long taskRunId);
    
    /**
     * 根据日志级别查询
     */
    List<ExecutionLog> findByLevelOrderByCreateTimeDesc(String level);
    
    /**
     * 分页查询日志（支持条件筛选）
     */
    @Query("SELECT e FROM ExecutionLog e WHERE " +
           "(:level IS NULL OR e.level = :level) AND " +
           "(:taskId IS NULL OR e.taskId = :taskId) AND " +
           "(:taskCode IS NULL OR e.taskCode LIKE %:taskCode%) AND " +
           "(:robotId IS NULL OR e.robotId = :robotId) AND " +
           "(:startTime IS NULL OR e.createTime >= :startTime) AND " +
           "(:endTime IS NULL OR e.createTime <= :endTime)")
    Page<ExecutionLog> findByConditions(
            @Param("level") String level,
            @Param("taskId") Long taskId,
            @Param("taskCode") String taskCode,
            @Param("robotId") Long robotId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );
    
    /**
     * 统计各级别日志数量
     */
    @Query("SELECT e.level, COUNT(e) FROM ExecutionLog e GROUP BY e.level")
    List<Object[]> countByLevel();
    
    /**
     * 删除指定时间之前的日志
     */
    void deleteByCreateTimeBefore(LocalDateTime time);
}
