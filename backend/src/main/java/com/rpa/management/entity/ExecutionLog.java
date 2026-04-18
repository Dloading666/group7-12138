package com.rpa.management.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 执行日志实体类
 */
@Data
@Entity
@Table(name = "sys_execution_log")
public class ExecutionLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 任务ID
     */
    @Column(name = "task_id", nullable = true)
    private Long taskId;

    @Column(name = "task_run_id")
    private Long taskRunId;
    
    /**
     * 任务编号
     */
    @Column(name = "task_code", length = 50)
    private String taskCode;
    
    /**
     * 任务名称
     */
    @Column(name = "task_name", length = 100)
    private String taskName;
    
    /**
     * 机器人ID
     */
    @Column(name = "robot_id")
    private Long robotId;
    
    /**
     * 机器人名称
     */
    @Column(name = "robot_name", length = 50)
    private String robotName;
    
    /**
     * 日志级别: INFO, WARN, ERROR
     */
    @Column(nullable = false, length = 20)
    private String level = "INFO";
    
    /**
     * 日志内容
     */
    @Lob
    @Column(nullable = false, name = "message", columnDefinition = "TEXT")
    private String message;
    
    /**
     * 执行阶段: start, process, end, error
     */
    @Column(name = "stage", length = 50)
    private String stage;
    
    /**
     * 额外数据（JSON格式）
     */
    @Lob
    @Column(name = "extra_data")
    private String extraData;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createTime;
}
