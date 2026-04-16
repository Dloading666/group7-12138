package com.rpa.management.controller;

import com.rpa.management.client.SpiderApiClient;
import com.rpa.management.entity.Task;
import com.rpa.management.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 接收 spider_exc 回调的接口
 * POST /api/spider/task/callback
 */
@Slf4j
@RestController
@RequestMapping("/spider/task")
@RequiredArgsConstructor
public class SpiderCallbackController {

    private final SpiderApiClient spiderApiClient;
    private final TaskRepository taskRepository;

    /**
     * spider_exc 执行完成后回调通知 project-gl
     */
    @PostMapping("/callback")
    @Transactional
    public void handleCallback(@RequestBody Map<String, Object> payload) {
        log.info("收到 spider_exc 回调: {}", payload);

        String taskId = (String) payload.get("taskId");
        String status = (String) payload.get("status");
        String errorMessage = (String) payload.get("errorMessage");
        String enterpriseName = (String) payload.get("enterpriseName");
        Long queryId = payload.get("queryId") != null ? ((Number) payload.get("queryId")).longValue() : null;
        Long collectionId = payload.get("collectionId") != null ? ((Number) payload.get("collectionId")).longValue() : null;
        Long parsingId = payload.get("parsingId") != null ? ((Number) payload.get("parsingId")).longValue() : null;
        Long processingId = payload.get("processingId") != null ? ((Number) payload.get("processingId")).longValue() : null;

        // 更新任务状态
        Task task = taskRepository.findByTaskId(taskId).orElse(null);
        if (task != null) {
            task.setStatus(status);
            if (enterpriseName != null && !enterpriseName.isEmpty()) {
                task.setEnterpriseName(enterpriseName);
            }
            if ("completed".equals(status)) {
                task.setResult("税务采集完成，queryId=" + queryId);
            } else if ("failed".equals(status)) {
                task.setErrorMessage(errorMessage);
                task.setResult("税务采集失败: " + errorMessage);
            }
            taskRepository.save(task);
            log.info("任务状态已更新: taskId={}, status={}, enterpriseName={}", taskId, status, enterpriseName);
        } else {
            log.warn("未找到任务: taskId={}", taskId);
        }
    }
}
