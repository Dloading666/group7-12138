package com.rpa.management.controller;

import com.rpa.management.dto.ApiResponse;
import com.rpa.management.dto.CrawlResultDTO;
import com.rpa.management.service.CrawlResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/crawl/results")
@Tag(name = "真实网站采集结果", description = "真实网站采集结果查询接口")
public class CrawlResultController {

    private final CrawlResultService crawlResultService;

    @GetMapping
    @Operation(summary = "分页查询采集结果")
    public ApiResponse<Page<CrawlResultDTO>> getResults(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String taskId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        LocalDateTime startDateTime = parseStart(firstNonBlank(startTime, startDate));
        LocalDateTime endDateTime = parseEnd(firstNonBlank(endTime, endDate));
        return ApiResponse.success(crawlResultService.getResults(keyword, taskId, status, startDateTime, endDateTime, page, size));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "获取单个采集结果")
    public ApiResponse<CrawlResultDTO> getResult(@PathVariable String taskId) {
        return ApiResponse.success(crawlResultService.getResultByTaskId(taskId));
    }

    private LocalDateTime parseStart(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.contains("T")) {
            return LocalDateTime.parse(value);
        }
        return LocalDateTime.parse(value + "T00:00:00");
    }

    private LocalDateTime parseEnd(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.contains("T")) {
            return LocalDateTime.parse(value);
        }
        return LocalDateTime.parse(value + "T23:59:59");
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
}
