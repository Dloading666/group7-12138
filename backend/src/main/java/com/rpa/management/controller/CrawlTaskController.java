package com.rpa.management.controller;

import com.rpa.management.dto.ApiResponse;
import com.rpa.management.dto.CreateCrawlTaskRequest;
import com.rpa.management.dto.TaskDTO;
import com.rpa.management.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/crawl")
@Tag(name = "真实网站采集", description = "真实网站采集任务创建接口")
public class CrawlTaskController {

    private final TaskService taskService;

    @PostMapping("/task")
    @Operation(summary = "创建真实网站采集任务")
    public ApiResponse<TaskDTO> createCrawlTask(@Valid @RequestBody CreateCrawlTaskRequest request,
                                                HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            String userName = (String) httpRequest.getAttribute("username");
            TaskDTO task = taskService.createCrawlTask(request, userId, userName);
            if ("immediate".equalsIgnoreCase(request.getExecuteType() == null ? "immediate" : request.getExecuteType())) {
                task = taskService.startTask(task.getId());
            }
            return ApiResponse.success("真实网站采集任务已创建", task);
        } catch (Exception ex) {
            log.error("Failed to create crawl task", ex);
            return ApiResponse.error(400, ex.getMessage());
        }
    }
}
