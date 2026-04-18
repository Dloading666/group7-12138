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

@Data
@Entity
@Table(name = "sys_task_run")
public class TaskRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", unique = true, nullable = false, length = 50)
    private String runId;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "task_code", length = 50)
    private String taskCode;

    @Column(name = "task_name", length = 100)
    private String taskName;

    @Column(name = "workflow_version_id")
    private Long workflowVersionId;

    @Column(name = "workflow_name", length = 200)
    private String workflowName;

    @Column(name = "workflow_category", length = 100)
    private String workflowCategory;

    @Column(nullable = false, length = 20)
    private String status = "pending";

    @Column(nullable = false)
    private Integer progress = 0;

    @Column(name = "trigger_type", length = 20)
    private String triggerType = "manual";

    @Column(name = "engine_run_id", length = 100)
    private String engineRunId;

    @Lob
    @Column(name = "input_config", columnDefinition = "LONGTEXT")
    private String inputConfig;

    @Lob
    @Column(name = "workflow_snapshot", columnDefinition = "LONGTEXT")
    private String workflowSnapshot;

    @Lob
    @Column(name = "result", columnDefinition = "LONGTEXT")
    private String result;

    @Lob
    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    private String errorMessage;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    private Integer duration = 0;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_name", length = 50)
    private String userName;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    private LocalDateTime updateTime;
}
