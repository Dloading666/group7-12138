package com.rpa.management.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程DTO
 */
@Data
public class WorkflowDTO {
    private Long id;
    private String workflowCode;
    private String name;
    private String description;
    private String category;
    private String status;
    private Integer version;
    private Long userId;
    private String userName;
    private LocalDateTime publishTime;
    private String config;
    private String inputSchema;
    private String graph;
    private Long crawlRobotId;
    private Long analysisRobotId;
    private Long notificationRobotId;
    private Long latestVersionId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    /**
     * 节点列表
     */
    private List<WorkflowNodeDTO> nodes;
    
    /**
     * 步骤数（从config解析）
     */
    private Integer stepCount;
}
