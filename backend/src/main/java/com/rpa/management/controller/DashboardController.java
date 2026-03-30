package com.rpa.management.controller;

import com.rpa.management.common.ApiResponse;
import com.rpa.management.dto.DashboardOverviewDto;
import com.rpa.management.security.PermissionCodes;
import com.rpa.management.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).DASHBOARD_VIEW)")
    public ApiResponse<DashboardOverviewDto> overview() {
        return ApiResponse.success(dashboardService.overview());
    }
}
