package com.rpa.management.service;

import com.rpa.management.common.enums.RobotStatus;
import com.rpa.management.common.enums.TaskStatus;
import com.rpa.management.dto.MonitorExecutionLogDto;
import com.rpa.management.dto.MonitorOperationLogDto;
import com.rpa.management.dto.StatisticsReportDto;
import com.rpa.management.entity.OperationLog;
import com.rpa.management.entity.Robot;
import com.rpa.management.entity.Task;
import com.rpa.management.repository.ExecutionLogRepository;
import com.rpa.management.repository.OperationLogRepository;
import com.rpa.management.repository.RobotRepository;
import com.rpa.management.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final TaskRepository taskRepository;
    private final RobotRepository robotRepository;
    private final ExecutionLogRepository executionLogRepository;
    private final OperationLogRepository operationLogRepository;
    private final MonitorService monitorService;

    @Transactional(readOnly = true)
    public StatisticsReportDto report() {
        List<Task> tasks = taskRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Robot> robots = robotRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        List<OperationLog> operationLogs = operationLogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        long totalTasks = tasks.size();
        long runningTasks = tasks.stream().filter(task -> task.getStatus() == TaskStatus.RUNNING).count();
        long completedTasks = tasks.stream().filter(task -> task.getStatus() == TaskStatus.COMPLETED).count();
        long failedTasks = tasks.stream().filter(task -> task.getStatus() == TaskStatus.FAILED).count();
        long totalRobots = robots.size();
        long onlineRobots = robots.stream().filter(robot -> robot.getStatus() == RobotStatus.ONLINE).count();
        long busyRobots = robots.stream().filter(robot -> robot.getStatus() == RobotStatus.BUSY).count();
        BigDecimal successRate = totalTasks == 0
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(completedTasks * 100.0 / totalTasks).setScale(1, RoundingMode.HALF_UP);

        LocalDate today = LocalDate.now();
        List<StatisticsReportDto.TrendPointDto> taskTrend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            long executionCount = tasks.stream()
                .filter(task -> task.getCreatedAt() != null && task.getCreatedAt().toLocalDate().equals(day))
                .count();
            long successCount = tasks.stream()
                .filter(task -> task.getCreatedAt() != null && task.getCreatedAt().toLocalDate().equals(day))
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                .count();
            String name = day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.CHINA);
            taskTrend.add(new StatisticsReportDto.TrendPointDto(name, executionCount, successCount));
        }

        Map<TaskStatus, Long> taskStatusDistribution = new EnumMap<>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) {
            taskStatusDistribution.put(status, tasks.stream().filter(task -> task.getStatus() == status).count());
        }

        Map<RobotStatus, Long> robotStatusDistribution = new EnumMap<>(RobotStatus.class);
        for (RobotStatus status : RobotStatus.values()) {
            robotStatusDistribution.put(status, robots.stream().filter(robot -> robot.getStatus() == status).count());
        }

        List<StatisticsReportDto.RobotPerformanceDto> topRobots = robots.stream()
            .sorted(Comparator
                .comparing((Robot robot) -> robot.getTaskCount() == null ? 0 : robot.getTaskCount())
                .reversed()
                .thenComparing(robot -> robot.getSuccessRate() == null ? BigDecimal.ZERO : robot.getSuccessRate(), Comparator.reverseOrder()))
            .limit(5)
            .map(robot -> new StatisticsReportDto.RobotPerformanceDto(
                robot.getId(),
                robot.getName(),
                robot.getStatus().name(),
                robot.getTaskCount(),
                robot.getSuccessRate()
            ))
            .toList();

        List<StatisticsReportDto.TaskSummaryDto> recentTasks = tasks.stream()
            .limit(10)
            .map(task -> new StatisticsReportDto.TaskSummaryDto(
                task.getId(),
                task.getTaskNo(),
                task.getName(),
                task.getType(),
                task.getStatus().name(),
                task.getProgress(),
                task.getCreatedAt() == null ? null : task.getCreatedAt().toString()
            ))
            .toList();

        List<MonitorExecutionLogDto> recentExecutionLogs = monitorService.realtime().recentExecutionLogs();
        List<MonitorOperationLogDto> recentOperationLogs = operationLogs.stream()
            .limit(10)
            .map(log -> new MonitorOperationLogDto(
                log.getId(),
                log.getUserId(),
                log.getUsername(),
                log.getOperation(),
                log.getMethod(),
                log.getParams(),
                log.getIp(),
                log.getStatus(),
                log.getErrorMsg(),
                log.getDuration(),
                log.getCreatedAt()
            ))
            .toList();

        List<StatisticsReportDto.StatusCountDto> executionLevelDistribution = executionLogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
            .stream()
            .collect(java.util.stream.Collectors.groupingBy(
                log -> log.getLevel() == null ? "UNKNOWN" : log.getLevel().toUpperCase(Locale.ROOT),
                java.util.stream.Collectors.counting()
            ))
            .entrySet().stream()
            .map(entry -> new StatisticsReportDto.StatusCountDto(entry.getKey(), entry.getValue()))
            .toList();

        return new StatisticsReportDto(
            totalTasks,
            runningTasks,
            completedTasks,
            failedTasks,
            totalRobots,
            onlineRobots,
            busyRobots,
            successRate,
            taskTrend,
            taskStatusDistribution.entrySet().stream()
                .map(entry -> new StatisticsReportDto.StatusCountDto(entry.getKey().name(), entry.getValue()))
                .toList(),
            robotStatusDistribution.entrySet().stream()
                .map(entry -> new StatisticsReportDto.StatusCountDto(entry.getKey().name(), entry.getValue()))
                .toList(),
            executionLevelDistribution,
            topRobots,
            recentTasks,
            recentExecutionLogs,
            recentOperationLogs,
            LocalDateTime.now()
        );
    }
}
