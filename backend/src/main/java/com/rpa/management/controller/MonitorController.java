package com.rpa.management.controller;

import com.rpa.management.dto.ApiResponse;
import com.rpa.management.dto.TaskDTO;
import com.rpa.management.service.RobotService;
import com.rpa.management.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 执行监控控制器
 */
@Slf4j
@Tag(name = "执行监控", description = "实时监控和统计接口")
@RestController
@RequestMapping("/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final TaskService taskService;
    private final RobotService robotService;
    
    /**
     * 获取实时监控统计数据
     */
    @Operation(summary = "获取监控统计", description = "获取实时监控统计数据")
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getMonitorStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 获取任务统计
            TaskService.TaskStats taskStats = taskService.getTaskStats();
            stats.put("running", taskStats.getRunning());
            stats.put("pending", taskStats.getPending());
            stats.put("completed", taskStats.getCompleted());
            stats.put("failed", taskStats.getFailed());
            stats.put("total", taskStats.getTotal());
            
            // 计算平均耗时
            stats.put("avgDuration", taskStats.getAvgDuration());
            
            // 在线机器人数量
            RobotService.RobotStats robotStats = robotService.getRobotStats();
            stats.put("onlineRobots", robotStats.getOnline());
            
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("获取监控统计失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 获取正在执行的任务列表
     */
    @Operation(summary = "获取执行中任务", description = "获取正在执行的任务列表")
    @GetMapping("/running-tasks")
    public ApiResponse<List<TaskDTO>> getRunningTasks(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<TaskDTO> tasks = taskService.getRunningTasks(limit);
            return ApiResponse.success(tasks);
        } catch (Exception e) {
            log.error("获取执行中任务失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 获取系统资源使用情况
     */
    @Operation(summary = "获取系统资源", description = "获取系统资源使用情况")
    @GetMapping("/system-resources")
    public ApiResponse<Map<String, Object>> getSystemResources() {
        try {
            Map<String, Object> resources = new HashMap<>();
            
            // 获取系统资源使用情况
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            // 内存使用率
            double memoryUsage = (double) usedMemory / maxMemory * 100;
            resources.put("memory", Math.round(memoryUsage));
            
            // CPU使用率（这里使用简单的模拟，实际可以使用操作系统API）
            resources.put("cpu", 35);
            
            // 磁盘使用率（模拟）
            resources.put("disk", 45);
            
            // 网络带宽（模拟）
            resources.put("network", 20);
            
            return ApiResponse.success(resources);
        } catch (Exception e) {
            log.error("获取系统资源失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 获取任务执行详情
     */
    @Operation(summary = "获取任务执行详情", description = "获取指定任务的执行详情")
    @GetMapping("/task/{taskId}/detail")
    public ApiResponse<Map<String, Object>> getTaskExecutionDetail(
            @PathVariable Long taskId) {
        try {
            Map<String, Object> detail = taskService.getTaskExecutionDetail(taskId);
            return ApiResponse.success(detail);
        } catch (Exception e) {
            log.error("获取任务执行详情失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
}
