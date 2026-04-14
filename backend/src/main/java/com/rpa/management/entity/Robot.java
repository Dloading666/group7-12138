package com.rpa.management.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 机器人实体类
 */
@Data
@Entity
@Table(name = "sys_robot")
public class Robot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 机器人编号
     */
    @Column(unique = true, nullable = false, length = 50)
    private String robotCode;
    
    /**
     * 机器人名称
     */
    @Column(nullable = false, length = 100)
    private String name;
    
    /**
     * 机器人类型：data_collector-数据采集, report_generator-报表生成, task_scheduler-任务调度, notification-消息通知
     */
    @Column(nullable = false, length = 50)
    private String type;
    
    /**
     * 机器人描述
     */
    @Column(length = 500)
    private String description;
    
    /**
     * 状态：online-在线, offline-离线, running-运行中
     */
    @Column(nullable = false, length = 20)
    private String status = "offline";
    
    /**
     * 执行任务总数
     */
    @Column(nullable = false)
    private Long totalTasks = 0L;
    
    /**
     * 成功任务数
     */
    @Column(nullable = false)
    private Long successTasks = 0L;
    
    /**
     * 失败任务数
     */
    @Column(nullable = false)
    private Long failedTasks = 0L;
    
    /**
     * 成功率
     */
    @Column(nullable = false)
    private Double successRate = 0.0;
    
    /**
     * 最后执行时间
     */
    private LocalDateTime lastExecuteTime;
    
    /**
     * 当前任务ID
     */
    @Column(length = 100)
    private String currentTaskId;
    
    /**
     * 最后心跳时间
     */
    private LocalDateTime lastHeartbeat;
    
    /**
     * 创建者ID
     */
    private Long createBy;
    
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
