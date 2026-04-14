package com.rpa.management.service;

import com.rpa.management.dto.ExecutionLogDTO;
import com.rpa.management.entity.ExecutionLog;
import com.rpa.management.repository.ExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 执行日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionLogService {
    
    private final ExecutionLogRepository executionLogRepository;
    
    /**
     * 记录日志
     */
    @Transactional
    public ExecutionLogDTO log(Long taskId, String taskCode, String taskName, 
                               Long robotId, String robotName, String level, 
                               String message, String stage) {
        ExecutionLog executionLog = new ExecutionLog();
        executionLog.setTaskId(taskId);
        executionLog.setTaskCode(taskCode);
        executionLog.setTaskName(taskName);
        executionLog.setRobotId(robotId);
        executionLog.setRobotName(robotName);
        executionLog.setLevel(level);
        executionLog.setMessage(message);
        executionLog.setStage(stage);
        
        executionLog = executionLogRepository.save(executionLog);
        return toDTO(executionLog);
    }
    
    /**
     * 记录INFO日志
     */
    public ExecutionLogDTO info(Long taskId, String taskCode, String taskName, 
                                Long robotId, String robotName, String message) {
        return log(taskId, taskCode, taskName, robotId, robotName, "INFO", message, null);
    }
    
    /**
     * 记录WARN日志
     */
    public ExecutionLogDTO warn(Long taskId, String taskCode, String taskName, 
                                Long robotId, String robotName, String message) {
        return log(taskId, taskCode, taskName, robotId, robotName, "WARN", message, null);
    }
    
    /**
     * 记录ERROR日志
     */
    public ExecutionLogDTO error(Long taskId, String taskCode, String taskName, 
                                 Long robotId, String robotName, String message) {
        return log(taskId, taskCode, taskName, robotId, robotName, "ERROR", message, "error");
    }
    
    /**
     * 分页查询日志
     */
    public Page<ExecutionLogDTO> getLogsByPage(String level, Long taskId, String taskCode, 
                                                Long robotId, LocalDateTime startTime, 
                                                LocalDateTime endTime, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createTime").descending());
        Page<ExecutionLog> logPage = executionLogRepository.findByConditions(
                level, taskId, taskCode, robotId, startTime, endTime, pageable
        );
        return logPage.map(this::toDTO);
    }
    
    /**
     * 获取任务的所有日志
     */
    public List<ExecutionLogDTO> getLogsByTaskId(Long taskId) {
        return executionLogRepository.findByTaskIdOrderByCreateTimeDesc(taskId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 清空所有日志
     */
    @Transactional
    public void clearAllLogs() {
        executionLogRepository.deleteAll();
        log.info("已清空所有执行日志");
    }
    
    /**
     * 清空指定天数之前的日志
     */
    @Transactional
    public void clearLogsBeforeDays(int days) {
        LocalDateTime time = LocalDateTime.now().minusDays(days);
        executionLogRepository.deleteByCreateTimeBefore(time);
        log.info("已清空{}天之前的执行日志", days);
    }
    
    /**
     * 转换为DTO
     */
    private ExecutionLogDTO toDTO(ExecutionLog log) {
        return ExecutionLogDTO.builder()
                .id(log.getId())
                .taskId(log.getTaskId())
                .taskCode(log.getTaskCode())
                .taskName(log.getTaskName())
                .robotId(log.getRobotId())
                .robotName(log.getRobotName())
                .level(log.getLevel())
                .message(log.getMessage())
                .stage(log.getStage())
                .extraData(log.getExtraData())
                .createTime(log.getCreateTime())
                .build();
    }
}
