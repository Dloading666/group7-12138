package com.rpa.management.repository;

import com.rpa.management.entity.User;
import com.rpa.management.entity.UserRole;
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
 * 用户Repository
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 根据用户名查询用户
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);
    
    /**
     * 检查用户名是否存在（排除指定ID）
     */
    boolean existsByUsernameAndIdNot(String username, Long id);
    
    /**
     * 根据角色查询用户
     */
    List<User> findByRole(UserRole role);
    
    /**
     * 根据状态查询用户
     */
    List<User> findByStatus(String status);
    
    /**
     * 分页查询用户（支持条件筛选）
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:username IS NULL OR u.username LIKE %:username%) AND " +
           "(:realName IS NULL OR u.realName LIKE %:realName%) AND " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:status IS NULL OR u.status = :status)")
    Page<User> findByConditions(
            @Param("username") String username,
            @Param("realName") String realName,
            @Param("role") UserRole role,
            @Param("status") String status,
            Pageable pageable
    );
    
    /**
     * 统计用户数量
     */
    long countByStatus(String status);
    
    /**
     * 修复角色数据：将小写的角色值转为大写
     */
    @Modifying
    @Query("UPDATE User u SET u.role = 'ADMIN' WHERE u.role = 'admin'")
    void updateAdminRoleToUppercase();
    
    /**
     * 修复角色数据：将所有小写角色值转为大写
     */
    @Modifying
    @Query(value = "UPDATE \"sys_user\" SET role = UPPER(role) WHERE role IN ('admin', 'user')", nativeQuery = true)
    void updateRoleToUppercase();
}
