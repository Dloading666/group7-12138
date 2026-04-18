package com.rpa.management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 任务实体
 */
@Data
@Entity
@Table(name = "sys_task")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", unique = true, length = 50)
    private String taskId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String type;

    @Column(name = "workflow_id")
    private Long workflowId;

    @Column(name = "workflow_version_id")
    private Long workflowVersionId;

    @Column(name = "workflow_version")
    private Integer workflowVersion;

    @Column(name = "workflow_name", length = 200)
    private String workflowName;

    @Column(name = "workflow_category", length = 100)
    private String workflowCategory;

    @Column(nullable = false, length = 20)
    private String status = "pending";

    @Column(nullable = false)
    private Integer progress = 0;

    @Column(name = "robot_id")
    private Long robotId;

    @Column(name = "robot_name", length = 50)
    private String robotName;

    @Column(length = 20)
    private String priority = "medium";

    @Column(name = "execute_type", length = 20)
    private String executeType = "immediate";

    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    private Integer duration = 0;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_name", length = 50)
    private String userName;

    @Lob
    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;

    @Lob
    @Column(name = "params", columnDefinition = "LONGTEXT")
    private String params;

    @Lob
    @Column(name = "input_config", columnDefinition = "LONGTEXT")
    private String inputConfig;

    @Lob
    @Column(name = "schedule_config", columnDefinition = "LONGTEXT")
    private String scheduleConfig;

    @Column(name = "latest_run_id")
    private Long latestRunId;

    @Column(name = "latest_run_status", length = 20)
    private String latestRunStatus;

    @Column(name = "last_run_time")
    private LocalDateTime lastRunTime;

    @Column(name = "next_run_time")
    private LocalDateTime nextRunTime;

    @Lob
    @Column(name = "result", columnDefinition = "LONGTEXT")
    private String result;

    @Lob
    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createTime;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(name = "enterprise_name", length = 200)
    private String enterpriseName;

    @UpdateTimestamp
    private LocalDateTime updateTime;
}
