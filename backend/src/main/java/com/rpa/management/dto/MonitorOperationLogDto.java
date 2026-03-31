package com.rpa.management.dto;

import java.time.LocalDateTime;

public record MonitorOperationLogDto(
    Long id,
    Long userId,
    String username,
    String operation,
    String method,
    String params,
    String ip,
    String status,
    String errorMsg,
    Long duration,
    LocalDateTime createdAt
) {
}
