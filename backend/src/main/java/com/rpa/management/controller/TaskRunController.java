package com.rpa.management.controller;

import com.rpa.management.dto.AiAnalysisMessageDTO;
import com.rpa.management.dto.AiAnalysisQuestionRequest;
import com.rpa.management.dto.ApiResponse;
import com.rpa.management.dto.CrawlResultDTO;
import com.rpa.management.dto.ExecutionLogDTO;
import com.rpa.management.dto.TaskRunDTO;
import com.rpa.management.service.CrawlResultService;
import com.rpa.management.service.ExecutionLogService;
import com.rpa.management.service.TaskRunAnalysisService;
import com.rpa.management.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "任务运行", description = "任务运行历史、运行详情与运行内置分析接口")
public class TaskRunController {

    private final TaskService taskService;
    private final TaskRunAnalysisService taskRunAnalysisService;
    private final ExecutionLogService executionLogService;
    private final CrawlResultService crawlResultService;

    @GetMapping("/tasks/{id}/runs")
    @Operation(summary = "获取任务运行历史")
    public ApiResponse<List<TaskRunDTO>> getTaskRuns(@PathVariable Long id) {
        return ApiResponse.success(taskService.getTaskRuns(id));
    }

    @GetMapping("/task-runs/{runId}")
    @Operation(summary = "获取任务运行详情")
    public ApiResponse<TaskRunDTO> getTaskRun(@PathVariable Long runId) {
        return ApiResponse.success(taskRunAnalysisService.getRun(runId));
    }

    @GetMapping("/task-runs/{runId}/logs")
    @Operation(summary = "获取任务运行日志")
    public ApiResponse<List<ExecutionLogDTO>> getRunLogs(@PathVariable Long runId) {
        return ApiResponse.success(executionLogService.getLogsByTaskRunId(runId));
    }

    @GetMapping("/task-runs/{runId}/crawl-result")
    @Operation(summary = "获取任务运行抓取结果")
    public ApiResponse<CrawlResultDTO> getRunCrawlResult(@PathVariable Long runId) {
        return ApiResponse.success(crawlResultService.getResultByTaskRunId(runId));
    }

    @GetMapping("/task-runs/{runId}/analysis/messages")
    @Operation(summary = "获取任务运行分析消息")
    public ApiResponse<List<AiAnalysisMessageDTO>> getAnalysisMessages(@PathVariable Long runId) {
        return ApiResponse.success(taskRunAnalysisService.getMessages(runId));
    }

    @PostMapping("/task-runs/{runId}/analysis/messages")
    @Operation(summary = "发送任务运行分析消息")
    public ApiResponse<List<AiAnalysisMessageDTO>> askAnalysis(@PathVariable Long runId,
                                                               @RequestBody AiAnalysisQuestionRequest request) {
        return ApiResponse.success(taskRunAnalysisService.askQuestion(runId, request.getQuestion()));
    }
}
