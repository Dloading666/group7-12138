package com.rpa.management.controller;

import com.rpa.management.entity.Task;
import com.rpa.management.entity.TaskRun;
import com.rpa.management.repository.TaskRepository;
import com.rpa.management.repository.TaskRunRepository;
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
@RequestMapping("/spider/task")
@RequiredArgsConstructor
public class SpiderCallbackController {

    private final TaskRepository taskRepository;
    private final TaskRunRepository taskRunRepository;

    @PostMapping("/callback")
    @Transactional
    public void handleCallback(@RequestBody Map<String, Object> payload) {
        log.info("收到 spider 回调: {}", payload);

        String taskId = (String) payload.get("taskId");
        String status = (String) payload.get("status");
        String errorMessage = (String) payload.get("errorMessage");
        String enterpriseName = (String) payload.get("enterpriseName");
        Long queryId = payload.get("queryId") != null ? ((Number) payload.get("queryId")).longValue() : null;

        TaskRun run = taskRunRepository.findByRunId(taskId).orElse(null);
        Task task = null;
        if (run != null) {
            task = taskRepository.findById(run.getTaskId()).orElse(null);
        }
        if (task == null) {
            task = taskRepository.findByTaskId(taskId).orElse(null);
        }
        if (task == null) {
            log.warn("未找到任务: {}", taskId);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (run != null) {
            run.setStatus(status);
            run.setEndTime(now);
            if (run.getStartTime() != null) {
                run.setDuration((int) java.time.Duration.between(run.getStartTime(), now).getSeconds());
            }
            if ("completed".equals(status)) {
                run.setProgress(100);
                run.setErrorMessage(null);
                run.setResult("税务采集完成，queryId=" + queryId);
            } else if ("failed".equals(status)) {
                run.setErrorMessage(errorMessage);
                run.setResult("税务采集失败: " + errorMessage);
            }
            taskRunRepository.save(run);
        }

        task.setStatus(status);
        task.setLatestRunStatus(status);
        task.setLatestRunId(run != null ? run.getId() : task.getLatestRunId());
        task.setLastRunTime(now);
        task.setEndTime(now);
        if (enterpriseName != null && !enterpriseName.isEmpty()) {
            task.setEnterpriseName(enterpriseName);
        }
        if ("completed".equals(status)) {
            task.setProgress(100);
            task.setErrorMessage(null);
            task.setResult("税务采集完成，queryId=" + queryId);
        } else if ("failed".equals(status)) {
            task.setErrorMessage(StringUtils.hasText(errorMessage) ? errorMessage : "税务采集失败");
            task.setResult("税务采集失败: " + errorMessage);
        }
        taskRepository.save(task);
    }
}
