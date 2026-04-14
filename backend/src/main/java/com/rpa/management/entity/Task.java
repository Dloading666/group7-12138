package com.rpa.management.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 任务实体类
 */
@Data
@Entity
@Table(name = "sys_task")
public class Task {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 任务编号
     */
    @Column(name = "task_id", unique = true, length = 50)
    private String taskId;
    
    /**
     * 任务名称
     */
    @Column(nullable = false, length = 100)
    private String name;
    
    /**
     * 任务类型
     */
    @Column(length = 50)
    private String type;
    
    /**
     * 任务状态：pending-等待中，running-执行中，completed-已完成，failed-失败
     */
    @Column(nullable = false, length = 20)
    private String status = "pending";
    
    /**
     * 执行进度（0-100）
     */
    @Column(nullable = false)
    private Integer progress = 0;
    
    /**
     * 执行机器人ID
     */
    @Column(name = "robot_id")
    private Long robotId;
    
    /**
     * 执行机器人名称
     */
    @Column(name = "robot_name", length = 50)
    private String robotName;
    
    /**
     * 优先级：high-高，medium-中，low-低
     */
    @Column(length = 20)
    private String priority = "medium";
    
    /**
     * 执行方式：immediate-立即执行，scheduled-定时执行
     */
    @Column(name = "execute_type", length = 20)
    private String executeType = "immediate";
    
    /**
     * 计划执行时间
     */
    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;
    
    /**
     * 实际开始时间
     */
    @Column(name = "start_time")
    private LocalDateTime startTime;
    
    /**
     * 实际结束时间
     */
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    /**
     * 执行耗时（秒）
     */
    private Integer duration = 0;
    
    /**
     * 创建用户ID
     */
    @Column(name = "user_id")
    private Long userId;
    
    /**
     * 创建用户名
     */
    @Column(name = "user_name", length = 50)
    private String userName;
    
    /**
     * 任务描述
     */
    @Lob
    @Column(name = "description")
    private String description;

    /**
     * 执行结果
     */
    @Lob
    @Column(name = "result")
    private String result;

    /**
     * 错误信息
     */
    @Lob
    @Column(name = "error_message")
    private String errorMessage;
    
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
