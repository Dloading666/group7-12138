package com.rpa.management.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 流程节点实体
 */
@Data
@Entity
@Table(name = "workflow_node")
public class WorkflowNode {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 所属流程ID
     */
    private Long workflowId;
    
    /**
     * 节点类型ID
     */
    private Long nodeTypeId;
    
    /**
     * 节点类型代码
     */
    @Column(length = 50)
    private String nodeType;
    
    /**
     * 节点名称
     */
    @Column(nullable = false, length = 200)
    private String name;
    
    /**
     * 节点描述
     */
    @Column(length = 500)
    private String description;
    
    /**
     * X坐标
     */
    private Integer x;
    
    /**
     * Y坐标
     */
    private Integer y;
    
    /**
     * 节点配置（JSON格式）
     */
    @Lob
    @Column(name = "config")
    private String config;
    
    /**
     * 超时时间（秒）
     */
    private Integer timeout;
    
    /**
     * 重试次数
     */
    private Integer retryCount;
    
    /**
     * 执行顺序
     */
    @Column(name = "node_order")
    private Integer order;
    
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
        if (timeout == null) {
            timeout = 60;
        }
        if (retryCount == null) {
            retryCount = 3;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
