package com.rpa.management.dto;

import com.rpa.management.common.enums.ExecuteType;
import com.rpa.management.common.enums.TaskPriority;
import com.rpa.management.common.enums.TaskStatus;
import com.rpa.management.entity.Task;

import java.time.LocalDateTime;

public record WorkflowDto(
    Long id,
    String workflowNo,
    String name,
    String type,
    TaskStatus status,
    Integer progress,
    TaskPriority priority,
    ExecuteType executeType,
    LocalDateTime scheduleTime,
    Long robotId,
    String robotName,
    Long createdByUserId,
    String definitionJson,
    String result,
    LocalDateTime createdAt,
    LocalDateTime startTime,
    LocalDateTime endTime,
    Integer duration
) {
    public static WorkflowDto from(Task task) {
        return new WorkflowDto(
            task.getId(),
            task.getTaskNo(),
            task.getName(),
            task.getType(),
            task.getStatus(),
            task.getProgress(),
            task.getPriority(),
            task.getExecuteType(),
            task.getScheduleTime(),
            task.getRobotId(),
            null,
            task.getCreatedByUserId(),
            task.getParams(),
            task.getResult(),
            task.getCreatedAt(),
            task.getStartTime(),
            task.getEndTime(),
            task.getDuration()
        );
    }
}
