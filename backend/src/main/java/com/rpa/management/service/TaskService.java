package com.rpa.management.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.rpa.management.dto.CreateCrawlTaskRequest;
import com.rpa.management.dto.TaskDTO;
import com.rpa.management.dto.TaskRunDTO;
import com.rpa.management.engine.RobotExecutor;
import com.rpa.management.entity.Robot;
import com.rpa.management.entity.Task;
import com.rpa.management.entity.WorkflowVersion;
import com.rpa.management.repository.RobotRepository;
import com.rpa.management.repository.TaskRepository;
import com.rpa.management.repository.WorkflowVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskRunService taskRunService;
    private final RobotRepository robotRepository;
    private final WorkflowVersionRepository workflowVersionRepository;
    private final RobotExecutor robotExecutor;
    private final CrawlResultService crawlResultService;
    private final ExecutionLogService executionLogService;

    @Transactional
    public TaskDTO createTask(TaskDTO dto, Long userId, String userName) {
        if (dto.getWorkflowVersionId() != null) {
            Task task = createWorkflowTaskEntity(dto, userId, userName);
            task = taskRepository.save(task);
            if ("immediate".equalsIgnoreCase(task.getExecuteType())) {
                return startTask(task.getId());
            }
            return toDTO(task);
        }

        Task task = new Task();
        task.setTaskId(generateTaskId());
        task.setName(dto.getName());
        task.setType(StringUtils.hasText(dto.getType()) ? dto.getType() : "default");
        task.setStatus("pending");
        task.setLatestRunStatus("pending");
        task.setProgress(0);
        task.setRobotId(dto.getRobotId());
        task.setRobotName(dto.getRobotName());
        task.setPriority(StringUtils.hasText(dto.getPriority()) ? dto.getPriority() : "medium");
        task.setExecuteType(StringUtils.hasText(dto.getExecuteType()) ? dto.getExecuteType() : "immediate");
        task.setScheduledTime(dto.getScheduledTime());
        task.setUserId(userId);
        task.setUserName(userName);
        task.setDescription(dto.getDescription());
        task.setParams(dto.getParams());
        task.setInputConfig(StringUtils.hasText(dto.getInputConfig()) ? dto.getInputConfig() : dto.getParams());
        task.setScheduleConfig(dto.getScheduleConfig());
        task = taskRepository.save(task);
        return toDTO(task);
    }

    @Transactional
    public TaskDTO createCrawlTask(CreateCrawlTaskRequest request, Long userId, String userName) {
        Robot robot = robotRepository.findById(request.getRobotId())
                .orElseThrow(() -> new RuntimeException("数据采集机器人不存在: " + request.getRobotId()));

        if (!"data_collector".equals(robot.getType())) {
            throw new RuntimeException("请选择数据采集机器人执行真实网站采集任务");
        }

        Task task = new Task();
        task.setTaskId(generateTaskId());
        task.setName(request.getName());
        task.setType("data-collection");
        task.setStatus("pending");
        task.setLatestRunStatus("pending");
        task.setProgress(0);
        task.setRobotId(robot.getId());
        task.setRobotName(robot.getName());
        task.setPriority("medium");
        task.setExecuteType(StringUtils.hasText(request.getExecuteType()) ? request.getExecuteType() : "immediate");
        task.setScheduledTime(request.getScheduledTime());
        task.setUserId(userId);
        task.setUserName(userName);
        task.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription() : "真实网站采集任务");
        String params = buildCrawlParams(request);
        task.setParams(params);
        task.setInputConfig(params);
        task.setScheduleConfig(buildScheduleConfig(task.getExecuteType(), task.getScheduledTime(), null));

        task = taskRepository.save(task);
        return toDTO(task);
    }

    @Transactional
    public TaskDTO updateTask(Long id, TaskDTO dto) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + id));

        if ("running".equals(task.getStatus())) {
            throw new RuntimeException("执行中的任务不可编辑");
        }

        task.setName(dto.getName());
        task.setDescription(dto.getDescription());
        task.setPriority(dto.getPriority());
        if (task.getWorkflowVersionId() == null) {
            task.setType(dto.getType());
            task.setRobotId(dto.getRobotId());
            task.setRobotName(dto.getRobotName());
            task.setParams(dto.getParams());
            task.setInputConfig(StringUtils.hasText(dto.getInputConfig()) ? dto.getInputConfig() : dto.getParams());
        } else {
            String inputConfig = StringUtils.hasText(dto.getInputConfig()) ? dto.getInputConfig() : dto.getParams();
            task.setInputConfig(inputConfig);
            task.setParams(inputConfig);
            if (StringUtils.hasText(dto.getScheduleConfig())) {
                task.setScheduleConfig(dto.getScheduleConfig());
                task.setExecuteType(resolveExecuteType(dto.getExecuteType(), dto.getScheduleConfig(), dto.getScheduledTime()));
                LocalDateTime nextRun = resolveNextRunTime(task.getExecuteType(), dto.getScheduleConfig(), dto.getScheduledTime());
                task.setNextRunTime(nextRun);
                task.setScheduledTime(nextRun);
            }
        }

        task = taskRepository.save(task);
        return toDTO(task);
    }

    @Transactional
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + id));

        if ("running".equals(task.getStatus())) {
            throw new RuntimeException("执行中的任务不能删除");
        }

        if (StringUtils.hasText(task.getTaskId())) {
            crawlResultService.deleteByTaskId(task.getTaskId());
        }
        taskRunService.deleteByTaskId(task.getId());
        taskRepository.delete(task);
    }

    @Transactional
    public void deleteTasks(List<Long> ids) {
        List<Task> tasks = taskRepository.findAllById(ids);
        List<Task> runningTasks = tasks.stream()
                .filter(task -> "running".equals(task.getStatus()))
                .toList();
        if (!runningTasks.isEmpty()) {
            throw new RuntimeException("执行中的任务不能删除");
        }

        tasks.forEach(task -> {
            if (StringUtils.hasText(task.getTaskId())) {
                crawlResultService.deleteByTaskId(task.getTaskId());
            }
            taskRunService.deleteByTaskId(task.getId());
        });
        taskRepository.deleteAll(tasks);
    }

    public TaskDTO getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + id));
        taskRunService.ensureBackfillRun(task);
        return toDTO(task);
    }

    public Page<TaskDTO> getTasksByPage(String name, String type, String status,
                                        String priority, Long userId, Long robotId,
                                        LocalDateTime startTime, LocalDateTime endTime,
                                        int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, size), Sort.by("createTime").descending());
        return taskRepository.findByConditions(
                emptyToNull(name),
                emptyToNull(type),
                emptyToNull(status),
                emptyToNull(priority),
                userId,
                robotId,
                startTime,
                endTime,
                pageable
        ).map(this::toDTO);
    }

    public List<TaskDTO> getAllTasks() {
        return taskRepository.findAll(Sort.by("createTime").descending())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<TaskDTO> getTasksByUserId(Long userId) {
        return taskRepository.findByUserIdOrderByCreateTimeDesc(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskDTO startTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + id));
        startTaskInternal(task, "manual");
        return toDTO(task);
    }

    @Transactional
    public void startTaskByScheduler(Long id, String triggerType) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + id));
        startTaskInternal(task, triggerType);
    }

    @Transactional
    public TaskDTO stopTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + id));

        if (!"running".equals(task.getStatus())) {
            throw new RuntimeException("只有执行中的任务可以停止");
        }

        if (task.getLatestRunId() != null) {
            taskRunService.failRun(task, task.getLatestRunId(), "任务已手动停止");
        } else {
            task.setStatus("pending");
            task.setLatestRunStatus("pending");
            task.setEndTime(LocalDateTime.now());
        }
        taskRepository.save(task);
        return toDTO(task);
    }

    @Transactional
    public TaskDTO completeTask(Long id, String result) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + id));
        if (task.getLatestRunId() != null) {
            taskRunService.completeRun(task, task.getLatestRunId(), result);
        } else {
            task.setStatus("completed");
            task.setLatestRunStatus("completed");
            task.setProgress(100);
            task.setEndTime(LocalDateTime.now());
            task.setResult(result);
            task.setDuration(calculateDuration(task.getStartTime(), task.getEndTime()));
        }
        taskRepository.save(task);
        return toDTO(task);
    }

    @Transactional
    public TaskDTO failTask(Long id, String errorMessage) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + id));
        if (task.getLatestRunId() != null) {
            taskRunService.failRun(task, task.getLatestRunId(), errorMessage);
        } else {
            task.setStatus("failed");
            task.setLatestRunStatus("failed");
            task.setEndTime(LocalDateTime.now());
            task.setErrorMessage(errorMessage);
            task.setDuration(calculateDuration(task.getStartTime(), task.getEndTime()));
        }
        taskRepository.save(task);
        return toDTO(task);
    }

    @Transactional
    public TaskDTO updateProgress(Long id, Integer progress) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + id));
        task.setProgress(Math.min(100, Math.max(0, progress)));
        if (task.getLatestRunId() != null) {
            taskRunService.updateProgress(task, task.getLatestRunId(), task.getProgress());
        }
        taskRepository.save(task);
        return toDTO(task);
    }

    public List<TaskRunDTO> getTaskRuns(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + taskId));
        taskRunService.ensureBackfillRun(task);
        return taskRunService.listRuns(taskId);
    }

    public TaskRunDTO getTaskRun(Long runId) {
        return taskRunService.getRun(runId);
    }

    public TaskStats getTaskStats() {
        TaskStats stats = new TaskStats();
        stats.setTotal(taskRepository.countAll());
        stats.setRunning(taskRepository.countRunning());
        stats.setCompleted(taskRepository.countCompleted());
        stats.setFailed(taskRepository.countFailed());
        stats.setPending(stats.getTotal() - stats.getRunning() - stats.getCompleted() - stats.getFailed());

        List<Task> completedTasks = taskRepository.findByStatus("completed");
        int avgDuration = (int) completedTasks.stream()
                .map(Task::getDuration)
                .filter(duration -> duration != null && duration > 0)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);
        stats.setAvgDuration(avgDuration);
        return stats;
    }

    public List<TaskDTO> getRunningTasks(int limit) {
        Pageable pageable = PageRequest.of(0, Math.max(1, limit), Sort.by(Sort.Direction.DESC, "startTime"));
        return taskRepository.findByStatus("running", pageable)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getTaskExecutionDetail(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + taskId));
        List<TaskRunDTO> runs = getTaskRuns(taskId);

        Map<String, Object> detail = new HashMap<>();
        detail.put("task", toDTO(task));
        detail.put("executionHistory", runs);
        detail.put("logs", task.getLatestRunId() != null
                ? executionLogService.getLogsByTaskRunId(task.getLatestRunId())
                : executionLogService.getLogsByTaskId(taskId));
        return detail;
    }

    public Task createWorkflowTaskEntity(TaskDTO dto, Long userId, String userName) {
        WorkflowVersion version = workflowVersionRepository.findById(dto.getWorkflowVersionId())
                .orElseThrow(() -> new RuntimeException("流程版本不存在: " + dto.getWorkflowVersionId()));

        Task task = new Task();
        task.setTaskId(generateTaskId());
        task.setName(StringUtils.hasText(dto.getName()) ? dto.getName() : version.getName());
        task.setType("workflow");
        task.setWorkflowId(version.getWorkflowId());
        task.setWorkflowVersionId(version.getId());
        task.setWorkflowVersion(version.getVersionNumber());
        task.setWorkflowName(version.getName());
        task.setWorkflowCategory(version.getCategory());
        task.setStatus("pending");
        task.setLatestRunStatus("pending");
        task.setProgress(0);
        task.setPriority(StringUtils.hasText(dto.getPriority()) ? dto.getPriority() : "medium");
        task.setUserId(userId);
        task.setUserName(userName);
        task.setDescription(StringUtils.hasText(dto.getDescription()) ? dto.getDescription() : version.getDescription());
        String inputConfig = StringUtils.hasText(dto.getInputConfig()) ? dto.getInputConfig() : dto.getParams();
        task.setInputConfig(inputConfig);
        task.setParams(inputConfig);
        String scheduleConfig = StringUtils.hasText(dto.getScheduleConfig())
                ? dto.getScheduleConfig()
                : buildScheduleConfig(dto.getExecuteType(), dto.getScheduledTime(), null);
        task.setScheduleConfig(scheduleConfig);
        task.setExecuteType(resolveExecuteType(dto.getExecuteType(), scheduleConfig, dto.getScheduledTime()));
        LocalDateTime nextRun = resolveNextRunTime(task.getExecuteType(), scheduleConfig, dto.getScheduledTime());
        task.setNextRunTime(nextRun);
        task.setScheduledTime(nextRun);
        return task;
    }

    @Transactional
    protected void startTaskInternal(Task task, String triggerType) {
        if ("running".equals(task.getStatus())) {
            throw new RuntimeException("任务正在执行中");
        }

        String workflowSnapshot = resolveWorkflowSnapshot(task);
        var run = taskRunService.createRun(task, triggerType, workflowSnapshot);
        taskRunService.markRunning(task, run);

        if ("cron".equalsIgnoreCase(task.getExecuteType())) {
            LocalDateTime nextRun = resolveNextRunTime(task.getExecuteType(), task.getScheduleConfig(), null);
            task.setNextRunTime(nextRun);
            task.setScheduledTime(nextRun);
        }

        taskRepository.save(task);
        robotExecutor.executeTaskRunAsync(run.getId());
    }

    private String resolveWorkflowSnapshot(Task task) {
        if (task.getWorkflowVersionId() == null) {
            return null;
        }
        return workflowVersionRepository.findById(task.getWorkflowVersionId())
                .map(WorkflowVersion::getGraph)
                .orElse(null);
    }

    private String buildCrawlParams(CreateCrawlTaskRequest request) {
        JSONObject params = new JSONObject();
        params.put("url", request.getUrl());
        params.put("timeout", request.getTimeout() != null ? request.getTimeout() : 30000);
        if (request.getHeaders() != null && !request.getHeaders().isEmpty()) {
            params.put("headers", request.getHeaders());
        }
        if (request.getCookies() != null && !request.getCookies().isEmpty()) {
            params.put("cookies", request.getCookies());
        }
        if (request.getExtractionRules() != null && !request.getExtractionRules().isEmpty()) {
            params.put("extractionRules", request.getExtractionRules());
        }
        if (request.getPagination() != null && request.getPagination().getMaxPages() != null
                && request.getPagination().getMaxPages() > 1) {
            params.put("pagination", request.getPagination());
        }
        return JSON.toJSONString(params);
    }

    private String buildScheduleConfig(String executeType, LocalDateTime scheduledTime, String cronExpression) {
        JSONObject schedule = new JSONObject();
        String mode = StringUtils.hasText(executeType) ? executeType : "immediate";
        schedule.put("mode", mode);
        if (scheduledTime != null) {
            schedule.put("scheduledTime", scheduledTime.toString());
        }
        if (StringUtils.hasText(cronExpression)) {
            schedule.put("cronExpression", cronExpression);
        }
        return schedule.toJSONString();
    }

    private String resolveExecuteType(String executeType, String scheduleConfig, LocalDateTime scheduledTime) {
        if (StringUtils.hasText(executeType)) {
            return executeType;
        }
        JSONObject schedule = parseParams(scheduleConfig);
        if (schedule != null && StringUtils.hasText(schedule.getString("mode"))) {
            return schedule.getString("mode");
        }
        if (scheduledTime != null) {
            return "scheduled";
        }
        return "immediate";
    }

    private LocalDateTime resolveNextRunTime(String executeType, String scheduleConfig, LocalDateTime scheduledTime) {
        if ("scheduled".equalsIgnoreCase(executeType)) {
            return scheduledTime;
        }
        if (!"cron".equalsIgnoreCase(executeType)) {
            return scheduledTime;
        }
        JSONObject schedule = parseParams(scheduleConfig);
        String cronExpression = schedule != null ? schedule.getString("cronExpression") : null;
        if (!StringUtils.hasText(cronExpression)) {
            throw new RuntimeException("Cron 调度缺少 cronExpression");
        }
        try {
            return CronExpression.parse(cronExpression).next(LocalDateTime.now());
        } catch (Exception ex) {
            throw new RuntimeException("Cron 表达式无效: " + cronExpression);
        }
    }

    private String generateTaskId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "T" + timestamp + random;
    }

    private Integer calculateDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return (int) Duration.between(start, end).getSeconds();
    }

    private TaskDTO toDTO(Task task) {
        JSONObject params = parseParams(StringUtils.hasText(task.getInputConfig()) ? task.getInputConfig() : task.getParams());
        JSONArray extractionRules = params != null ? params.getJSONArray("extractionRules") : null;
        return TaskDTO.builder()
                .id(task.getId())
                .taskId(task.getTaskId())
                .name(task.getName())
                .type(task.getType())
                .workflowId(task.getWorkflowId())
                .workflowVersionId(task.getWorkflowVersionId())
                .workflowVersion(task.getWorkflowVersion())
                .workflowName(task.getWorkflowName())
                .workflowCategory(task.getWorkflowCategory())
                .status(task.getStatus())
                .progress(task.getProgress())
                .robotId(task.getRobotId())
                .robotName(task.getRobotName())
                .priority(task.getPriority())
                .executeType(task.getExecuteType())
                .scheduledTime(task.getScheduledTime())
                .startTime(task.getStartTime())
                .endTime(task.getEndTime())
                .duration(task.getDuration())
                .userId(task.getUserId())
                .userName(task.getUserName())
                .description(task.getDescription())
                .result(task.getResult())
                .params(task.getParams())
                .inputConfig(task.getInputConfig())
                .scheduleConfig(task.getScheduleConfig())
                .latestRunId(task.getLatestRunId())
                .latestRunStatus(task.getLatestRunStatus())
                .lastRunTime(task.getLastRunTime())
                .nextRunTime(task.getNextRunTime())
                .runCount(taskRunService.countRuns(task.getId()))
                .errorMessage(task.getErrorMessage())
                .createTime(task.getCreateTime())
                .taxId(task.getTaxId())
                .enterpriseName(task.getEnterpriseName())
                .updateTime(task.getUpdateTime())
                .crawlUrl(params != null ? params.getString("url") : null)
                .crawlTimeout(params != null ? params.getInteger("timeout") : null)
                .hasHeaders(params != null && params.getJSONObject("headers") != null && !params.getJSONObject("headers").isEmpty())
                .hasCookies(params != null && params.getJSONArray("cookies") != null && !params.getJSONArray("cookies").isEmpty())
                .hasPagination(params != null && params.getJSONObject("pagination") != null)
                .extractionRuleCount(extractionRules != null ? extractionRules.size() : 0)
                .sourceTaskRecordId(params != null ? params.getLong("sourceTaskRecordId") : null)
                .sourceTaskId(params != null ? params.getString("sourceTaskId") : null)
                .sourceTaskName(params != null ? params.getString("sourceTaskName") : null)
                .sourceTitle(params != null ? params.getString("sourceTitle") : null)
                .sourceFinalUrl(params != null ? params.getString("sourceFinalUrl") : null)
                .query(params != null ? params.getString("query") : null)
                .build();
    }

    private JSONObject parseParams(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return JSON.parseObject(raw);
        } catch (Exception ex) {
            log.warn("Failed to parse task payload", ex);
            return null;
        }
    }

    private String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    @lombok.Data
    public static class TaskStats {
        private Long total;
        private Long pending;
        private Long running;
        private Long completed;
        private Long failed;
        private Integer avgDuration;
    }
}
