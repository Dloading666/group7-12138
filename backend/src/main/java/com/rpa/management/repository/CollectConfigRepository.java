package com.rpa.management.repository;

import com.rpa.management.entity.CollectConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 采集配置Repository
 */
@Repository
public interface CollectConfigRepository extends JpaRepository<CollectConfig, Long> {
    
    /**
     * 查询启用的配置
     */
    List<CollectConfig> findByIsEnabledTrue();
    
    /**
     * 根据机器人ID查询配置
     */
    List<CollectConfig> findByRobotId(Long robotId);
    
    /**
     * 根据任务ID查询配置
     */
    List<CollectConfig> findByTaskId(Long taskId);
    
    /**
     * 分页查询配置
     */
    @Query("SELECT c FROM CollectConfig c WHERE " +
           "(:name IS NULL OR c.name LIKE %:name%) AND " +
           "(:collectType IS NULL OR c.collectType = :collectType) AND " +
           "(:isEnabled IS NULL OR c.isEnabled = :isEnabled)")
    Page<CollectConfig> findByConditions(
            @Param("name") String name,
            @Param("collectType") String collectType,
            @Param("isEnabled") Boolean isEnabled,
            Pageable pageable
    );
}
