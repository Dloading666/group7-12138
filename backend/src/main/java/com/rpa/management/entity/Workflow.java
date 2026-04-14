package com.rpa.management.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 流程定义实体
 */
@Data
@Entity
@Table(name = "workflow")
public class Workflow {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 流程编号（唯一标识）
     */
    @Column(unique = true, nullable = false, length = 100)
    private String workflowCode;
    
    /**
     * 流程名称
     */
    @Column(nullable = false, length = 200)
    private String name;
    
    /**
     * 流程描述
     */
    @Column(length = 1000)
    private String description;
    
    /**
     * 状态：draft-草稿, published-已发布, archived-已归档
     */
    @Column(length = 20)
    private String status;
    
    /**
     * 版本号
     */
    private Integer version;
    
    /**
     * 创建用户ID
     */
    private Long userId;
    
    /**
     * 创建用户名
     */
    @Column(length = 100)
    private String userName;
    
    /**
     * 发布时间
     */
    private LocalDateTime publishTime;
    
    /**
     * 流程配置（JSON格式，包含节点和连线）
     */
    @Lob
    @Column(name = "config")
    private String config;
    
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
        if (status == null) {
            status = "draft";
        }
        if (version == null) {
            version = 1;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
