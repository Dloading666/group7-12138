package com.rpa.management.service;

import com.rpa.management.common.enums.TaskStatus;
import com.rpa.management.dto.DashboardOverviewDto;
import com.rpa.management.entity.Task;
import com.rpa.management.repository.RobotRepository;
import com.rpa.management.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TaskRepository taskRepository;
    private final RobotRepository robotRepository;

    @Transactional(readOnly = true)
    public DashboardOverviewDto overview() {
        List<Task> tasks = taskRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        long totalTasks = tasks.size();
        long runningTasks = tasks.stream().filter(task -> task.getStatus() == TaskStatus.RUNNING).count();
        long totalRobots = robotRepository.count();
        long completedTasks = tasks.stream().filter(task -> task.getStatus() == TaskStatus.COMPLETED).count();
        BigDecimal successRate = totalTasks == 0
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(completedTasks * 100.0 / totalTasks).setScale(1, RoundingMode.HALF_UP);

        LocalDate today = LocalDate.now();
        List<DashboardOverviewDto.TrendPointDto> trend = new ArrayList<>();
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
            trend.add(new DashboardOverviewDto.TrendPointDto(name, executionCount, successCount));
        }

        Map<TaskStatus, Long> statusMap = new EnumMap<>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) {
            statusMap.put(status, tasks.stream().filter(task -> task.getStatus() == status).count());
        }
        List<DashboardOverviewDto.StatusCountDto> distribution = statusMap.entrySet().stream()
            .map(entry -> new DashboardOverviewDto.StatusCountDto(entry.getKey().name(), entry.getValue()))
            .toList();

        List<DashboardOverviewDto.RecentTaskDto> recentTasks = tasks.stream().limit(5)
            .map(task -> new DashboardOverviewDto.RecentTaskDto(
                task.getId(),
                task.getTaskNo(),
                task.getName(),
                task.getType(),
                task.getStatus().name(),
                task.getProgress(),
                task.getCreatedAt() == null ? null : task.getCreatedAt().toString()))
            .toList();

        return new DashboardOverviewDto(totalTasks, runningTasks, totalRobots, successRate, trend, distribution, recentTasks);
    }
}
