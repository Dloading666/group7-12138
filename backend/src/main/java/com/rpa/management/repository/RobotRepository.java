package com.rpa.management.repository;

import com.rpa.management.entity.Robot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 机器人Repository
 */
@Repository
public interface RobotRepository extends JpaRepository<Robot, Long> {
    
    /**
     * 根据机器人编号查询
     */
    Optional<Robot> findByRobotCode(String robotCode);
    
    /**
     * 检查机器人编号是否存在
     */
    boolean existsByRobotCode(String robotCode);
    
    /**
     * 根据状态查询
     */
    List<Robot> findByStatus(String status);
    
    /**
     * 根据类型查询
     */
    List<Robot> findByType(String type);
    
    /**
     * 分页查询（支持条件筛选）
     */
    @Query("SELECT r FROM Robot r WHERE " +
           "(:name IS NULL OR r.name LIKE %:name%) AND " +
           "(:type IS NULL OR r.type = :type) AND " +
           "(:status IS NULL OR r.status = :status)")
    Page<Robot> findByConditions(
            @Param("name") String name,
            @Param("type") String type,
            @Param("status") String status,
            Pageable pageable
    );
    
    /**
     * 统计各状态数量
     */
    long countByStatus(String status);
    
    /**
     * 统计在线机器人数量
     */
    @Query("SELECT COUNT(r) FROM Robot r WHERE r.status IN ('online', 'running')")
    long countOnlineRobots();

    /**
     * 直接更新状态（绕过 Hibernate 一级缓存）
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE sys_robot SET status = :status WHERE id = :id AND status <> 'running'", nativeQuery = true)
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
