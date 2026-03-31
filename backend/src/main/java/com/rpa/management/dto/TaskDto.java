package com.rpa.management.dto;

import com.rpa.management.common.enums.ExecuteType;
import com.rpa.management.common.enums.TaskPriority;
import com.rpa.management.common.enums.TaskStatus;
import com.rpa.management.entity.Task;

import java.time.LocalDateTime;

public record TaskDto(
    Long id,
    String taskNo,
    String name,
    String type,
    TaskStatus status,
    Integer progress,
    TaskPriority priority,
    ExecuteType executeType,
    LocalDateTime scheduleTime,
    Long robotId,
    Long createdByUserId,
    String params,
    String result,
    LocalDateTime createdAt,
    LocalDateTime startTime,
    LocalDateTime endTime,
    Integer duration
) {
    public static TaskDto from(Task task) {
        return new TaskDto(
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
