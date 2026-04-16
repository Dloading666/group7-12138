package com.rpa.management.controller;

import com.rpa.management.dto.ApiResponse;
import com.rpa.management.dto.TaskDTO;
import com.rpa.management.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/tasks")
@Tag(name = "任务管理", description = "任务管理接口")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @Operation(summary = "创建任务")
    public ApiResponse<TaskDTO> createTask(@Valid @RequestBody TaskDTO dto, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            String userName = (String) request.getAttribute("username");
            return ApiResponse.success("创建任务成功", taskService.createTask(dto, userId, userName));
        } catch (Exception ex) {
            log.error("Failed to create task", ex);
            return ApiResponse.error(400, ex.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新任务")
    public ApiResponse<TaskDTO> updateTask(@PathVariable Long id, @Valid @RequestBody TaskDTO dto) {
        try {
            return ApiResponse.success("更新任务成功", taskService.updateTask(id, dto));
        } catch (Exception ex) {
            log.error("Failed to update task {}", id, ex);
            return ApiResponse.error(400, ex.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除任务")
    public ApiResponse<Void> deleteTask(@PathVariable Long id) {
        try {
            taskService.deleteTask(id);
            return ApiResponse.success("删除任务成功", null);
        } catch (Exception ex) {
            log.error("Failed to delete task {}", id, ex);
            return ApiResponse.error(400, ex.getMessage());
        }
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除任务")
    public ApiResponse<Void> deleteTasks(@RequestBody List<Long> ids) {
        try {
            taskService.deleteTasks(ids);
            return ApiResponse.success("批量删除成功", null);
        } catch (Exception ex) {
            log.error("Failed to batch delete tasks", ex);
            return ApiResponse.error(400, ex.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取任务详情")
    public ApiResponse<TaskDTO> getTaskById(@PathVariable Long id) {
        try {
            return ApiResponse.success(taskService.getTaskById(id));
        } catch (Exception ex) {
            log.error("Failed to load task {}", id, ex);
            return ApiResponse.error(404, ex.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "分页查询任务")
    public ApiResponse<Page<TaskDTO>> getTasksByPage(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long robotId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        String nameFilter = firstNonBlank(name, keyword);
        LocalDateTime startDateTime = parseStart(firstNonBlank(startTime, startDate));
        LocalDateTime endDateTime = parseEnd(firstNonBlank(endTime, endDate));
        return ApiResponse.success(taskService.getTasksByPage(
                nameFilter,
                type,
                status,
                priority,
                null,
                robotId,
                startDateTime,
                endDateTime,
                page,
                size
        ));
    }

    @GetMapping("/all")
    @Operation(summary = "获取全部任务")
    public ApiResponse<List<TaskDTO>> getAllTasks() {
        return ApiResponse.success(taskService.getAllTasks());
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "启动任务")
    public ApiResponse<TaskDTO> startTask(@PathVariable Long id) {
        try {
            return ApiResponse.success("任务已启动", taskService.startTask(id));
        } catch (Exception ex) {
            log.error("Failed to start task {}", id, ex);
            return ApiResponse.error(400, ex.getMessage());
        }
    }

    @PostMapping("/{id}/stop")
    @Operation(summary = "停止任务")
    public ApiResponse<TaskDTO> stopTask(@PathVariable Long id) {
        try {
            return ApiResponse.success("任务已停止", taskService.stopTask(id));
        } catch (Exception ex) {
            log.error("Failed to stop task {}", id, ex);
            return ApiResponse.error(400, ex.getMessage());
        }
    }

    @PutMapping("/{id}/progress")
    @Operation(summary = "更新任务进度")
    public ApiResponse<TaskDTO> updateProgress(@PathVariable Long id, @RequestParam Integer progress) {
        try {
            return ApiResponse.success(taskService.updateProgress(id, progress));
        } catch (Exception ex) {
            log.error("Failed to update task progress {}", id, ex);
            return ApiResponse.error(400, ex.getMessage());
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "获取任务统计")
    public ApiResponse<TaskService.TaskStats> getTaskStats() {
        return ApiResponse.success(taskService.getTaskStats());
    }

    private LocalDateTime parseStart(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.contains("T") ? LocalDateTime.parse(value) : LocalDateTime.parse(value + "T00:00:00");
    }

    private LocalDateTime parseEnd(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.contains("T") ? LocalDateTime.parse(value) : LocalDateTime.parse(value + "T23:59:59");
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
}
