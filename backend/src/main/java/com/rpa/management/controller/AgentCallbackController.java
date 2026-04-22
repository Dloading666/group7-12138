package com.rpa.management.controller;

import com.alibaba.fastjson2.JSON;
import com.rpa.management.entity.Task;
import com.rpa.management.entity.TaskRun;
import com.rpa.management.repository.TaskRepository;
import com.rpa.management.repository.TaskRunRepository;
import com.rpa.management.service.ExecutionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentCallbackController {

    private final TaskRepository taskRepository;
    private final TaskRunRepository taskRunRepository;
    private final ExecutionLogService executionLogService;

    @PostMapping("/callback")
    @Transactional
    public Map<String, String> handleCallback(@RequestBody Map<String, Object> payload) {
        log.info("收到 Agent 回调: {}", payload);

        String taskId = (String) payload.get("taskId");
        String engineRunId = (String) payload.get("runId");
        String status = (String) payload.get("status");
        String errorMessage = (String) payload.get("errorMessage");
        Object result = payload.get("result");

        if (!StringUtils.hasText(taskId) || !StringUtils.hasText(status)) {
            return Map.of("code", "400", "message", "taskId 和 status 为必填字段");
        }

        TaskRun taskRun = taskRunRepository.findByRunId(taskId)
                .orElseGet(() -> taskRunRepository.findByEngineRunId(engineRunId).orElse(null));
        Task task = null;
        if (taskRun != null) {
            task = taskRepository.findById(taskRun.getTaskId()).orElse(null);
        }
        if (task == null) {
            task = taskRepository.findByTaskId(taskId).orElse(null);
        }
        if (task == null) {
            return Map.of("code", "404", "message", "任务不存在: " + taskId);
        }

        String formattedResult = formatAiResult(result);
        LocalDateTime now = LocalDateTime.now();

        if (taskRun != null) {
            taskRun.setEngineRunId(engineRunId);
            taskRun.setStatus(status);
            taskRun.setEndTime(now);
            if (taskRun.getStartTime() != null) {
                long seconds = java.time.Duration.between(taskRun.getStartTime(), now).getSeconds();
                taskRun.setDuration((int) seconds);
            }
            if ("completed".equals(status)) {
                taskRun.setProgress(100);
                taskRun.setErrorMessage(null);
                taskRun.setResult(formattedResult);
            } else if ("failed".equals(status)) {
                taskRun.setErrorMessage(errorMessage);
                taskRun.setResult(StringUtils.hasText(formattedResult) ? formattedResult : "AI 工作流执行失败");
            }
            taskRunRepository.save(taskRun);
        }

        task.setStatus(status);
        task.setLatestRunStatus(status);
        task.setLatestRunId(taskRun != null ? taskRun.getId() : task.getLatestRunId());
        task.setLastRunTime(now);
        task.setEndTime(now);
        if (task.getStartTime() != null) {
            long seconds = java.time.Duration.between(task.getStartTime(), task.getEndTime()).getSeconds();
            task.setDuration((int) seconds);
        }
        if ("completed".equals(status)) {
            task.setProgress(100);
            task.setErrorMessage(null);
            task.setResult(formattedResult);
            executionLogService.info(task.getId(), taskRun != null ? taskRun.getId() : null, task.getTaskId(), task.getName(),
                    task.getRobotId(), task.getRobotName(), "AI 工作流执行完成，engineRunId=" + engineRunId);
        } else if ("failed".equals(status)) {
            task.setErrorMessage(errorMessage);
            task.setResult("AI 工作流执行失败: " + errorMessage);
            executionLogService.error(task.getId(), taskRun != null ? taskRun.getId() : null, task.getTaskId(), task.getName(),
                    task.getRobotId(), task.getRobotName(), "AI 工作流执行失败: " + errorMessage);
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
        if (StringUtils.hasText(summary)) {
            builder.append("摘要:\n").append(summary).append("\n\n");
        }
        if (keyPoints != null) {
            builder.append("关键要点:\n").append(JSON.toJSONString(keyPoints)).append("\n\n");
        }
        if (StringUtils.hasText(analysis)) {
            builder.append("详细分析:\n").append(analysis);
        }
        return builder.isEmpty() ? JSON.toJSONString(resultMap) : builder.toString().trim();
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
