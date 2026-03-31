package com.rpa.management.dto;

import java.time.LocalDateTime;

public record MonitorExecutionLogDto(
    Long id,
    Long taskId,
    String taskNo,
    String taskName,
    Long robotId,
    String robotName,
    String level,
    String message,
    LocalDateTime createdAt
) {
}
