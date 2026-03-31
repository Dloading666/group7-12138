package com.rpa.management.config;

import com.rpa.management.common.enums.RobotStatus;
import com.rpa.management.common.enums.TaskStatus;
import com.rpa.management.entity.Robot;
import com.rpa.management.entity.Task;
import com.rpa.management.repository.RobotRepository;
import com.rpa.management.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * 任务执行引擎：模拟机器人真实执行任务的过程
 * <ul>
 *   <li>每 3 秒推进 RUNNING 任务的进度（5~15%）</li>
 *   <li>进度达 100% 时自动标记为 COMPLETED，并释放机器人</li>
 *   <li>每 12 秒更新 ONLINE/BUSY 机器人的心跳时间</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskExecutionEngine {

    private final TaskRepository taskRepository;
    private final RobotRepository robotRepository;
    private final Random random = new Random();

    // ─────────────────────────────────────────────────────────────────────────
    // 任务进度推进（每 3 秒一次）
    // ─────────────────────────────────────────────────────────────────────────

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void advanceRunningTasks() {
        List<Task> running = taskRepository.findAllByStatus(TaskStatus.RUNNING);
        if (running.isEmpty()) return;

        for (Task task : running) {
            int step = 5 + random.nextInt(11);          // 每步推进 5~15%
            int newProgress = Math.min(task.getProgress() + step, 100);
            task.setProgress(newProgress);

            if (newProgress >= 100) {
                completeTask(task);
            }
            taskRepository.save(task);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 机器人心跳更新（每 12 秒一次）
    // ─────────────────────────────────────────────────────────────────────────

    @Scheduled(fixedDelay = 12000)
    @Transactional
    public void refreshRobotHeartbeats() {
        List<Robot> active = robotRepository.findAllByStatusIn(
                List.of(RobotStatus.ONLINE, RobotStatus.BUSY));
        if (active.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();
        for (Robot robot : active) {
            robot.setLastHeartbeat(now);
            robotRepository.save(robot);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 私有辅助
    // ─────────────────────────────────────────────────────────────────────────

    private void completeTask(Task task) {
        task.setStatus(TaskStatus.COMPLETED);
        task.setProgress(100);
        task.setEndTime(LocalDateTime.now());
        if (task.getStartTime() != null) {
            task.setDuration(
                    (int) Duration.between(task.getStartTime(), task.getEndTime()).toSeconds());
        }
        log.info("Task [{}] {} completed in {} s",
                task.getTaskNo(), task.getName(), task.getDuration());

        // 释放机器人并更新统计
        if (task.getRobotId() != null) {
            robotRepository.findById(task.getRobotId()).ifPresent(robot -> {
                robot.setStatus(RobotStatus.ONLINE);
                robot.setLastHeartbeat(LocalDateTime.now());
                robot.setTaskCount(robot.getTaskCount() + 1);
                recalcSuccessRate(robot, true);
                robotRepository.save(robot);
                log.info("Robot [{}] released back to ONLINE, totalTasks={}",
                        robot.getName(), robot.getTaskCount());
            });
        }
    }

    /**
     * 滚动更新成功率：新成功率 = (旧成功率 × 旧任务数 + 本次结果) / (旧任务数 + 1)
     */
    private void recalcSuccessRate(Robot robot, boolean success) {
        int prevCount = robot.getTaskCount(); // completeTask 里已 +1 前的值，此处仍是旧值
        if (prevCount < 0) prevCount = 0;
        double prevRate = robot.getSuccessRate() != null
                ? robot.getSuccessRate().doubleValue() : 100.0;
        double newRate = (prevRate * prevCount + (success ? 100.0 : 0.0)) / (prevCount + 1);
        robot.setSuccessRate(
                BigDecimal.valueOf(Math.min(100, Math.max(0, newRate)))
                        .setScale(2, RoundingMode.HALF_UP));
    }
}
