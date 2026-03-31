package com.rpa.management.controller;

import com.rpa.management.common.ApiResponse;
import com.rpa.management.dto.StatisticsReportDto;
import com.rpa.management.security.PermissionCodes;
import com.rpa.management.service.StatisticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/query")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).STATISTICS_VIEW)")
    public ApiResponse<StatisticsReportDto> query() {
        return ApiResponse.success(statisticsService.report());
    }

    @GetMapping("/report")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).STATISTICS_VIEW)")
    public ApiResponse<StatisticsReportDto> report() {
        return ApiResponse.success(statisticsService.report());
    }
}
