package com.rpa.management.repository;

import com.rpa.management.entity.CollectData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 采集数据Repository
 */
@Repository
public interface CollectDataRepository extends JpaRepository<CollectData, Long> {
    
    /**
     * 根据配置ID查询数据
     */
    List<CollectData> findByConfigIdOrderByCollectTimeDesc(Long configId);
    
    /**
     * 根据数据hash查询（用于去重）
     */
    Optional<CollectData> findByDataHash(String dataHash);
    
    /**
     * 检查数据是否存在
     */
    boolean existsByDataHash(String dataHash);
    
    /**
     * 分页查询采集数据
     */
    @Query("SELECT d FROM CollectData d WHERE " +
           "(:configId IS NULL OR d.configId = :configId) AND " +
           "(:taskId IS NULL OR d.taskId = :taskId) AND " +
           "(:status IS NULL OR d.status = :status) AND " +
           "(:startTime IS NULL OR d.collectTime >= :startTime) AND " +
           "(:endTime IS NULL OR d.collectTime <= :endTime)")
    Page<CollectData> findByConditions(
            @Param("configId") Long configId,
            @Param("taskId") Long taskId,
            @Param("status") String status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );
    
    /**
     * 统计配置的采集数据量
     */
    Long countByConfigId(Long configId);
    
    /**
     * 删除指定配置的所有数据
     */
    void deleteByConfigId(Long configId);
}
