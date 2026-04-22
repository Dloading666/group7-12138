package com.rpa.management.scheduler;

import com.rpa.management.entity.Task;
import com.rpa.management.repository.TaskRepository;
import com.rpa.management.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ScheduledTaskExecutor {

    private final TaskRepository taskRepository;
    private final TaskService taskService;

    @Scheduled(cron = "0 * * * * ?")
    public void checkScheduledTasks() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Task> tasks = taskRepository.findAll();

            for (Task task : tasks) {
                if ("running".equals(task.getStatus())) {
                    continue;
                }

                if ("scheduled".equalsIgnoreCase(task.getExecuteType())
                        && task.getScheduledTime() != null
                        && !task.getScheduledTime().isAfter(now)) {
                    log.info("触发一次性定时任务: {} - {}", task.getTaskId(), task.getName());
                    task.setExecuteType("immediate");
                    task.setNextRunTime(null);
                    taskRepository.save(task);
                    taskService.startTaskByScheduler(task.getId(), "scheduled");
                    continue;
                }

                if ("cron".equalsIgnoreCase(task.getExecuteType())
                        && task.getNextRunTime() != null
                        && !task.getNextRunTime().isAfter(now)) {
                    log.info("触发 Cron 任务: {} - {}", task.getTaskId(), task.getName());
                    taskService.startTaskByScheduler(task.getId(), "cron");
                }
            }
        } catch (Exception e) {
            log.error("检查调度任务失败", e);
        }
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void checkTimeoutTasks() {
        try {
            List<Task> runningTasks = taskRepository.findByStatus("running");
            LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(30);

            for (Task task : runningTasks) {
                if (task.getStartTime() != null && task.getStartTime().isBefore(timeoutThreshold)) {
                    log.warn("任务执行超时: {}", task.getTaskId());
                    taskService.failTask(task.getId(), "任务执行超时");
                }
            }
        } catch (Exception e) {
            log.error("检查超时任务失败", e);
        }
    }
}
