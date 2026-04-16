package com.rpa.management.controller;

import com.alibaba.fastjson2.JSON;
import com.rpa.management.entity.Task;
import com.rpa.management.repository.TaskRepository;
import com.rpa.management.service.AiAnalysisService;
import com.rpa.management.service.ExecutionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 接收 Python Agent（LangGraph）执行结果回调。
 */
@Slf4j
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentCallbackController {

    private final TaskRepository taskRepository;
    private final ExecutionLogService executionLogService;
    private final AiAnalysisService aiAnalysisService;

    @PostMapping("/callback")
    @Transactional
    public Map<String, String> handleCallback(@RequestBody Map<String, Object> payload) {
        log.info("收到 Agent 回调: {}", payload);

        String taskId = (String) payload.get("taskId");
        String runId = (String) payload.get("runId");
        String status = (String) payload.get("status");
        String errorMessage = (String) payload.get("errorMessage");
        Object result = payload.get("result");

        if (taskId == null || status == null) {
            log.warn("Agent 回调缺少必要字段: taskId={}, status={}", taskId, status);
            return Map.of("code", "400", "message", "taskId 和 status 为必填字段");
        }

        Task task = taskRepository.findByTaskId(taskId).orElse(null);
        if (task == null) {
            log.warn("Agent 回调未找到任务: taskId={}", taskId);
            return Map.of("code", "404", "message", "任务不存在: " + taskId);
        }

        task.setStatus(status);
        task.setEndTime(LocalDateTime.now());
        if (task.getStartTime() != null) {
            long seconds = java.time.Duration.between(task.getStartTime(), task.getEndTime()).getSeconds();
            task.setDuration((int) seconds);
        }

        if ("completed".equals(status)) {
            task.setProgress(100);
            task.setErrorMessage(null);
            task.setResult(formatAiResult(result));
            executionLogService.info(task.getId(), taskId, task.getName(),
                    task.getRobotId(), task.getRobotName(),
                    "AI 工作流执行完成，runId=" + runId);
            if ("ai_workflow".equals(task.getType())) {
                aiAnalysisService.saveInitialResultMessageIfAbsent(task.getId(), task.getResult());
            }
        } else if ("failed".equals(status)) {
            task.setProgress(task.getProgress() != null ? task.getProgress() : 0);
            task.setErrorMessage(errorMessage);
            task.setResult("AI 工作流执行失败: " + errorMessage);
            executionLogService.error(task.getId(), taskId, task.getName(),
                    task.getRobotId(), task.getRobotName(),
                    "AI 工作流执行失败: " + errorMessage);
        }

        taskRepository.save(task);
        return Map.of("code", "200", "message", "回调处理成功");
    }

    private String formatAiResult(Object result) {
        if (result == null) {
            return "AI 分析已完成";
        }
        if (!(result instanceof Map<?, ?> resultMap)) {
            return String.valueOf(result);
        }

        String summary = stringValue(resultMap.get("summary"));
        String analysis = stringValue(resultMap.get("analysis"));
        Object keyPoints = resultMap.get("key_points");

        StringBuilder builder = new StringBuilder();
        if (summary != null && !summary.isBlank()) {
            builder.append("摘要：\n").append(summary).append("\n\n");
        }
        if (keyPoints != null) {
            builder.append("关键要点：\n").append(JSON.toJSONString(keyPoints)).append("\n\n");
        }
        if (analysis != null && !analysis.isBlank()) {
            builder.append("详细分析：\n").append(analysis);
        }
        if (builder.isEmpty()) {
            return JSON.toJSONString(resultMap);
        }
        return builder.toString().trim();
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
