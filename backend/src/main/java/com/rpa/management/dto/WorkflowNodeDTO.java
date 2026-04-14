package com.rpa.management.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 流程节点DTO
 */
@Data
public class WorkflowNodeDTO {
    private Long id;
    private Long workflowId;
    private Long nodeTypeId;
    private String nodeType;
    private String name;
    private String description;
    private Integer x;
    private Integer y;
    private String config;
    private Integer timeout;
    private Integer retryCount;
    private Integer order;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    /**
     * 节点类型名称
     */
    private String nodeTypeName;
    
    /**
     * 节点类型图标
     */
    private String nodeTypeIcon;
}
