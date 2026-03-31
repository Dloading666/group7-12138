package com.rpa.management.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record StatisticsReportDto(
    long totalTasks,
    long runningTasks,
    long completedTasks,
    long failedTasks,
    long totalRobots,
    long onlineRobots,
    long busyRobots,
    BigDecimal successRate,
    List<TrendPointDto> taskTrend,
    List<StatusCountDto> taskStatusDistribution,
    List<StatusCountDto> robotStatusDistribution,
    List<StatusCountDto> executionLevelDistribution,
    List<RobotPerformanceDto> topRobots,
    List<TaskSummaryDto> recentTasks,
    List<MonitorExecutionLogDto> recentExecutionLogs,
    List<MonitorOperationLogDto> recentOperationLogs,
    LocalDateTime generatedAt
) {
    public record TrendPointDto(String name, long executionCount, long successCount) {
    }

    public record StatusCountDto(String name, long value) {
    }

    public record RobotPerformanceDto(Long id, String name, String status, Integer taskCount, BigDecimal successRate) {
    }

    public record TaskSummaryDto(Long id, String taskNo, String name, String type, String status, Integer progress, String createdAt) {
    }
}
