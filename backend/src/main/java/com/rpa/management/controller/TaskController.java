package com.rpa.management.controller;

import com.rpa.management.dto.ApiResponse;
import com.rpa.management.dto.TaskDTO;
import com.rpa.management.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 任务管理控制器
 */
@Slf4j
@Tag(name = "任务管理", description = "任务的增删改查接口")
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {
    
    private final TaskService taskService;
    
    /**
     * 创建任务
     */
    @Operation(summary = "创建任务", description = "创建新的任务")
    @PostMapping
    public ApiResponse<TaskDTO> createTask(@Valid @RequestBody TaskDTO dto, HttpServletRequest request) {
        try {
            // 获取当前用户信息
            Long userId = (Long) request.getAttribute("userId");
            String userName = (String) request.getAttribute("username");
            
            TaskDTO task = taskService.createTask(dto, userId, userName);
            return ApiResponse.success("创建任务成功", task);
        } catch (Exception e) {
            log.error("创建任务失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 更新任务
     */
    @Operation(summary = "更新任务", description = "更新指定任务的信息")
    @PutMapping("/{id}")
    public ApiResponse<TaskDTO> updateTask(
            @Parameter(description = "任务ID") @PathVariable Long id,
            @Valid @RequestBody TaskDTO dto) {
        try {
            TaskDTO task = taskService.updateTask(id, dto);
            return ApiResponse.success("更新任务成功", task);
        } catch (Exception e) {
            log.error("更新任务失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 删除任务
     */
    @Operation(summary = "删除任务", description = "删除指定任务")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTask(@Parameter(description = "任务ID") @PathVariable Long id) {
        try {
            taskService.deleteTask(id);
            return ApiResponse.success("删除任务成功", null);
        } catch (Exception e) {
            log.error("删除任务失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 批量删除任务
     */
    @Operation(summary = "批量删除任务", description = "批量删除多个任务")
    @DeleteMapping("/batch")
    public ApiResponse<Void> deleteTasks(@RequestBody List<Long> ids) {
        try {
            taskService.deleteTasks(ids);
            return ApiResponse.success("批量删除成功", null);
        } catch (Exception e) {
            log.error("批量删除任务失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 根据ID查询任务
     */
    @Operation(summary = "查询任务详情", description = "根据ID查询任务详细信息")
    @GetMapping("/{id}")
    public ApiResponse<TaskDTO> getTaskById(@Parameter(description = "任务ID") @PathVariable Long id) {
        try {
            TaskDTO task = taskService.getTaskById(id);
            return ApiResponse.success(task);
        } catch (Exception e) {
            log.error("查询任务失败: {}", e.getMessage());
            return ApiResponse.error(404, e.getMessage());
        }
    }
    
    /**
     * 分页查询任务
     */
    @Operation(summary = "分页查询任务", description = "分页查询任务列表，支持条件筛选")
    @GetMapping
    public ApiResponse<Page<TaskDTO>> getTasksByPage(
            @Parameter(description = "任务名称") @RequestParam(required = false) String name,
            @Parameter(description = "任务类型") @RequestParam(required = false) String type,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "优先级") @RequestParam(required = false) String priority,
            @Parameter(description = "机器人ID") @RequestParam(required = false) Long robotId,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        
        java.time.LocalDateTime startDateTime = null;
        java.time.LocalDateTime endDateTime = null;
        
        if (startTime != null && !startTime.isEmpty()) {
            startDateTime = java.time.LocalDateTime.parse(startTime + "T00:00:00");
        }
        if (endTime != null && !endTime.isEmpty()) {
            endDateTime = java.time.LocalDateTime.parse(endTime + "T23:59:59");
        }
        
        Page<TaskDTO> taskPage = taskService.getTasksByPage(name, type, status, priority, null, robotId, startDateTime, endDateTime, page, size);
        return ApiResponse.success(taskPage);
    }
    
    /**
     * 查询所有任务
     */
    @Operation(summary = "查询所有任务", description = "获取所有任务列表")
    @GetMapping("/all")
    public ApiResponse<List<TaskDTO>> getAllTasks() {
        List<TaskDTO> tasks = taskService.getAllTasks();
        return ApiResponse.success(tasks);
    }
    
    /**
     * 启动任务
     */
    @Operation(summary = "启动任务", description = "启动指定任务")
    @PostMapping("/{id}/start")
    public ApiResponse<TaskDTO> startTask(@Parameter(description = "任务ID") @PathVariable Long id) {
        try {
            TaskDTO task = taskService.startTask(id);
            return ApiResponse.success("任务已启动", task);
        } catch (Exception e) {
            log.error("启动任务失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 停止任务
     */
    @Operation(summary = "停止任务", description = "停止指定任务")
    @PostMapping("/{id}/stop")
    public ApiResponse<TaskDTO> stopTask(@Parameter(description = "任务ID") @PathVariable Long id) {
        try {
            TaskDTO task = taskService.stopTask(id);
            return ApiResponse.success("任务已停止", task);
        } catch (Exception e) {
            log.error("停止任务失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 更新任务进度
     */
    @Operation(summary = "更新任务进度", description = "更新任务执行进度")
    @PutMapping("/{id}/progress")
    public ApiResponse<TaskDTO> updateProgress(
            @Parameter(description = "任务ID") @PathVariable Long id,
            @Parameter(description = "进度") @RequestParam Integer progress) {
        try {
            TaskDTO task = taskService.updateProgress(id, progress);
            return ApiResponse.success(task);
        } catch (Exception e) {
            log.error("更新进度失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 获取任务统计数据
     */
    @Operation(summary = "获取任务统计", description = "获取任务统计数据")
    @GetMapping("/stats")
    public ApiResponse<TaskService.TaskStats> getTaskStats() {
        TaskService.TaskStats stats = taskService.getTaskStats();
        return ApiResponse.success(stats);
    }
}
