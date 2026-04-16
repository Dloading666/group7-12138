package com.spider.exc.controller;

import com.spider.exc.domain.entity.IndicatorQuery;
import com.spider.exc.dto.SpiderTaskRequest;
import com.spider.exc.dto.SpiderTaskResponse;
import com.spider.exc.dto.SpiderTaskResultDTO;
import com.spider.exc.service.SpiderTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 接收 project-gl 调度的爬虫任务
 */
@Slf4j
@RestController
@RequestMapping("/api/spider/task")
@RequiredArgsConstructor
public class SpiderTaskController {

    private final SpiderTaskService spiderTaskService;

    /**
     * 接收 project-gl 提交的任务
     * POST /api/spider/task/submit
     */
    @PostMapping("/submit")
    public SpiderTaskResponse submitTask(@RequestBody SpiderTaskRequest request) {
        try {
            Long id = spiderTaskService.submitTask(request);
            return SpiderTaskResponse.success(request.getTaskId(), "pending");
        } catch (Exception e) {
            log.error("提交任务失败: {}", e.getMessage(), e);
            return SpiderTaskResponse.failed(request.getTaskId(), e.getMessage());
        }
    }

    /**
     * 查询任务状态
     * GET /api/spider/task/{taskId}/status
     */
    @GetMapping("/{taskId}/status")
    public SpiderTaskResponse getTaskStatus(@PathVariable String taskId) {
        try {
            String status = spiderTaskService.getTaskStatus(taskId);
            return SpiderTaskResponse.success(taskId, status);
        } catch (Exception e) {
            log.error("查询任务状态失败: {}", e.getMessage(), e);
            return SpiderTaskResponse.failed(taskId, e.getMessage());
        }
    }

    /**
     * 获取四步指标结果
     * GET /api/spider/task/{taskId}/result
     */
    @GetMapping("/{taskId}/result")
    public SpiderTaskResponse getTaskResult(@PathVariable String taskId) {
        try {
            IndicatorQuery result = spiderTaskService.getTaskResult(taskId);
            return SpiderTaskResponse.success(taskId, "completed", result);
        } catch (Exception e) {
            log.error("获取任务结果失败: {}", e.getMessage(), e);
            return SpiderTaskResponse.failed(taskId, e.getMessage());
        }
    }

    /**
     * project-gl 回调通知（spider_exc 内部不使用，供 project-gl 回调用）
     * POST /api/spider/task/callback
     */
    @PostMapping("/callback")
    public void handleCallback(@RequestBody Map<String, Object> payload) {
        log.info("收到回调: {}", payload);
        // spider_exc 侧的任务状态通过 SpiderTaskService 内部管理
        // 此接口仅作日志记录
    }

    /**
     * 获取所有 spider 任务列表（带结果）
     * GET /api/spider/task/results
     */
    @GetMapping("/results")
    public SpiderTaskResponse getTaskResults() {
        try {
            java.util.List<SpiderTaskResultDTO> results = spiderTaskService.getTaskList();
            return SpiderTaskResponse.success("list", "success", results);
        } catch (Exception e) {
            log.error("获取任务列表失败: {}", e.getMessage(), e);
            return SpiderTaskResponse.failed("list", e.getMessage());
        }
    }
}
