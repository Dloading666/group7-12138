package com.rpa.management.engine;

import com.rpa.management.entity.CollectConfig;
import com.rpa.management.entity.Robot;
import com.rpa.management.entity.Task;
import com.rpa.management.repository.CollectConfigRepository;
import com.rpa.management.repository.RobotRepository;
import com.rpa.management.repository.TaskRepository;
import com.rpa.management.service.ExecutionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private final WebCollector webCollector;
    private final ExecutionLogService executionLogService;
    
    /**
     * 异步执行任务
     */
    @Async
    public CompletableFuture<TaskExecutionResult> executeTaskAsync(Long taskId) {
        TaskExecutionResult result = new TaskExecutionResult();
        result.setTaskId(taskId);
        
        try {
            // 获取任务
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("任务不存在: " + taskId));
            
            // 检查任务状态
            if (!"pending".equals(task.getStatus()) && !"running".equals(task.getStatus())) {
                throw new RuntimeException("任务状态不允许执行: " + task.getStatus());
            }
            
            // 获取机器人
            if (task.getRobotId() == null) {
                throw new RuntimeException("任务未分配机器人");
            }
            
            Robot robot = robotRepository.findById(task.getRobotId())
                    .orElseThrow(() -> new RuntimeException("机器人不存在: " + task.getRobotId()));
            
            // 检查机器人状态
            if (!"online".equals(robot.getStatus()) && !"running".equals(robot.getStatus())) {
                throw new RuntimeException("机器人未在线: " + robot.getStatus());
            }
            
            // 更新任务和机器人状态
            task.setStatus("running");
            task.setStartTime(LocalDateTime.now());
            task.setProgress(0);
            taskRepository.save(task);
            
            robot.setStatus("running");
            robot.setLastExecuteTime(LocalDateTime.now());
            robotRepository.save(robot);
            
            // 记录日志
            executionLogService.info(taskId, task.getTaskId(), task.getName(), 
                    robot.getId(), robot.getName(), "开始执行任务");
            
            // 根据任务类型执行不同的工作
            int processedCount = 0;
            String taskType = task.getType();
            
            if ("data-collection".equals(taskType) || "data_collection".equals(taskType)) {
                // 数据采集任务
                processedCount = executeDataCollection(task, robot);
            } else if ("report".equals(taskType)) {
                // 报表生成任务
                processedCount = executeReportGeneration(task, robot);
            } else if ("data-sync".equals(taskType) || "data_sync".equals(taskType)) {
                // 数据同步任务
                processedCount = executeDataSync(task, robot);
            } else {
                // 默认任务
                processedCount = executeDefaultTask(task, robot);
            }
            
            // 更新任务完成
            task.setStatus("completed");
            task.setProgress(100);
            task.setEndTime(LocalDateTime.now());
            task.setDuration(calculateDuration(task.getStartTime(), task.getEndTime()));
            task.setResult("执行成功，处理了 " + processedCount + " 条数据");
            taskRepository.save(task);
            
            // 更新机器人状态
            robot.setStatus("online");
            robot.setTotalTasks(robot.getTotalTasks() + 1);
            robot.setSuccessTasks(robot.getSuccessTasks() + 1);
            robot.setSuccessRate((double) robot.getSuccessTasks() / robot.getTotalTasks());
            robotRepository.save(robot);
            
            // 记录日志
            executionLogService.info(taskId, task.getTaskId(), task.getName(), 
                    robot.getId(), robot.getName(), "任务执行成功，处理了 " + processedCount + " 条数据");
            
            result.setSuccess(true);
            result.setMessage("执行成功");
            result.setProcessedCount(processedCount);
            
        } catch (Exception e) {
            log.error("任务执行失败: {}", e.getMessage(), e);
            
            // 获取任务和机器人用于更新状态
            Task task = taskRepository.findById(taskId).orElse(null);
            Robot robot = null;
            if (task != null && task.getRobotId() != null) {
                robot = robotRepository.findById(task.getRobotId()).orElse(null);
            }
            
            // 更新任务失败
            if (task != null) {
                task.setStatus("failed");
                task.setEndTime(LocalDateTime.now());
                task.setErrorMessage(e.getMessage());
                task.setDuration(calculateDuration(task.getStartTime(), task.getEndTime()));
                taskRepository.save(task);
            }
            
            // 更新机器人状态
            if (robot != null) {
                robot.setStatus("online");
                robot.setTotalTasks(robot.getTotalTasks() + 1);
                robot.setFailedTasks(robot.getFailedTasks() + 1);
                robot.setSuccessRate(robot.getTotalTasks() > 0 ? 
                        (double) robot.getSuccessTasks() / robot.getTotalTasks() : 0.0);
                robotRepository.save(robot);
            }
            
            // 记录日志
            if (task != null && robot != null) {
                executionLogService.error(taskId, task.getTaskId(), task.getName(), 
                        robot.getId(), robot.getName(), "任务执行失败: " + e.getMessage());
            }
            
            result.setSuccess(false);
            result.setMessage(e.getMessage());
        }
        
        return CompletableFuture.completedFuture(result);
    }
    
    /**
     * 执行数据采集任务
     */
    private int executeDataCollection(Task task, Robot robot) {
        log.info("执行数据采集任务: {}", task.getName());
        
        // 查找任务关联的采集配置
        CollectConfig config = collectConfigRepository.findByTaskId(task.getId())
                .stream()
                .findFirst()
                .orElse(null);
        
        if (config == null) {
            // 如果没有关联配置，创建默认配置
            executionLogService.info(task.getId(), task.getTaskId(), task.getName(), 
                    robot.getId(), robot.getName(), "未找到采集配置，使用默认配置");
            return 0;
        }
        
        // 更新进度
        updateTaskProgress(task.getId(), 20);
        
        // 执行采集
        int count = webCollector.collect(config, task.getId(), robot.getId());
        
        // 更新进度
        updateTaskProgress(task.getId(), 100);
        
        // 更新采集配置统计
        config.setTotalCount(config.getTotalCount() + 1);
        config.setSuccessCount(config.getSuccessCount() + 1);
        config.setLastExecuteTime(LocalDateTime.now());
        config.setLastExecuteStatus("success");
        collectConfigRepository.save(config);
        
        return count;
    }
    
    /**
     * 执行报表生成任务
     */
    private int executeReportGeneration(Task task, Robot robot) {
        log.info("执行报表生成任务: {}", task.getName());
        
        executionLogService.info(task.getId(), task.getTaskId(), task.getName(), 
                robot.getId(), robot.getName(), "开始生成报表");
        
        // 模拟报表生成过程
        updateTaskProgress(task.getId(), 30);
        sleep(1000);
        
        updateTaskProgress(task.getId(), 60);
        sleep(1000);
        
        updateTaskProgress(task.getId(), 90);
        sleep(500);
        
        executionLogService.info(task.getId(), task.getTaskId(), task.getName(), 
                robot.getId(), robot.getName(), "报表生成完成");
        
        return 1;
    }
    
    /**
     * 执行数据同步任务
     */
    private int executeDataSync(Task task, Robot robot) {
        log.info("执行数据同步任务: {}", task.getName());
        
        executionLogService.info(task.getId(), task.getTaskId(), task.getName(), 
                robot.getId(), robot.getName(), "开始数据同步");
        
        // 模拟数据同步过程
        updateTaskProgress(task.getId(), 25);
        sleep(800);
        
        updateTaskProgress(task.getId(), 50);
        sleep(800);
        
        updateTaskProgress(task.getId(), 75);
        sleep(800);
        
        executionLogService.info(task.getId(), task.getTaskId(), task.getName(), 
                robot.getId(), robot.getName(), "数据同步完成");
        
        return 10;
    }
    
    /**
     * 执行默认任务
     */
    private int executeDefaultTask(Task task, Robot robot) {
        log.info("执行默认任务: {}", task.getName());
        
        executionLogService.info(task.getId(), task.getTaskId(), task.getName(), 
                robot.getId(), robot.getName(), "开始执行任务");
        
        // 模拟任务执行过程
        for (int i = 0; i <= 100; i += 10) {
            updateTaskProgress(task.getId(), i);
            sleep(500);
        }
        
        executionLogService.info(task.getId(), task.getTaskId(), task.getName(), 
                robot.getId(), robot.getName(), "任务执行完成");
        
        return 1;
    }
    
    /**
     * 更新任务进度
     */
    private void updateTaskProgress(Long taskId, int progress) {
        taskRepository.findById(taskId).ifPresent(t -> {
            t.setProgress(progress);
            taskRepository.save(t);
        });
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
     * 休眠
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 任务执行结果
     */
    @lombok.Data
    public static class TaskExecutionResult {
        private Long taskId;
        private boolean success;
        private String message;
        private int processedCount;
    }
}
