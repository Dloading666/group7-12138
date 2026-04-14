package com.rpa.management.repository;

import com.rpa.management.entity.WorkflowNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 流程节点Repository
 */
@Repository
public interface WorkflowNodeRepository extends JpaRepository<WorkflowNode, Long> {
    
    /**
     * 根据流程ID查询所有节点
     */
    List<WorkflowNode> findByWorkflowIdOrderByOrderAsc(Long workflowId);
    
    /**
     * 根据流程ID删除所有节点
     */
    void deleteByWorkflowId(Long workflowId);
    
    /**
     * 根据流程ID统计节点数量
     */
    long countByWorkflowId(Long workflowId);
}
