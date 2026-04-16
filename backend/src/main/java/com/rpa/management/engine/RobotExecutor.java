package com.rpa.management.engine;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.rpa.management.client.AgentApiClient;
import com.rpa.management.client.SpiderApiClient;
import com.rpa.management.dto.CrawlResultDTO;
import com.rpa.management.entity.CollectConfig;
import com.rpa.management.entity.Robot;
import com.rpa.management.entity.Task;
import com.rpa.management.repository.CollectConfigRepository;
import com.rpa.management.repository.RobotRepository;
import com.rpa.management.repository.TaskRepository;
import com.rpa.management.service.CrawlResultService;
import com.rpa.management.service.ExecutionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * 机器人任务执行器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RobotExecutor {

    private final RobotRepository robotRepository;
    private final TaskRepository taskRepository;
    private final CollectConfigRepository collectConfigRepository;
    private final ExecutionLogService executionLogService;
    private final SpiderApiClient spiderApiClient;
    private final AgentApiClient agentApiClient;
    private final CrawlResultService crawlResultService;

    @Async
    public CompletableFuture<TaskExecutionResult> executeTaskAsync(Long taskId) {
        TaskExecutionResult result = new TaskExecutionResult();
        result.setTaskId(taskId);

        Task task = null;
        Robot robot = null;

        try {
            task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("任务不存在: " + taskId));

            if (!"pending".equals(task.getStatus()) && !"running".equals(task.getStatus())) {
                throw new RuntimeException("任务状态不允许执行: " + task.getStatus());
            }

            if (task.getRobotId() == null) {
                throw new RuntimeException("任务未分配机器人");
            }

            Long robotId = task.getRobotId();
            robot = robotRepository.findById(robotId)
                    .orElseThrow(() -> new RuntimeException("机器人不存在: " + robotId));

            if (!"online".equals(robot.getStatus()) && !"running".equals(robot.getStatus())) {
                throw new RuntimeException("机器人未在线: " + robot.getStatus());
            }

            task.setStatus("running");
            if (task.getStartTime() == null) {
                task.setStartTime(LocalDateTime.now());
            }
            task.setProgress(0);
            taskRepository.save(task);

            robot.setStatus("running");
            robot.setCurrentTaskId(task.getTaskId());
            robot.setLastExecuteTime(LocalDateTime.now());
            robotRepository.save(robot);

            executionLogService.info(taskId, task.getTaskId(), task.getName(),
                    robot.getId(), robot.getName(), "开始执行任务");

            int processedCount = executeTaskByType(task, robot);

            finalizeTaskSuccess(task.getId(), processedCount);
            finalizeRobotSuccess(robot.getId());

            executionLogService.info(taskId, task.getTaskId(), task.getName(),
                    robot.getId(), robot.getName(), "任务执行成功");

            result.setSuccess(true);
            result.setMessage("执行成功");
            result.setProcessedCount(processedCount);
        } catch (Exception ex) {
            log.error("任务执行失败", ex);
            finalizeTaskFailure(taskId, ex.getMessage());
            if (robot != null) {
                finalizeRobotFailure(robot.getId());
                executionLogService.error(taskId,
                        task != null ? task.getTaskId() : null,
                        task != null ? task.getName() : null,
                        robot.getId(),
                        robot.getName(),
                        "任务执行失败: " + ex.getMessage());
            }

            result.setSuccess(false);
            result.setMessage(ex.getMessage());
        }

        return CompletableFuture.completedFuture(result);
    }

    private int executeTaskByType(Task task, Robot robot) {
        String taskType = task.getType();

        // AI 工作流任务 → Python Agent (port 5000)
        if ("ai_workflow".equals(taskType) || "workflow".equals(taskType)) {
            return executeAiWorkflowTask(task, robot);
        }

        // 爬虫/数据采集任务 → Spider Java (port 8081)
        if ("data-collection".equals(taskType) || "data_collection".equals(taskType) || "web-crawl".equals(taskType)) {
            return executeDataCollection(task, robot);
        }
        if ("report".equals(taskType)) {
            return executeReportGeneration(task, robot);
        }
        if ("data-sync".equals(taskType) || "data_sync".equals(taskType)) {
            return executeDataSync(task, robot);
        }
        return executeDefaultTask(task, robot);
    }

    /**
     * 提交 AI 工作流任务到 Python Agent（非阻塞，由 Agent 回调更新最终状态）。
     *
     * 流程：
     *   1. 调用 Agent POST /submit → 立即返回 runId
     *   2. 本方法轮询数据库，等待 AgentCallbackController 将状态改为 completed/failed
     *   3. 超过 15 分钟未回调视为超时，主动取消 Agent 任务
     */
    private int executeAiWorkflowTask(Task task, Robot robot) {
        executionLogService.info(task.getId(), task.getTaskId(), task.getName(),
                robot.getId(), robot.getName(), "提交 AI 工作流任务到 Agent");

        JSONObject params = parseJson(task.getParams());
        Long workflowId = params != null ? params.getLong("workflowId") : null;
        JSONObject submitParams = buildAiWorkflowSubmitParams(params);

        updateTaskProgress(task.getId(), 5);

        // 提交任务（非阻塞，返回 runId 供后续取消使用）
        String runId = agentApiClient.submitWorkflowTask(task.getTaskId(), workflowId, submitParams);

        executionLogService.info(task.getId(), task.getTaskId(), task.getName(),
                robot.getId(), robot.getName(),
                "AI 工作流任务已提交，等待 Agent 回调，runId=" + runId);

        // 轮询本地 DB，等待 AgentCallbackController 更新状态
        // 最长等待 15 分钟（与 Agent 的 TIMEOUT_SECONDS=900 保持一致）
        final int maxWaitSeconds = 900;
        int waited = 0;
        while (waited < maxWaitSeconds) {
            sleep(5_000L);
            waited += 5;

            Task currentTask = taskRepository.findById(task.getId())
                    .orElseThrow(() -> new RuntimeException("任务不存在"));

            if ("completed".equals(currentTask.getStatus())) {
                return 1;
            }
            if ("failed".equals(currentTask.getStatus())) {
                throw new RuntimeException(StringUtils.hasText(currentTask.getErrorMessage())
                        ? currentTask.getErrorMessage()
                        : "AI 工作流执行失败");
            }

            // 平滑进度：10% → 95%
            int progress = Math.min(95, 10 + (int) Math.round(85.0 * waited / maxWaitSeconds));
            updateTaskProgress(task.getId(), progress);
        }

        // 超时：主动通知 Agent 取消
        if (StringUtils.hasText(runId)) {
            agentApiClient.cancelTask(runId);
        }
        throw new RuntimeException("AI 工作流执行超时（超过 15 分钟）");
    }

    private JSONObject buildAiWorkflowSubmitParams(JSONObject params) {
        JSONObject submitParams = params != null ? new JSONObject(params) : new JSONObject();
        CrawlResultDTO source = resolveAiSource(params);
        if (source != null) {
            submitParams.put("content", buildAiSourceContent(source));
            submitParams.putIfAbsent("sourceTaskRecordId", source.getTaskRecordId());
            submitParams.putIfAbsent("sourceTaskId", source.getTaskId());
            submitParams.putIfAbsent("sourceTaskName", source.getTaskName());
            submitParams.putIfAbsent("sourceTitle", source.getTitle());
            submitParams.putIfAbsent("sourceFinalUrl", source.getFinalUrl());
        }
        if (!StringUtils.hasText(submitParams.getString("type"))) {
            submitParams.put("type", StringUtils.hasText(submitParams.getString("query")) ? "qa" : "analysis");
        }
        return submitParams;
    }

    private CrawlResultDTO resolveAiSource(JSONObject params) {
        if (params == null) {
            return null;
        }

        Long sourceTaskRecordId = params.getLong("sourceTaskRecordId");
        if (sourceTaskRecordId != null) {
            return crawlResultService.getResultByTaskRecordId(sourceTaskRecordId);
        }

        String sourceTaskId = params.getString("sourceTaskId");
        if (StringUtils.hasText(sourceTaskId)) {
            return crawlResultService.getResultByTaskId(sourceTaskId);
        }
        return null;
    }

    private String buildAiSourceContent(CrawlResultDTO source) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(source.getTitle())) {
            builder.append("标题: ").append(source.getTitle()).append("\n\n");
        }
        if (StringUtils.hasText(source.getFinalUrl())) {
            builder.append("来源URL: ").append(source.getFinalUrl()).append("\n\n");
        }
        if (StringUtils.hasText(source.getSummaryText())) {
            builder.append("摘要:\n").append(source.getSummaryText()).append("\n\n");
        }
        if (source.getStructuredData() != null && !source.getStructuredData().isEmpty()) {
            builder.append("结构化数据:\n")
                    .append(JSON.toJSONString(source.getStructuredData()))
                    .append("\n\n");
        }
        if (builder.isEmpty() && StringUtils.hasText(source.getRawHtml())) {
            String rawHtml = source.getRawHtml();
            builder.append(rawHtml, 0, Math.min(rawHtml.length(), 12000));
        }
        return builder.toString();
    }

    private int executeDataCollection(Task task, Robot robot) {
        JSONObject params = parseJson(task.getParams());
        if (params != null && StringUtils.hasText(params.getString("url"))) {
            return executeGenericWebCollection(task, robot, params);
        }

        CollectConfig config = collectConfigRepository.findByTaskId(task.getId())
                .stream()
                .findFirst()
                .orElse(null);

        if (config == null) {
            throw new RuntimeException("未找到真实网站采集参数，请重新创建采集任务");
        }

        if ("spider-tax".equals(config.getCollectType())) {
            return executeSpiderTaxCollection(task, config, robot);
        }

        throw new RuntimeException("旧版模拟采集模式已下线，请从数据采集页面创建真实网站任务");
    }

    private int executeGenericWebCollection(Task task, Robot robot, JSONObject params) {
        String url = params.getString("url");
        if (!StringUtils.hasText(url)) {
            throw new RuntimeException("真实网站采集任务缺少 URL");
        }

        executionLogService.info(task.getId(), task.getTaskId(), task.getName(),
                robot.getId(), robot.getName(), "提交真实网站采集任务: " + url);

        updateTaskProgress(task.getId(), 10);

        spiderApiClient.submitGenericCrawlTask(task.getTaskId(), params);
        return waitForGenericCrawlCompletion(task.getId(), task.getTaskId(), params.getInteger("timeout"));
    }

    private int waitForGenericCrawlCompletion(Long taskRecordId, String taskId, Integer timeoutMillis) {
        int maxWaitSeconds = Math.max(90, ((timeoutMillis != null ? timeoutMillis : 30000) / 1000) + 90);
        int waited = 0;

        while (waited < maxWaitSeconds) {
            sleep(2000L);
            waited += 2;

            Task currentTask = taskRepository.findByTaskId(taskId)
                    .orElseThrow(() -> new RuntimeException("任务不存在: " + taskId));

            if ("completed".equals(currentTask.getStatus())) {
                return crawlResultService.getTotalCount(taskId);
            }
            if ("failed".equals(currentTask.getStatus())) {
                throw new RuntimeException(StringUtils.hasText(currentTask.getErrorMessage())
                        ? currentTask.getErrorMessage()
                        : "真实网站抓取失败");
            }

            int progress = Math.min(95, 15 + (int) Math.round(80.0 * waited / maxWaitSeconds));
            updateTaskProgress(taskRecordId, progress);
        }

        throw new RuntimeException("真实网站抓取超时");
    }

    private int executeSpiderTaxCollection(Task task, CollectConfig config, Robot robot) {
        executionLogService.info(task.getId(), task.getTaskId(), task.getName(),
                robot.getId(), robot.getName(), "提交税务专用爬虫任务");

        JSONObject spiderConfig = parseJson(config.getSpiderConfig());
        if (spiderConfig == null) {
            throw new RuntimeException("税务爬虫配置缺少 spiderConfig");
        }

        String taxNo = spiderConfig.getString("taxNo");
        String uscCode = spiderConfig.getString("uscCode");
        String appDate = spiderConfig.getString("appDate");

        spiderApiClient.submitSpiderTask(task.getTaskId(), taxNo, uscCode, appDate);

        int maxWaitSeconds = 300;
        int waited = 0;
        while (waited < maxWaitSeconds) {
            sleep(5000L);
            waited += 5;

            String status = spiderApiClient.getTaskStatus(task.getTaskId());
            updateTaskProgress(task.getId(), 20 + (int) Math.round(60.0 * waited / maxWaitSeconds));

            if ("completed".equals(status)) {
                JSONObject result = spiderApiClient.getTaskResult(task.getTaskId());
                String resultSummary = "税务采集完成";
                String resultTaxNo = taxNo;

                if (result != null && result.getJSONObject("data") != null) {
                    JSONObject data = result.getJSONObject("data");
                    String businessJson = data.getString("businessJson");
                    if (StringUtils.hasText(businessJson)) {
                        try {
                            JSONObject bizObj = JSON.parseObject(businessJson);
                            JSONObject collectedData = bizObj.getJSONObject("collectedData");
                            if (collectedData != null && StringUtils.hasText(collectedData.getString("taxNo"))) {
                                resultTaxNo = collectedData.getString("taxNo");
                            }
                        } catch (Exception ex) {
                            log.warn("Failed to parse tax spider businessJson", ex);
                        }
                    }
                }

                Task freshTask = taskRepository.findById(task.getId()).orElse(task);
                freshTask.setResult(resultSummary + "，taxNo=" + resultTaxNo);
                freshTask.setTaxId(resultTaxNo);
                taskRepository.save(freshTask);
                return 1;
            }

            if ("failed".equals(status)) {
                throw new RuntimeException("税务爬虫执行失败，请检查 spider_exc 日志");
            }
        }

        throw new RuntimeException("税务爬虫执行超时");
    }

    private int executeReportGeneration(Task task, Robot robot) {
        executionLogService.info(task.getId(), task.getTaskId(), task.getName(),
                robot.getId(), robot.getName(), "开始生成报表");
        updateTaskProgress(task.getId(), 30);
        sleep(1000L);
        updateTaskProgress(task.getId(), 60);
        sleep(1000L);
        updateTaskProgress(task.getId(), 90);
        sleep(500L);
        return 1;
    }

    private int executeDataSync(Task task, Robot robot) {
        executionLogService.info(task.getId(), task.getTaskId(), task.getName(),
                robot.getId(), robot.getName(), "开始数据同步");
        updateTaskProgress(task.getId(), 25);
        sleep(800L);
        updateTaskProgress(task.getId(), 50);
        sleep(800L);
        updateTaskProgress(task.getId(), 75);
        sleep(800L);
        return 10;
    }

    private int executeDefaultTask(Task task, Robot robot) {
        executionLogService.info(task.getId(), task.getTaskId(), task.getName(),
                robot.getId(), robot.getName(), "开始执行默认任务");
        for (int i = 0; i <= 100; i += 10) {
            updateTaskProgress(task.getId(), i);
            sleep(500L);
        }
        return 1;
    }

    @Transactional
    protected void finalizeTaskSuccess(Long taskId, int processedCount) {
        taskRepository.findById(taskId).ifPresent(task -> {
            task.setStatus("completed");
            task.setProgress(100);
            if (task.getEndTime() == null) {
                task.setEndTime(LocalDateTime.now());
            }
            task.setDuration(calculateDuration(task.getStartTime(), task.getEndTime()));
            if (!StringUtils.hasText(task.getResult())) {
                task.setResult("执行成功，处理了 " + processedCount + " 条数据");
            }
            taskRepository.save(task);
        });
    }

    @Transactional
    protected void finalizeTaskFailure(Long taskId, String errorMessage) {
        taskRepository.findById(taskId).ifPresent(task -> {
            if (!"completed".equals(task.getStatus())) {
                task.setStatus("failed");
            }
            task.setEndTime(LocalDateTime.now());
            task.setDuration(calculateDuration(task.getStartTime(), task.getEndTime()));
            if (!StringUtils.hasText(task.getErrorMessage())) {
                task.setErrorMessage(errorMessage);
            }
            if (!StringUtils.hasText(task.getResult())) {
                task.setResult("执行失败");
            }
            taskRepository.save(task);
        });
    }

    @Transactional
    protected void finalizeRobotSuccess(Long robotId) {
        robotRepository.findById(robotId).ifPresent(robot -> {
            long totalTasks = robot.getTotalTasks() != null ? robot.getTotalTasks() : 0L;
            long successTasks = robot.getSuccessTasks() != null ? robot.getSuccessTasks() : 0L;
            robot.setStatus("online");
            robot.setCurrentTaskId(null);
            robot.setTotalTasks(totalTasks + 1);
            robot.setSuccessTasks(successTasks + 1);
            robot.setSuccessRate(robot.getTotalTasks() > 0
                    ? (double) robot.getSuccessTasks() / robot.getTotalTasks()
                    : 0.0);
            robotRepository.save(robot);
        });
    }

    @Transactional
    protected void finalizeRobotFailure(Long robotId) {
        robotRepository.findById(robotId).ifPresent(robot -> {
            long totalTasks = robot.getTotalTasks() != null ? robot.getTotalTasks() : 0L;
            long failedTasks = robot.getFailedTasks() != null ? robot.getFailedTasks() : 0L;
            robot.setStatus("online");
            robot.setCurrentTaskId(null);
            robot.setTotalTasks(totalTasks + 1);
            robot.setFailedTasks(failedTasks + 1);
            robot.setSuccessRate(robot.getTotalTasks() > 0
                    ? (double) robot.getSuccessTasks() / robot.getTotalTasks()
                    : 0.0);
            robotRepository.save(robot);
        });
    }

    private void updateTaskProgress(Long taskId, int progress) {
        taskRepository.findById(taskId).ifPresent(task -> {
            task.setProgress(progress);
            taskRepository.save(task);
        });
    }

    private Integer calculateDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return (int) Duration.between(start, end).getSeconds();
    }

    private JSONObject parseJson(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return JSON.parseObject(raw);
        } catch (Exception ex) {
            log.warn("Failed to parse JSON payload", ex);
            return null;
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("任务执行被中断");
        }
    }

    @lombok.Data
    public static class TaskExecutionResult {
        private Long taskId;
        private boolean success;
        private String message;
        private int processedCount;
    }
}
