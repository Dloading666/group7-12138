package com.rpa.management.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 节点类型实体（可动态配置）
 */
@Data
@Entity
@Table(name = "node_type")
public class NodeType {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 节点类型代码（唯一标识）
     */
    @Column(unique = true, nullable = false, length = 50)
    private String type;
    
    /**
     * 节点类型名称
     */
    @Column(nullable = false, length = 100)
    private String name;
    
    /**
     * 图标名称
     */
    @Column(length = 100)
    private String icon;
    
    /**
     * 图标颜色
     */
    @Column(length = 20)
    private String color;
    
    /**
     * 分类（基础、机器人、逻辑等）
     */
    @Column(length = 50)
    private String category;
    
    /**
     * 排序
     */
    private Integer sortOrder;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 默认配置（JSON格式）
     */
    @Lob
    @Column(name = "default_config")
    private String defaultConfig;
    
    /**
     * 描述
     */
    @Column(length = 500)
    private String description;
    
    /**
     * 创建时间
     */
    @Column(updatable = false)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (enabled == null) {
            enabled = true;
        }
        if (sortOrder == null) {
            sortOrder = 0;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
