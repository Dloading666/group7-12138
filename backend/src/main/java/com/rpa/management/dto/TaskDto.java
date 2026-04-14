package com.rpa.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {
    
    /**
     * 任务ID
     */
    private Long id;
    
    /**
     * 任务编号
     */
    private String taskId;
    
    /**
     * 任务名称
     */
    @NotBlank(message = "任务名称不能为空")
    @Size(max = 100, message = "任务名称长度不能超过100个字符")
    private String name;
    
    /**
     * 任务类型
     */
    private String type;
    
    /**
     * 任务状态
     */
    private String status;
    
    /**
     * 执行进度
     */
    private Integer progress;
    
    /**
     * 执行机器人ID
     */
    private Long robotId;
    
    /**
     * 执行机器人名称
     */
    private String robotName;
    
    /**
     * 优先级
     */
    private String priority;
    
    /**
     * 执行方式
     */
    private String executeType;
    
    /**
     * 计划执行时间
     */
    private LocalDateTime scheduledTime;
    
    /**
     * 实际开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 实际结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 执行耗时（秒）
     */
    private Integer duration;
    
    /**
     * 创建用户ID
     */
    private Long userId;
    
    /**
     * 创建用户名
     */
    private String userName;
    
    /**
     * 任务描述
     */
    @Size(max = 500, message = "任务描述长度不能超过500个字符")
    private String description;
    
    /**
     * 执行结果
     */
    private String result;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
