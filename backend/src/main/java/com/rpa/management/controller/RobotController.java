package com.rpa.management.controller;

import com.rpa.management.common.ApiResponse;
import com.rpa.management.common.PaginationUtils;
import com.rpa.management.common.PageResponse;
import com.rpa.management.dto.RobotDto;
import com.rpa.management.dto.RobotStatusChangeRequest;
import com.rpa.management.dto.RobotUpsertRequest;
import com.rpa.management.security.PermissionCodes;
import com.rpa.management.service.RobotService;
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
@RequestMapping("/robots")
public class RobotController {

    private final RobotService robotService;

    public RobotController(RobotService robotService) {
        this.robotService = robotService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).ROBOT_VIEW)")
    public ApiResponse<PageResponse<RobotDto>> list(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(PaginationUtils.page(robotService.listAll(), page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).ROBOT_VIEW)")
    public ApiResponse<RobotDto> detail(@PathVariable Long id) {
        return ApiResponse.success(robotService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).ROBOT_CREATE)")
    public ApiResponse<RobotDto> create(@Valid @RequestBody RobotUpsertRequest request) {
        return ApiResponse.success(robotService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).ROBOT_UPDATE)")
    public ApiResponse<RobotDto> update(@PathVariable Long id, @Valid @RequestBody RobotUpsertRequest request) {
        return ApiResponse.success(robotService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).ROBOT_DELETE)")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        robotService.delete(id);
        return ApiResponse.success("OK", null);
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).ROBOT_START)")
    public ApiResponse<RobotDto> start(@PathVariable Long id) {
        return ApiResponse.success(robotService.start(id));
    }

    @PostMapping("/{id}/stop")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).ROBOT_STOP)")
    public ApiResponse<RobotDto> stop(@PathVariable Long id) {
        return ApiResponse.success(robotService.stop(id));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).ROBOT_UPDATE)")
    public ApiResponse<RobotDto> changeStatus(@PathVariable Long id, @Valid @RequestBody RobotStatusChangeRequest request) {
        return ApiResponse.success(robotService.changeStatus(id, request));
    }
}
