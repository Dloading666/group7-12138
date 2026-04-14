package com.rpa.management.service;

import com.rpa.management.dto.TaskDTO;
import com.rpa.management.engine.RobotExecutor;
import com.rpa.management.entity.Task;
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
    private final RobotExecutor robotExecutor;
    
    /**
     * 创建任务
     */
    @Transactional
    public TaskDTO createTask(TaskDTO dto, Long userId, String userName) {
        Task task = new Task();
        task.setTaskId(generateTaskId());
        task.setName(dto.getName());
        task.setType(dto.getType());
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
        
        task = taskRepository.save(task);
        log.info("创建任务成功: {}", task.getTaskId());
        
        return toDTO(task);
    }
    
    /**
     * 更新任务
     */
    @Transactional
    public TaskDTO updateTask(Long id, TaskDTO dto) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + id));
        
        // 只有等待中的任务可以编辑
        if (!"pending".equals(task.getStatus())) {
            throw new RuntimeException("只有等待中的任务可以编辑");
        }
        
        task.setName(dto.getName());
        task.setType(dto.getType());
        task.setRobotId(dto.getRobotId());
        task.setRobotName(dto.getRobotName());
        task.setPriority(dto.getPriority());
        task.setExecuteType(dto.getExecuteType());
        task.setScheduledTime(dto.getScheduledTime());
        task.setDescription(dto.getDescription());
        
        task = taskRepository.save(task);
        log.info("更新任务成功: {}", task.getTaskId());
        
        return toDTO(task);
    }
    
    /**
     * 删除任务
     */
    @Transactional
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + id));
        
        // 执行中的任务不能删除
        if ("running".equals(task.getStatus())) {
            throw new RuntimeException("执行中的任务不能删除");
        }
        
        taskRepository.deleteById(id);
        log.info("删除任务成功: {}", task.getTaskId());
    }
    
    /**
     * 批量删除任务
     */
    @Transactional
    public void deleteTasks(List<Long> ids) {
        List<Task> tasks = taskRepository.findAllById(ids);
        
        // 检查是否有执行中的任务
        List<Task> runningTasks = tasks.stream()
                .filter(t -> "running".equals(t.getStatus()))
                .collect(Collectors.toList());
        
        if (!runningTasks.isEmpty()) {
            throw new RuntimeException("执行中的任务不能删除: " + 
                    runningTasks.stream().map(Task::getTaskId).collect(Collectors.joining(", ")));
        }
        
        taskRepository.deleteAllById(ids);
        log.info("批量删除任务成功, 数量: {}", ids.size());
    }
    
    /**
     * 根据ID查询任务
     */
    public TaskDTO getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + id));
        return toDTO(task);
    }
    
    /**
     * 分页查询任务
     */
    public Page<TaskDTO> getTasksByPage(String name, String type, String status, 
                                         String priority, Long userId, Long robotId,
                                         LocalDateTime startTime, LocalDateTime endTime,
                                         int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createTime").descending());
        Page<Task> taskPage = taskRepository.findByConditions(name, type, status, priority, userId, robotId, startTime, endTime, pageable);
        return taskPage.map(this::toDTO);
    }
    
    /**
     * 查询所有任务
     */
    public List<TaskDTO> getAllTasks() {
        return taskRepository.findAll(Sort.by("createTime").descending()).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 查询用户的任务
     */
    public List<TaskDTO> getTasksByUserId(Long userId) {
        return taskRepository.findByUserIdOrderByCreateTimeDesc(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 启动任务
     */
    @Transactional
    public TaskDTO startTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + id));
        
        if (!"pending".equals(task.getStatus())) {
            throw new RuntimeException("只有等待中的任务可以启动");
        }
        
        task.setStatus("running");
        task.setStartTime(LocalDateTime.now());
        task.setProgress(0);
        
        task = taskRepository.save(task);
        log.info("启动任务成功: {}", task.getTaskId());
        
        // 异步执行任务
        robotExecutor.executeTaskAsync(id);
        
        return toDTO(task);
    }
    
    /**
     * 停止任务
     */
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
        log.info("停止任务成功: {}", task.getTaskId());
        
        return toDTO(task);
    }
    
    /**
     * 完成任务
     */
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
        log.info("完成任务成功: {}", task.getTaskId());
        
        return toDTO(task);
    }
    
    /**
     * 任务失败
     */
    @Transactional
    public TaskDTO failTask(Long id, String errorMessage) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + id));
        
        task.setStatus("failed");
        task.setEndTime(LocalDateTime.now());
        task.setErrorMessage(errorMessage);
        task.setDuration(calculateDuration(task.getStartTime(), task.getEndTime()));
        
        task = taskRepository.save(task);
        log.info("任务标记失败: {}", task.getTaskId());
        
        return toDTO(task);
    }
    
    /**
     * 更新任务进度
     */
    @Transactional
    public TaskDTO updateProgress(Long id, Integer progress) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + id));
        
        task.setProgress(Math.min(100, Math.max(0, progress)));
        
        task = taskRepository.save(task);
        return toDTO(task);
    }
    
    /**
     * 获取任务统计数据
     */
    public TaskStats getTaskStats() {
        TaskStats stats = new TaskStats();
        stats.setTotal(taskRepository.countAll());
        stats.setRunning(taskRepository.countRunning());
        stats.setCompleted(taskRepository.countCompleted());
        stats.setFailed(taskRepository.countFailed());
        stats.setPending(stats.getTotal() - stats.getRunning() - stats.getCompleted() - stats.getFailed());
        return stats;
    }
    
    /**
     * 生成任务编号
     */
    private String generateTaskId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "T" + timestamp + random;
    }
    
    /**
     * 计算执行耗时（秒）
     */
    private Integer calculateDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return (int) java.time.Duration.between(start, end).getSeconds();
    }
    
    /**
     * 转换为DTO
     */
    private TaskDTO toDTO(Task task) {
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
                .errorMessage(task.getErrorMessage())
                .createTime(task.getCreateTime())
                .updateTime(task.getUpdateTime())
                .build();
    }
    
    /**
     * 任务统计数据
     */
    @lombok.Data
    public static class TaskStats {
        private Long total;
        private Long pending;
        private Long running;
        private Long completed;
        private Long failed;
        private Integer avgDuration;
    }
    
    /**
     * 获取正在执行的任务列表
     */
    public List<TaskDTO> getRunningTasks(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "startTime"));
        List<Task> tasks = taskRepository.findByStatus("running", pageable);
        return tasks.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取任务执行详情
     */
    public Map<String, Object> getTaskExecutionDetail(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在: " + taskId));
        
        Map<String, Object> detail = new HashMap<>();
        detail.put("task", toDTO(task));
        detail.put("executionHistory", getExecutionHistory(taskId));
        detail.put("logs", getTaskLogs(taskId));
        
        return detail;
    }
    
    private List<Map<String, Object>> getExecutionHistory(Long taskId) {
        // TODO: 从数据库获取执行历史记录
        return List.of();
    }
    
    private List<Map<String, Object>> getTaskLogs(Long taskId) {
        // TODO: 从ExecutionLogService获取日志
        return List.of();
    }
}
