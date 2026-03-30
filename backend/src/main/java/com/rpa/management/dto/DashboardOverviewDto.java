package com.rpa.management.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardOverviewDto(
    long totalTasks,
    long runningTasks,
    long totalRobots,
    BigDecimal successRate,
    List<TrendPointDto> executionTrend,
    List<StatusCountDto> taskStatusDistribution,
    List<RecentTaskDto> recentTasks
) {
    public record TrendPointDto(String name, long executionCount, long successCount) {
    }

    public record StatusCountDto(String name, long value) {
    }

    public record RecentTaskDto(Long id, String taskNo, String name, String type, String status, Integer progress, String createdAt) {
    }
}
