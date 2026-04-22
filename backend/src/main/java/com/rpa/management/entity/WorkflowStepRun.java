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
@Table(name = "sys_workflow_step_run")
public class WorkflowStepRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "step_run_id", unique = true, nullable = false, length = 50)
    private String stepRunId;

    @Column(name = "task_run_id")
    private Long taskRunId;

    @Column(name = "debug_run_id")
    private Long debugRunId;

    @Column(name = "node_id", nullable = false, length = 100)
    private String nodeId;

    @Column(name = "node_type", nullable = false, length = 100)
    private String nodeType;

    @Column(name = "node_label", length = 200)
    private String nodeLabel;

    @Column(name = "branch_key", length = 100)
    private String branchKey;

    @Column(name = "engine_task_id", length = 100)
    private String engineTaskId;

    @Column(name = "robot_id")
    private Long robotId;

    @Column(name = "robot_name", length = 100)
    private String robotName;

    @Column(name = "robot_type", length = 50)
    private String robotType;

    @Column(nullable = false, length = 20)
    private String status = "pending";

    @Lob
    @Column(name = "input_snapshot", columnDefinition = "LONGTEXT")
    private String inputSnapshot;

    @Lob
    @Column(name = "output_snapshot", columnDefinition = "LONGTEXT")
    private String outputSnapshot;

    @Lob
    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    private String errorMessage;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    private Integer duration = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    private LocalDateTime updateTime;
}
