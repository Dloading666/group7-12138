package com.rpa.management.controller;

import com.rpa.management.common.ApiResponse;
import com.rpa.management.common.PaginationUtils;
import com.rpa.management.common.PageResponse;
import com.rpa.management.dto.TaskDto;
import com.rpa.management.dto.TaskStatusChangeRequest;
import com.rpa.management.dto.TaskUpsertRequest;
import com.rpa.management.security.PermissionCodes;
import com.rpa.management.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).TASK_VIEW)")
    public ApiResponse<PageResponse<TaskDto>> list(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(PaginationUtils.page(taskService.listAll(), page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).TASK_VIEW)")
    public ApiResponse<TaskDto> detail(@PathVariable Long id) {
        return ApiResponse.success(taskService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).TASK_CREATE)")
    public ApiResponse<TaskDto> create(@Valid @RequestBody TaskUpsertRequest request) {
        return ApiResponse.success(taskService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).TASK_UPDATE)")
    public ApiResponse<TaskDto> update(@PathVariable Long id, @Valid @RequestBody TaskUpsertRequest request) {
        return ApiResponse.success(taskService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).TASK_DELETE)")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ApiResponse.success("OK", null);
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).TASK_START)")
    public ApiResponse<TaskDto> start(@PathVariable Long id) {
        return ApiResponse.success(taskService.start(id));
    }

    @PostMapping("/{id}/stop")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).TASK_STOP)")
    public ApiResponse<TaskDto> stop(@PathVariable Long id) {
        return ApiResponse.success(taskService.stop(id));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).TASK_UPDATE)")
    public ApiResponse<TaskDto> changeStatus(@PathVariable Long id, @Valid @RequestBody TaskStatusChangeRequest request) {
        return ApiResponse.success(taskService.changeStatus(id, request));
    }
}
