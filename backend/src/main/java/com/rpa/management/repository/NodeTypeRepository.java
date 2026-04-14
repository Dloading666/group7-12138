package com.rpa.management.repository;

import com.rpa.management.entity.NodeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 节点类型Repository
 */
@Repository
public interface NodeTypeRepository extends JpaRepository<NodeType, Long> {
    
    /**
     * 根据类型代码查询
     */
    Optional<NodeType> findByType(String type);
    
    /**
     * 根据分类查询
     */
    List<NodeType> findByCategoryOrderBySortOrderAsc(String category);
    
    /**
     * 查询所有启用的节点类型
     */
    List<NodeType> findByEnabledTrueOrderBySortOrderAsc();
    
    /**
     * 检查类型代码是否存在
     */
    boolean existsByType(String type);
}
