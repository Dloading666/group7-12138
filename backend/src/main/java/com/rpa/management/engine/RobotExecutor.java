package com.rpa.management.engine;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.rpa.management.client.AgentApiClient;
import com.rpa.management.client.SpiderApiClient;
import com.rpa.management.dto.CrawlResultDTO;
import com.rpa.management.entity.CollectConfig;
import com.rpa.management.entity.Robot;
import com.rpa.management.entity.Task;
import com.rpa.management.entity.TaskRun;
import com.rpa.management.repository.CollectConfigRepository;
import com.rpa.management.repository.RobotRepository;
import com.rpa.management.repository.TaskRepository;
import com.rpa.management.repository.TaskRunRepository;
import com.rpa.management.service.CrawlResultService;
import com.rpa.management.service.ExecutionLogService;
import com.rpa.management.service.TaskRunService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class RobotExecutor {

    private final RobotRepository robotRepository;
    private final TaskRepository taskRepository;
    private final TaskRunRepository taskRunRepository;
    private final CollectConfigRepository collectConfigRepository;
    private final TaskRunService taskRunService;
    private final ExecutionLogService executionLogService;
    private final SpiderApiClient spiderApiClient;
    private final AgentApiClient agentApiClient;
    private final CrawlResultService crawlResultService;
    private final WorkflowRunExecutor workflowRunExecutor;

    @Async
    public CompletableFuture<TaskExecutionResult> executeTaskAsync(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));
        TaskRun run = taskRunService.createRun(task, "manual", null);
        taskRunService.markRunning(task, run);
        taskRepository.save(task);
        return executeTaskRunAsync(run.getId());
    }

    @Async
    public CompletableFuture<TaskExecutionResult> executeTaskRunAsync(Long taskRunId) {
        TaskExecutionResult result = new TaskExecutionResult();
        result.setTaskRunId(taskRunId);

        TaskRun run = null;
        Task task = null;
        Robot robot = null;

        try {
            run = taskRunRepository.findById(taskRunId)
                    .orElseThrow(() -> new RuntimeException("Task run not found: " + taskRunId));
            Long taskId = run.getTaskId();
            task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

            if (task.getWorkflowVersionId() != null) {
                executionLogService.info(task.getId(), run.getId(), task.getTaskId(), task.getName(), null, null,
                        "Start workflow task run");
                workflowRunExecutor.execute(task, run);
                result.setSuccess(true);
                result.setMessage("success");
                result.setProcessedCount(1);
                return CompletableFuture.completedFuture(result);
            }

            robot = resolveRobot(task);
            if (robot != null) {
                robot.setStatus("running");
                robot.setCurrentTaskId(task.getTaskId());
                robot.setLastExecuteTime(LocalDateTime.now());
                robotRepository.save(robot);
            }

            executionLogService.info(task.getId(), run.getId(), task.getTaskId(), task.getName(),
                    robot != null ? robot.getId() : null,
                    robot != null ? robot.getName() : null,
                    "Start legacy task run");

            int processedCount = executeLegacyTask(task, run, robot);
            finalizeTaskSuccess(task, run, processedCount);
            finalizeRobotSuccess(robot);

            result.setSuccess(true);
            result.setMessage("success");
            result.setProcessedCount(processedCount);
        } catch (Exception ex) {
            log.error("Task execution failed", ex);
            if (task != null && run != null) {
                finalizeTaskFailure(task, run, ex.getMessage());
            }
            finalizeRobotFailure(robot);
            if (task != null) {
                executionLogService.error(task.getId(), run != null ? run.getId() : null, task.getTaskId(), task.getName(),
                        robot != null ? robot.getId() : null,
                        robot != null ? robot.getName() : null,
                        "Task execution failed: " + ex.getMessage());
            }
            result.setSuccess(false);
            result.setMessage(ex.getMessage());
        }

        return CompletableFuture.completedFuture(result);
    }

    private int executeLegacyTask(Task task, TaskRun run, Robot robot) {
        String taskType = task.getType();
        if ("ai_workflow".equals(taskType) || "workflow".equals(taskType)) {
            return executeAiWorkflowTask(task, run, robot);
        }
        if ("data-collection".equals(taskType) || "data_collection".equals(taskType) || "web-crawl".equals(taskType)) {
            return executeDataCollection(task, run, robot);
        }
        if ("report".equals(taskType)) {
            return executeSimulatedTask(task, run, robot, "Generate report", 1);
        }
        if ("data-sync".equals(taskType) || "data_sync".equals(taskType)) {
            return executeSimulatedTask(task, run, robot, "Sync data", 10);
        }
        return executeSimulatedTask(task, run, robot, "Execute default task", 1);
    }

    private Robot resolveRobot(Task task) {
        if (task.getRobotId() == null) {
            return null;
        }
        Robot robot = robotRepository.findById(task.getRobotId())
                .orElseThrow(() -> new RuntimeException("Robot not found: " + task.getRobotId()));
        if (!"online".equals(robot.getStatus()) && !"running".equals(robot.getStatus())) {
            throw new RuntimeException("Robot is offline: " + robot.getStatus());
        }
        return robot;
    }

    private int executeAiWorkflowTask(Task task, TaskRun run, Robot robot) {
        JSONObject params = parseJson(firstNonBlank(run.getInputConfig(), task.getParams()));
        Long workflowId = task.getWorkflowId();
        if (workflowId == null && params != null) {
            workflowId = params.getLong("workflowId");
        }
        if (workflowId == null) {
            throw new RuntimeException("workflowId is required for ai_workflow task");
        }

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

        executionLogService.info(task.getId(), run.getId(), task.getTaskId(), task.getName(),
                robot != null ? robot.getId() : null,
                robot != null ? robot.getName() : null,
                "Submit ai workflow to agent");
        taskRunService.updateProgress(task, run.getId(), 5);
        String engineRunId = agentApiClient.submitWorkflowTask(run.getRunId(), workflowId, submitParams);
        taskRunService.bindEngineRunId(run.getId(), engineRunId);

        int waited = 0;
        final int maxWaitSeconds = 900;
        while (waited < maxWaitSeconds) {
            sleep(5_000L);
            waited += 5;
            TaskRun freshRun = taskRunRepository.findById(run.getId()).orElse(run);
            if ("completed".equals(freshRun.getStatus())) {
                return 1;
            }
            if ("failed".equals(freshRun.getStatus())) {
                throw new RuntimeException(StringUtils.hasText(freshRun.getErrorMessage())
                        ? freshRun.getErrorMessage()
                        : "AI workflow execution failed");
            }
            int progress = Math.min(95, 10 + (int) Math.round(85.0 * waited / maxWaitSeconds));
            taskRunService.updateProgress(task, run.getId(), progress);
        }

        if (StringUtils.hasText(engineRunId)) {
            agentApiClient.cancelTask(engineRunId);
        }
        throw new RuntimeException("AI workflow execution timeout");
    }

    private int executeDataCollection(Task task, TaskRun run, Robot robot) {
        JSONObject params = parseJson(firstNonBlank(run.getInputConfig(), task.getParams()));
        if (params != null && StringUtils.hasText(params.getString("url"))) {
            return executeGenericWebCollection(task, run, robot, params);
        }

        CollectConfig config = collectConfigRepository.findByTaskId(task.getId())
                .stream()
                .findFirst()
                .orElse(null);
        if (config == null) {
            throw new RuntimeException("Collection config not found");
        }
        if ("spider-tax".equals(config.getCollectType())) {
            return executeSpiderTaxCollection(task, run, config, robot);
        }
        throw new RuntimeException("Legacy collection mode is no longer supported");
    }

    private int executeGenericWebCollection(Task task, TaskRun run, Robot robot, JSONObject params) {
        String url = params.getString("url");
        if (!StringUtils.hasText(url)) {
            throw new RuntimeException("Collection url is required");
        }
        executionLogService.info(task.getId(), run.getId(), task.getTaskId(), task.getName(),
                robot != null ? robot.getId() : null,
                robot != null ? robot.getName() : null,
                "Submit crawl task: " + url);
        taskRunService.updateProgress(task, run.getId(), 10);
        spiderApiClient.submitGenericCrawlTask(run.getRunId(), params);
        return waitForGenericCrawlCompletion(task, run, params.getInteger("timeout"));
    }

    private int waitForGenericCrawlCompletion(Task task, TaskRun run, Integer timeoutMillis) {
        int maxWaitSeconds = Math.max(90, ((timeoutMillis != null ? timeoutMillis : 30000) / 1000) + 90);
        int waited = 0;
        while (waited < maxWaitSeconds) {
            sleep(2000L);
            waited += 2;
            TaskRun freshRun = taskRunRepository.findById(run.getId()).orElse(run);
            if ("completed".equals(freshRun.getStatus())) {
                return crawlResultService.getTotalCount(run.getRunId());
            }
            if ("failed".equals(freshRun.getStatus())) {
                throw new RuntimeException(StringUtils.hasText(freshRun.getErrorMessage())
                        ? freshRun.getErrorMessage()
                        : "Crawl failed");
            }
            int progress = Math.min(95, 15 + (int) Math.round(80.0 * waited / maxWaitSeconds));
            taskRunService.updateProgress(task, run.getId(), progress);
        }
        throw new RuntimeException("Crawl timeout");
    }

    private int executeSpiderTaxCollection(Task task, TaskRun run, CollectConfig config, Robot robot) {
        executionLogService.info(task.getId(), run.getId(), task.getTaskId(), task.getName(),
                robot != null ? robot.getId() : null,
                robot != null ? robot.getName() : null,
                "Submit spider-tax task");
        JSONObject spiderConfig = parseJson(config.getSpiderConfig());
        if (spiderConfig == null) {
            throw new RuntimeException("spiderConfig is required");
        }
        spiderApiClient.submitSpiderTask(run.getRunId(), spiderConfig.getString("taxNo"), spiderConfig.getString("uscCode"), spiderConfig.getString("appDate"));

        int maxWaitSeconds = 300;
        int waited = 0;
        while (waited < maxWaitSeconds) {
            sleep(5_000L);
            waited += 5;
            TaskRun freshRun = taskRunRepository.findById(run.getId()).orElse(run);
            if ("completed".equals(freshRun.getStatus())) {
                return 1;
            }
            if ("failed".equals(freshRun.getStatus())) {
                throw new RuntimeException(StringUtils.hasText(freshRun.getErrorMessage())
                        ? freshRun.getErrorMessage()
                        : "Spider-tax task failed");
            }
            taskRunService.updateProgress(task, run.getId(), 20 + (int) Math.round(60.0 * waited / maxWaitSeconds));
        }
        throw new RuntimeException("Spider-tax task timeout");
    }

    private int executeSimulatedTask(Task task, TaskRun run, Robot robot, String stageMessage, int processedCount) {
        executionLogService.info(task.getId(), run.getId(), task.getTaskId(), task.getName(),
                robot != null ? robot.getId() : null,
                robot != null ? robot.getName() : null,
                stageMessage);
        for (int progress : new int[]{25, 50, 75, 100}) {
            taskRunService.updateProgress(task, run.getId(), progress);
            sleep(800L);
        }
        return processedCount;
    }

    private CrawlResultDTO resolveAiSource(JSONObject params) {
        if (params == null) {
            return null;
        }
        Long sourceTaskRunId = params.getLong("sourceTaskRunId");
        if (sourceTaskRunId != null) {
            return crawlResultService.getResultByTaskRunId(sourceTaskRunId);
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
            builder.append("Title: ").append(source.getTitle()).append("\n\n");
        }
        if (StringUtils.hasText(source.getFinalUrl())) {
            builder.append("URL: ").append(source.getFinalUrl()).append("\n\n");
        }
        if (StringUtils.hasText(source.getSummaryText())) {
            builder.append("Summary:\n").append(source.getSummaryText()).append("\n\n");
        }
        if (source.getStructuredData() != null && !source.getStructuredData().isEmpty()) {
            builder.append("Structured Data:\n").append(JSON.toJSONString(source.getStructuredData())).append("\n\n");
        }
        return builder.toString();
    }

    @Transactional
    protected void finalizeTaskSuccess(Task task, TaskRun run, int processedCount) {
        Task managedTask = taskRepository.findById(task.getId()).orElse(task);
        TaskRun freshRun = taskRunRepository.findById(run.getId()).orElse(run);
        if (!"completed".equals(freshRun.getStatus())) {
            String result = StringUtils.hasText(freshRun.getResult())
                    ? freshRun.getResult()
                    : "success, processed " + processedCount + " item(s)";
            taskRunService.completeRun(managedTask, freshRun.getId(), result);
        }
        taskRepository.save(managedTask);
    }

    @Transactional
    protected void finalizeTaskFailure(Task task, TaskRun run, String errorMessage) {
        Task managedTask = taskRepository.findById(task.getId()).orElse(task);
        TaskRun freshRun = taskRunRepository.findById(run.getId()).orElse(run);
        if (!"completed".equals(freshRun.getStatus())) {
            taskRunService.failRun(managedTask, freshRun.getId(), errorMessage);
        }
        taskRepository.save(managedTask);
    }

    @Transactional
    protected void finalizeRobotSuccess(Robot robot) {
        if (robot == null) {
            return;
        }
        robotRepository.findById(robot.getId()).ifPresent(current -> {
            long totalTasks = current.getTotalTasks() != null ? current.getTotalTasks() : 0L;
            long successTasks = current.getSuccessTasks() != null ? current.getSuccessTasks() : 0L;
            current.setStatus("online");
            current.setCurrentTaskId(null);
            current.setTotalTasks(totalTasks + 1);
            current.setSuccessTasks(successTasks + 1);
            current.setSuccessRate(current.getTotalTasks() > 0
                    ? (double) current.getSuccessTasks() / current.getTotalTasks()
                    : 0.0);
            robotRepository.save(current);
        });
    }

    @Transactional
    protected void finalizeRobotFailure(Robot robot) {
        if (robot == null) {
            return;
        }
        robotRepository.findById(robot.getId()).ifPresent(current -> {
            long totalTasks = current.getTotalTasks() != null ? current.getTotalTasks() : 0L;
            long failedTasks = current.getFailedTasks() != null ? current.getFailedTasks() : 0L;
            long successTasks = current.getSuccessTasks() != null ? current.getSuccessTasks() : 0L;
            current.setStatus("online");
            current.setCurrentTaskId(null);
            current.setTotalTasks(totalTasks + 1);
            current.setFailedTasks(failedTasks + 1);
            current.setSuccessRate(current.getTotalTasks() > 0
                    ? (double) successTasks / current.getTotalTasks()
                    : 0.0);
            robotRepository.save(current);
        });
    }

    private JSONObject parseJson(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return JSON.parseObject(raw);
        } catch (Exception ex) {
            return null;
        }
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Task execution interrupted");
        }
    }

    @Data
    public static class TaskExecutionResult {
        private Long taskRunId;
        private boolean success;
        private String message;
        private int processedCount;
    }
}
