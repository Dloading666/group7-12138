package com.rpa.management.repository;

import com.rpa.management.entity.Workflow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 流程Repository
 */
@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, Long> {
    
    /**
     * 根据流程编号查询
     */
    Optional<Workflow> findByWorkflowCode(String workflowCode);
    
    /**
     * 检查流程编号是否存在
     */
    boolean existsByWorkflowCode(String workflowCode);
    
    /**
     * 根据状态查询
     */
    List<Workflow> findByStatusOrderByCreateTimeDesc(String status);
    
    /**
     * 根据用户ID查询
     */
    List<Workflow> findByUserIdOrderByCreateTimeDesc(Long userId);
    
    /**
     * 分页查询
     */
    @Query("SELECT w FROM Workflow w WHERE " +
           "(:name IS NULL OR w.name LIKE %:name%) AND " +
           "(:status IS NULL OR w.status = :status) AND " +
           "(:userId IS NULL OR w.userId = :userId)")
    Page<Workflow> findByConditions(
            @Param("name") String name,
            @Param("status") String status,
            @Param("userId") Long userId,
            Pageable pageable
    );
}
