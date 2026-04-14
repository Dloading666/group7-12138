package com.rpa.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 执行日志DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionLogDTO {
    
    private Long id;
    
    /**
     * 任务ID
     */
    private Long taskId;
    
    /**
     * 任务编号
     */
    private String taskCode;
    
    /**
     * 任务名称
     */
    private String taskName;
    
    /**
     * 机器人ID
     */
    private Long robotId;
    
    /**
     * 机器人名称
     */
    private String robotName;
    
    /**
     * 日志级别
     */
    private String level;
    
    /**
     * 日志内容
     */
    private String message;
    
    /**
     * 执行阶段
     */
    private String stage;
    
    /**
     * 额外数据
     */
    private String extraData;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
