package com.rpa.management.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 权限/资源实体类
 */
@Data
@Entity
@Table(name = "sys_permission")
public class Permission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 权限名称
     */
    @Column(nullable = false, length = 50)
    private String name;
    
    /**
     * 权限编码（唯一标识）
     */
    @Column(unique = true, nullable = false, length = 100)
    private String code;
    
    /**
     * 权限类型：menu-菜单，button-按钮，api-接口
     */
    @Column(nullable = false, length = 20)
    private String type = "menu";
    
    /**
     * 父级ID（用于树形结构）
     */
    @Column(name = "parent_id")
    private Long parentId = 0L;
    
    /**
     * 路由路径
     */
    @Column(length = 200)
    private String path;
    
    /**
     * 图标
     */
    @Column(length = 50)
    private String icon;
    
    /**
     * 排序号
     */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
    
    /**
     * 权限状态：active-启用，inactive-禁用
     */
    @Column(nullable = false, length = 20)
    private String status = "active";
    
    /**
     * 描述
     */
    @Column(length = 200)
    private String description;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @UpdateTimestamp
    private LocalDateTime updateTime;
}
