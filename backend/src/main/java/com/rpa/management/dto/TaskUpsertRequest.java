package com.rpa.management.dto;

import com.rpa.management.common.enums.ExecuteType;
import com.rpa.management.common.enums.TaskPriority;
import com.rpa.management.common.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record TaskUpsertRequest(
    @NotBlank String taskNo,
    @NotBlank String name,
    String type,
    @NotNull TaskStatus status,
    Integer progress,
    @NotNull TaskPriority priority,
    @NotNull ExecuteType executeType,
    LocalDateTime scheduleTime,
    Long robotId,
    Long createdByUserId,
    String params,
    String result
) {
}
