package com.rpa.management.controller;

import com.rpa.management.dto.ApiResponse;
import com.rpa.management.dto.CrawlResultDTO;
import com.rpa.management.repository.TaskRepository;
import com.rpa.management.service.CrawlResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据查询接口，面向真实网站采集结果
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/data")
@Tag(name = "数据查询", description = "真实网站采集结果查询")
public class DataQueryController {

    private final CrawlResultService crawlResultService;
    private final TaskRepository taskRepository;

    @GetMapping("/query")
    @Operation(summary = "分页查询真实网站采集结果")
    public ApiResponse<Page<CrawlResultDTO>> queryData(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String taskId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        LocalDateTime startDateTime = parseStart(startDate);
        LocalDateTime endDateTime = parseEnd(endDate);
        return ApiResponse.success(
                crawlResultService.getResults(keyword, taskId, status, startDateTime, endDateTime, page, size)
        );
    }

    /**
     * 任务状态分布统计
     * 返回：{ pending: N, running: N, completed: N, failed: N, total: N }
     */
    @GetMapping("/stats/status")
    @Operation(summary = "任务状态统计")
    public ApiResponse<Map<String, Long>> getTaskStatusStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", taskRepository.countAll());
        stats.put("running", taskRepository.countRunning());
        stats.put("completed", taskRepository.countCompleted());
        stats.put("failed", taskRepository.countFailed());
        long pending = stats.get("total") - stats.get("running") - stats.get("completed") - stats.get("failed");
        stats.put("pending", Math.max(0, pending));
        return ApiResponse.success(stats);
    }

    /**
     * 任务类型分布统计
     * 返回：[ { type: "data-collection", count: N }, ... ]
     */
    @GetMapping("/stats/type")
    @Operation(summary = "任务类型统计")
    public ApiResponse<List<Map<String, Object>>> getTaskTypeStats() {
        List<Object[]> rows = taskRepository.findAll().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        t -> t.getType() != null ? t.getType() : "default",
                        java.util.stream.Collectors.counting()
                ))
                .entrySet().stream()
                .map(e -> new Object[]{e.getKey(), e.getValue()})
                .collect(java.util.stream.Collectors.toList());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("type", row[0]);
            item.put("count", row[1]);
            result.add(item);
        }
        return ApiResponse.success(result);
    }

    /**
     * 近 N 天任务趋势（按天统计任务数）
     */
    @GetMapping("/stats/trend")
    @Operation(summary = "任务趋势统计")
    public ApiResponse<List<Map<String, Object>>> getTaskTrend(
            @RequestParam(defaultValue = "7") int days) {
        LocalDateTime start = LocalDate.now().minusDays(days - 1).atStartOfDay();
        Map<String, List<com.rpa.management.entity.Task>> grouped = taskRepository.findAll().stream()
                .filter(t -> t.getCreateTime() != null && !t.getCreateTime().isBefore(start))
                .collect(java.util.stream.Collectors.groupingBy(
                        t -> t.getCreateTime().toLocalDate().toString()
                ));

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).toString();
            List<com.rpa.management.entity.Task> tasks = grouped.getOrDefault(date, List.of());
            Map<String, Object> item = new HashMap<>();
            item.put("date", date);
            item.put("count", (long) tasks.size());
            item.put("total", (long) tasks.size());
            item.put("completed", tasks.stream().filter(t -> "completed".equals(t.getStatus())).count());
            item.put("failed", tasks.stream().filter(t -> "failed".equals(t.getStatus())).count());
            result.add(item);
        }
        return ApiResponse.success(result);
    }

    /**
     * 概览统计（任务总数、成功率、机器人数等）
     */
    @GetMapping("/stats/overview")
    @Operation(summary = "系统概览统计")
    public ApiResponse<Map<String, Object>> getOverviewStats() {
        long total = taskRepository.countAll();
        long completed = taskRepository.countCompleted();
        long failed = taskRepository.countFailed();
        long running = taskRepository.countRunning();

        double successRate = total > 0
                ? Math.round((double) completed / total * 10000.0) / 100.0
                : 0.0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTasks", total);
        stats.put("completedTasks", completed);
        stats.put("failedTasks", failed);
        stats.put("runningTasks", running);
        stats.put("successRate", successRate);
        return ApiResponse.success(stats);
    }

    private LocalDateTime parseStart(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value + "T00:00:00");
    }

    private LocalDateTime parseEnd(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value + "T23:59:59");
    }
}
