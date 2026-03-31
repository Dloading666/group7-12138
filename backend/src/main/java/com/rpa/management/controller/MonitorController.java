package com.rpa.management.controller;

import com.rpa.management.common.ApiResponse;
import com.rpa.management.common.PageResponse;
import com.rpa.management.dto.MonitorExecutionLogDto;
import com.rpa.management.dto.MonitorOperationLogDto;
import com.rpa.management.dto.MonitorRealtimeDto;
import com.rpa.management.security.PermissionCodes;
import com.rpa.management.service.MonitorService;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/monitor")
public class MonitorController {

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping("/realtime")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).MONITOR_VIEW)")
    public ApiResponse<MonitorRealtimeDto> realtime() {
        return ApiResponse.success(monitorService.realtime());
    }

    @GetMapping("/logs")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).MONITOR_LOG)")
    public ApiResponse<PageResponse<MonitorExecutionLogDto>> executionLogs(@RequestParam(required = false) String keyword,
                                                                           @RequestParam(required = false) String level,
                                                                           @RequestParam(required = false) Long taskId,
                                                                           @RequestParam(required = false) String taskNo,
                                                                           @RequestParam(required = false) Long robotId,
                                                                           @RequestParam(defaultValue = "1") int page,
                                                                           @RequestParam(defaultValue = "10") int size) {
        Page<MonitorExecutionLogDto> result = monitorService.executionLogs(keyword, level, taskId, taskNo, robotId, page, size);
        return ApiResponse.success(new PageResponse<>(result.getContent(), result.getTotalElements(), page, size));
    }

    @GetMapping("/operation-logs")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).MONITOR_LOG)")
    public ApiResponse<PageResponse<MonitorOperationLogDto>> operationLogs(@RequestParam(required = false) String keyword,
                                                                           @RequestParam(required = false) String username,
                                                                           @RequestParam(required = false) String status,
                                                                           @RequestParam(defaultValue = "1") int page,
                                                                           @RequestParam(defaultValue = "10") int size) {
        Page<MonitorOperationLogDto> result = monitorService.operationLogs(keyword, username, status, page, size);
        return ApiResponse.success(new PageResponse<>(result.getContent(), result.getTotalElements(), page, size));
    }
}
