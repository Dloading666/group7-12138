package com.rpa.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 机器人DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RobotDTO {
    
    private Long id;
    
    /**
     * 机器人编号
     */
    private String robotCode;
    
    /**
     * 机器人名称
     */
    private String name;
    
    /**
     * 机器人类型
     */
    private String type;
    
    /**
     * 类型显示名称
     */
    private String typeDisplayName;
    
    /**
     * 机器人描述
     */
    private String description;
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 状态显示名称
     */
    private String statusDisplayName;
    
    /**
     * 执行任务总数
     */
    private Long totalTasks;
    
    /**
     * 成功任务数
     */
    private Long successTasks;
    
    /**
     * 失败任务数
     */
    private Long failedTasks;
    
    /**
     * 成功率
     */
    private Double successRate;
    
    /**
     * 最后执行时间
     */
    private LocalDateTime lastExecuteTime;
    
    /**
     * 当前任务ID
     */
    private String currentTaskId;
    
    /**
     * 最后心跳时间
     */
    private LocalDateTime lastHeartbeat;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
