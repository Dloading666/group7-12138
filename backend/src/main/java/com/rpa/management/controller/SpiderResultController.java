package com.rpa.management.controller;

import com.alibaba.fastjson2.JSONObject;
import com.rpa.management.client.SpiderApiClient;
import com.rpa.management.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * spider 结果查询接口
 * GET /api/spider/result/{taskId}
 * GET /api/spider/results
 */
@Slf4j
@RestController
@RequestMapping("/spider")
@RequiredArgsConstructor
public class SpiderResultController {

    private final SpiderApiClient spiderApiClient;

    /**
     * 获取单个 spider 任务结果
     */
    @GetMapping("/result/{taskId}")
    public ApiResponse<JSONObject> getSpiderResult(@PathVariable String taskId) {
        try {
            JSONObject result = spiderApiClient.getSpiderResultFromExc(taskId);
            if (result != null) {
                return ApiResponse.success(result);
            }
            return ApiResponse.error("未找到任务结果");
        } catch (Exception e) {
            log.error("获取 spider 结果失败: taskId={}, error={}", taskId, e.getMessage());
            return ApiResponse.error("获取 spider 结果失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有 spider 任务结果列表
     */
    @GetMapping("/results")
    public ApiResponse<JSONObject> getSpiderResultsList() {
        try {
            JSONObject result = spiderApiClient.getSpiderResultsList();
            if (result != null) {
                return ApiResponse.success(result);
            }
            return ApiResponse.error("未找到任务列表");
        } catch (Exception e) {
            log.error("获取 spider 任务列表失败: error={}", e.getMessage());
            return ApiResponse.error("获取 spider 任务列表失败: " + e.getMessage());
        }
    }
}
