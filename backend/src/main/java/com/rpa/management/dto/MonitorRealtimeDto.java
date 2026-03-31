package com.rpa.management.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MonitorRealtimeDto(
    long totalTasks,
    long runningTasks,
    long onlineRobots,
    long busyRobots,
    long executionLogCount24h,
    long operationLogCount24h,
    List<StatusCountDto> taskStatusDistribution,
    List<StatusCountDto> robotStatusDistribution,
    List<MonitorExecutionLogDto> recentExecutionLogs,
    List<MonitorOperationLogDto> recentOperationLogs,
    LocalDateTime generatedAt
) {
    public record StatusCountDto(String name, long value) {
    }
}
