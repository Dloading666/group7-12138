package com.rpa.management.repository;

import com.rpa.management.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 角色Repository
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    /**
     * 根据角色编码查询
     */
    Optional<Role> findByCode(String code);
    
    /**
     * 检查角色编码是否存在
     */
    boolean existsByCode(String code);
    
    /**
     * 检查角色编码是否存在（排除指定ID）
     */
    boolean existsByCodeAndIdNot(String code, Long id);
    
    /**
     * 根据状态查询角色列表
     */
    List<Role> findByStatus(String status);
    
    /**
     * 根据状态查询角色列表（带排序）
     */
    List<Role> findByStatusOrderBySortOrderAsc(String status);
    
    /**
     * 查询所有角色（按排序号升序）
     */
    List<Role> findAllByOrderBySortOrderAsc();
    
    /**
     * 分页查询角色（支持名称模糊查询）
     */
    @Query("SELECT r FROM Role r WHERE " +
           "(:name IS NULL OR r.name LIKE %:name%) AND " +
           "(:code IS NULL OR r.code LIKE %:code%) AND " +
           "(:status IS NULL OR r.status = :status)")
    Page<Role> findByConditions(
            @Param("name") String name,
            @Param("code") String code,
            @Param("status") String status,
            Pageable pageable
    );
    
    /**
     * 根据ID列表查询角色
     */
    List<Role> findByIdIn(List<Long> ids);
}
