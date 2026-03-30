package com.rpa.management.entity;

import com.rpa.management.common.entity.BaseEntity;
import com.rpa.management.common.enums.ExecuteType;
import com.rpa.management.common.enums.TaskPriority;
import com.rpa.management.common.enums.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@Entity
@Table(name = "tasks", indexes = {
    @Index(name = "idx_tasks_task_no", columnList = "taskNo"),
    @Index(name = "idx_tasks_status", columnList = "status"),
    @Index(name = "idx_tasks_priority", columnList = "priority")
})
public class Task extends BaseEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String taskNo;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "task_type", length = 64)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status = TaskStatus.PENDING;

    @Column(nullable = false)
    private Integer progress = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExecuteType executeType = ExecuteType.IMMEDIATE;

    private LocalDateTime scheduleTime;

    private Long robotId;

    private Long createdByUserId;

    @Column(columnDefinition = "LONGTEXT")
    private String params;

    @Column(columnDefinition = "LONGTEXT")
    private String result;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer duration;
}
