package com.rpa.management.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.rpa.management.dto.CreateCrawlTaskRequest;
import com.rpa.management.dto.TaskDTO;
import com.rpa.management.engine.RobotExecutor;
import com.rpa.management.entity.Robot;
import com.rpa.management.entity.Task;
import com.rpa.management.repository.RobotRepository;
import com.rpa.management.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 任务服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final RobotRepository robotRepository;
    private final RobotExecutor robotExecutor;
    private final CrawlResultService crawlResultService;

    @Transactional
    public TaskDTO createTask(TaskDTO dto, Long userId, String userName) {
        Task task = new Task();
        task.setTaskId(generateTaskId());
        task.setName(dto.getName());
        task.setType(StringUtils.hasText(dto.getType()) ? dto.getType() : "default");
        task.setStatus("pending");
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
        task = taskRepository.save(task);
        return toDTO(task);
    }

    @Transactional
    public TaskDTO createCrawlTask(CreateCrawlTaskRequest request, Long userId, String userName) {
        Robot robot = robotRepository.findById(request.getRobotId())
                .orElseThrow(() -> new RuntimeException("数据采集机器人不存在: " + request.getRobotId()));

        if (!"data_collector".equals(robot.getType())) {
            throw new RuntimeException("请选择数据采集机器人执行真实网站抓取");
        }

        Task task = new Task();
        task.setTaskId(generateTaskId());
        task.setName(request.getName());
        task.setType("data-collection");
        task.setStatus("pending");
        task.setProgress(0);
        task.setRobotId(robot.getId());
        task.setRobotName(robot.getName());
        task.setPriority("medium");
        task.setExecuteType(StringUtils.hasText(request.getExecuteType()) ? request.getExecuteType() : "immediate");
        task.setScheduledTime(request.getScheduledTime());
        task.setUserId(userId);
        task.setUserName(userName);
        task.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription() : "真实网站采集任务");
        task.setParams(buildCrawlParams(request));

        task = taskRepository.save(task);
        log.info("Created crawl task {} for {}", task.getTaskId(), request.getUrl());

        return toDTO(task);
    }

    @Transactional
    public TaskDTO updateTask(Long id, TaskDTO dto) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + id));

        if (!"pending".equals(task.getStatus())) {
            throw new RuntimeException("只有待执行任务可以编辑");
        }

        task.setName(dto.getName());
        task.setType(dto.getType());
        task.setRobotId(dto.getRobotId());
        task.setRobotName(dto.getRobotName());
        task.setPriority(dto.getPriority());
        task.setExecuteType(dto.getExecuteType());
        task.setScheduledTime(dto.getScheduledTime());
        task.setDescription(dto.getDescription());
        task.setParams(dto.getParams());
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

        tasks.stream()
                .map(Task::getTaskId)
                .filter(StringUtils::hasText)
                .forEach(crawlResultService::deleteByTaskId);
        taskRepository.deleteAll(tasks);
    }

    public TaskDTO getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + id));
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

        if (!"pending".equals(task.getStatus())) {
            throw new RuntimeException("只有待执行任务可以启动");
        }

        task.setStatus("running");
        task.setStartTime(LocalDateTime.now());
        task.setProgress(0);
        task = taskRepository.save(task);

        robotExecutor.executeTaskAsync(id);
        return toDTO(task);
    }

    @Transactional
    public TaskDTO stopTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + id));

        if (!"running".equals(task.getStatus())) {
            throw new RuntimeException("只有执行中的任务可以停止");
        }

        task.setStatus("pending");
        task.setEndTime(LocalDateTime.now());
        task = taskRepository.save(task);
        return toDTO(task);
    }

    @Transactional
    public TaskDTO completeTask(Long id, String result) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + id));

        task.setStatus("completed");
        task.setProgress(100);
        task.setEndTime(LocalDateTime.now());
        task.setResult(result);
        task.setDuration(calculateDuration(task.getStartTime(), task.getEndTime()));
        task = taskRepository.save(task);
        return toDTO(task);
    }

    @Transactional
    public TaskDTO failTask(Long id, String errorMessage) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + id));

        task.setStatus("failed");
        task.setEndTime(LocalDateTime.now());
        task.setErrorMessage(errorMessage);
        task.setDuration(calculateDuration(task.getStartTime(), task.getEndTime()));
        task = taskRepository.save(task);
        return toDTO(task);
    }

    @Transactional
    public TaskDTO updateProgress(Long id, Integer progress) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + id));
        task.setProgress(Math.min(100, Math.max(0, progress)));
        task = taskRepository.save(task);
        return toDTO(task);
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

        Map<String, Object> detail = new HashMap<>();
        detail.put("task", toDTO(task));
        detail.put("executionHistory", List.of());
        detail.put("logs", List.of());
        return detail;
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

    private String generateTaskId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "T" + timestamp + random;
    }

    private Integer calculateDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return (int) java.time.Duration.between(start, end).getSeconds();
    }

    private TaskDTO toDTO(Task task) {
        JSONObject params = parseParams(task.getParams());
        JSONArray extractionRules = params != null ? params.getJSONArray("extractionRules") : null;
        return TaskDTO.builder()
                .id(task.getId())
                .taskId(task.getTaskId())
                .name(task.getName())
                .type(task.getType())
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
                .workflowId(params != null ? params.getLong("workflowId") : null)
                .workflowName(params != null ? params.getString("workflowName") : null)
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
            log.warn("Failed to parse task params for task {}", raw, ex);
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
