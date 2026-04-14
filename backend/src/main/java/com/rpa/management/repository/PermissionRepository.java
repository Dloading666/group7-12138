package com.rpa.management.repository;

import com.rpa.management.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 权限Repository
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    
    /**
     * 根据编码查询
     */
    Permission findByCode(String code);
    
    /**
     * 检查编码是否存在
     */
    boolean existsByCode(String code);
    
    /**
     * 查询所有权限并按排序号排序
     */
    List<Permission> findAllByOrderBySortOrderAsc();
    
    /**
     * 根据状态查询并排序
     */
    List<Permission> findByStatusOrderBySortOrderAsc(String status);
    
    /**
     * 根据父级ID查询
     */
    List<Permission> findByParentIdOrderBySortOrderAsc(Long parentId);
    
    /**
     * 查询顶级权限（父级ID为0）
     */
    @Query("SELECT p FROM Permission p WHERE p.parentId = 0 OR p.parentId IS NULL ORDER BY p.sortOrder ASC")
    List<Permission> findTopPermissions();
    
    /**
     * 根据ID列表查询
     */
    List<Permission> findByIdIn(List<Long> ids);
    
    /**
     * 根据类型查询
     */
    List<Permission> findByTypeOrderBySortOrderAsc(String type);
    
    /**
     * 查询启用的菜单权限
     */
    @Query("SELECT p FROM Permission p WHERE p.type = 'menu' AND p.status = 'active' ORDER BY p.sortOrder ASC")
    List<Permission> findActiveMenus();
}
