package com.rpa.management.scheduler;

import com.rpa.management.engine.RobotExecutor;
import com.rpa.management.entity.Task;
import com.rpa.management.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务执行器
 * 注意：类名不能叫TaskScheduler，会与Spring内置的taskScheduler Bean冲突
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ScheduledTaskExecutor {
    
    private final TaskRepository taskRepository;
    private final RobotExecutor robotExecutor;
    
    /**
     * 每分钟检查定时任务
     */
    @Scheduled(cron = "0 * * * * ?")
    public void checkScheduledTasks() {
        try {
            log.debug("检查定时任务...");
            
            // 查找需要执行的定时任务
            List<Task> tasks = taskRepository.findByStatus("pending");
            LocalDateTime now = LocalDateTime.now();
            
            for (Task task : tasks) {
                if ("scheduled".equals(task.getExecuteType()) && 
                    task.getScheduledTime() != null && 
                    !task.getScheduledTime().isAfter(now)) {
                    
                    log.info("触发定时任务: {} - {}", task.getTaskId(), task.getName());
                    
                    // 更新为立即执行
                    task.setExecuteType("immediate");
                    taskRepository.save(task);
                    
                    // 异步执行
                    robotExecutor.executeTaskAsync(task.getId());
                }
            }
        } catch (Exception e) {
            log.error("检查定时任务失败: {}", e.getMessage());
        }
    }
    
    /**
     * 每5分钟检查超时任务
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void checkTimeoutTasks() {
        try {
            log.debug("检查超时任务...");
            
            List<Task> runningTasks = taskRepository.findByStatus("running");
            LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(30);
            
            for (Task task : runningTasks) {
                if (task.getStartTime() != null && task.getStartTime().isBefore(timeoutThreshold)) {
                    log.warn("任务执行超时: {}", task.getTaskId());
                    
                    task.setStatus("failed");
                    task.setErrorMessage("任务执行超时");
                    task.setEndTime(LocalDateTime.now());
                    taskRepository.save(task);
                }
            }
        } catch (Exception e) {
            log.error("检查超时任务失败: {}", e.getMessage());
        }
    }
}
