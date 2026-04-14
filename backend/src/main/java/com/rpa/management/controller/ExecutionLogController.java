package com.rpa.management.controller;

import com.rpa.management.dto.ApiResponse;
import com.rpa.management.dto.ExecutionLogDTO;
import com.rpa.management.service.ExecutionLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 执行日志控制器
 */
@Slf4j
@Tag(name = "执行日志", description = "执行日志的查询和管理接口")
@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
public class ExecutionLogController {
    
    private final ExecutionLogService executionLogService;
    
    /**
     * 分页查询日志
     */
    @Operation(summary = "分页查询日志", description = "分页查询执行日志，支持条件筛选")
    @GetMapping
    public ApiResponse<Page<ExecutionLogDTO>> getLogsByPage(
            @Parameter(description = "日志级别") @RequestParam(required = false) String level,
            @Parameter(description = "任务ID") @RequestParam(required = false) Long taskId,
            @Parameter(description = "任务编号") @RequestParam(required = false) String taskCode,
            @Parameter(description = "机器人ID") @RequestParam(required = false) Long robotId,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "50") int size) {
        
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;
        
        if (startTime != null && !startTime.isEmpty()) {
            startDateTime = LocalDateTime.parse(startTime + "T00:00:00");
        }
        if (endTime != null && !endTime.isEmpty()) {
            endDateTime = LocalDateTime.parse(endTime + "T23:59:59");
        }
        
        Page<ExecutionLogDTO> logPage = executionLogService.getLogsByPage(
                level, taskId, taskCode, robotId, startDateTime, endDateTime, page, size
        );
        return ApiResponse.success(logPage);
    }
    
    /**
     * 获取任务的所有日志
     */
    @Operation(summary = "获取任务日志", description = "获取指定任务的所有执行日志")
    @GetMapping("/task/{taskId}")
    public ApiResponse<List<ExecutionLogDTO>> getLogsByTaskId(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        List<ExecutionLogDTO> logs = executionLogService.getLogsByTaskId(taskId);
        return ApiResponse.success(logs);
    }
    
    /**
     * 清空所有日志
     */
    @Operation(summary = "清空所有日志", description = "清空所有执行日志")
    @DeleteMapping("/clear")
    public ApiResponse<Void> clearAllLogs() {
        try {
            executionLogService.clearAllLogs();
            return ApiResponse.success("日志已清空", null);
        } catch (Exception e) {
            log.error("清空日志失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 清空指定天数之前的日志
     */
    @Operation(summary = "清空历史日志", description = "清空指定天数之前的执行日志")
    @DeleteMapping("/clear-before/{days}")
    public ApiResponse<Void> clearLogsBeforeDays(
            @Parameter(description = "天数") @PathVariable int days) {
        try {
            executionLogService.clearLogsBeforeDays(days);
            return ApiResponse.success("历史日志已清空", null);
        } catch (Exception e) {
            log.error("清空历史日志失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
}
