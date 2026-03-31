package com.rpa.management.service;

import com.rpa.management.common.enums.RobotStatus;
import com.rpa.management.common.enums.TaskStatus;
import com.rpa.management.common.entity.BaseEntity;
import com.rpa.management.dto.MonitorExecutionLogDto;
import com.rpa.management.dto.MonitorOperationLogDto;
import com.rpa.management.dto.MonitorRealtimeDto;
import com.rpa.management.entity.ExecutionLog;
import com.rpa.management.entity.OperationLog;
import com.rpa.management.entity.Robot;
import com.rpa.management.entity.Task;
import com.rpa.management.repository.ExecutionLogRepository;
import com.rpa.management.repository.OperationLogRepository;
import com.rpa.management.repository.RobotRepository;
import com.rpa.management.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonitorService {

    private final TaskRepository taskRepository;
    private final RobotRepository robotRepository;
    private final ExecutionLogRepository executionLogRepository;
    private final OperationLogRepository operationLogRepository;

    @Transactional(readOnly = true)
    public MonitorRealtimeDto realtime() {
        List<Task> tasks = taskRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Robot> robots = robotRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        List<ExecutionLog> executionLogs = executionLogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        List<OperationLog> operationLogs = operationLogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        long runningTasks = tasks.stream().filter(task -> task.getStatus() == TaskStatus.RUNNING).count();
        long onlineRobots = robots.stream().filter(robot -> robot.getStatus() == RobotStatus.ONLINE).count();
        long busyRobots = robots.stream().filter(robot -> robot.getStatus() == RobotStatus.BUSY).count();
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        long executionCount24h = executionLogs.stream().filter(log -> log.getCreatedAt() != null && log.getCreatedAt().isAfter(cutoff)).count();
        long operationCount24h = operationLogs.stream().filter(log -> log.getCreatedAt() != null && log.getCreatedAt().isAfter(cutoff)).count();

        Map<TaskStatus, Long> taskStatusDistribution = new EnumMap<>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) {
            taskStatusDistribution.put(status, tasks.stream().filter(task -> task.getStatus() == status).count());
        }

        Map<RobotStatus, Long> robotStatusDistribution = new EnumMap<>(RobotStatus.class);
        for (RobotStatus status : RobotStatus.values()) {
            robotStatusDistribution.put(status, robots.stream().filter(robot -> robot.getStatus() == status).count());
        }

        Map<Long, Task> taskMap = tasks.stream().collect(Collectors.toMap(Task::getId, task -> task, (left, right) -> left));
        Map<Long, Robot> robotMap = robots.stream().collect(Collectors.toMap(Robot::getId, robot -> robot, (left, right) -> left));

        List<MonitorExecutionLogDto> recentExecutionLogs = executionLogs.stream()
            .limit(10)
            .map(log -> mapExecutionLog(log, taskMap, robotMap))
            .toList();

        List<MonitorOperationLogDto> recentOperationLogs = operationLogs.stream()
            .limit(10)
            .map(this::mapOperationLog)
            .toList();

        return new MonitorRealtimeDto(
            tasks.size(),
            runningTasks,
            onlineRobots,
            busyRobots,
            executionCount24h,
            operationCount24h,
            taskStatusDistribution.entrySet().stream()
                .map(entry -> new MonitorRealtimeDto.StatusCountDto(entry.getKey().name(), entry.getValue()))
                .toList(),
            robotStatusDistribution.entrySet().stream()
                .map(entry -> new MonitorRealtimeDto.StatusCountDto(entry.getKey().name(), entry.getValue()))
                .toList(),
            recentExecutionLogs,
            recentOperationLogs,
            LocalDateTime.now()
        );
    }

    @Transactional(readOnly = true)
    public Page<MonitorExecutionLogDto> executionLogs(String keyword, String level, Long taskId, String taskNo, Long robotId, int page, int size) {
        Long resolvedTaskId = resolveTaskId(taskId, taskNo);
        Page<ExecutionLog> result = executionLogRepository.findAll(buildExecutionLogSpec(keyword, level, resolvedTaskId, robotId),
            PageRequest.of(Math.max(page - 1, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "createdAt")));

        Map<Long, Task> taskMap = loadTaskMap(result.getContent());
        Map<Long, Robot> robotMap = loadRobotMap(result.getContent());
        return result.map(log -> mapExecutionLog(log, taskMap, robotMap));
    }

    @Transactional(readOnly = true)
    public Page<MonitorOperationLogDto> operationLogs(String keyword, String username, String status, int page, int size) {
        Page<OperationLog> result = operationLogRepository.findAll(buildOperationLogSpec(keyword, username, status),
            PageRequest.of(Math.max(page - 1, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "createdAt")));
        return result.map(this::mapOperationLog);
    }

    private Specification<ExecutionLog> buildExecutionLogSpec(String keyword, String level, Long taskId, Long robotId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();
            if (taskId != null) {
                predicates.add(cb.equal(root.get("taskId"), taskId));
            }
            if (robotId != null) {
                predicates.add(cb.equal(root.get("robotId"), robotId));
            }
            if (StringUtils.hasText(level)) {
                predicates.add(cb.equal(cb.lower(root.get("level")), level.trim().toLowerCase(Locale.ROOT)));
            }
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("level")), pattern),
                    cb.like(cb.lower(root.get("message")), pattern)
                ));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<OperationLog> buildOperationLogSpec(String keyword, String username, String status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();
            if (StringUtils.hasText(username)) {
                predicates.add(cb.like(cb.lower(root.get("username")), "%" + username.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(cb.lower(root.get("status")), status.trim().toLowerCase(Locale.ROOT)));
            }
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("operation")), pattern),
                    cb.like(cb.lower(root.get("method")), pattern),
                    cb.like(cb.lower(root.get("params")), pattern),
                    cb.like(cb.lower(root.get("errorMsg")), pattern)
                ));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Long resolveTaskId(Long taskId, String taskNo) {
        if (taskId != null) {
            return taskId;
        }
        if (StringUtils.hasText(taskNo)) {
            return taskRepository.findByTaskNo(taskNo.trim()).map(Task::getId).orElse(-1L);
        }
        return null;
    }

    private Map<Long, Task> loadTaskMap(Collection<ExecutionLog> logs) {
        Set<Long> taskIds = logs.stream().map(ExecutionLog::getTaskId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        if (taskIds.isEmpty()) {
            return Map.of();
        }
        return taskRepository.findAllById(taskIds).stream().collect(Collectors.toMap(Task::getId, task -> task));
    }

    private Map<Long, Robot> loadRobotMap(Collection<ExecutionLog> logs) {
        Set<Long> robotIds = logs.stream().map(ExecutionLog::getRobotId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        if (robotIds.isEmpty()) {
            return Map.of();
        }
        return robotRepository.findAllById(robotIds).stream().collect(Collectors.toMap(Robot::getId, robot -> robot));
    }

    private MonitorExecutionLogDto mapExecutionLog(ExecutionLog log, Map<Long, Task> taskMap, Map<Long, Robot> robotMap) {
        Task task = log.getTaskId() == null ? null : taskMap.get(log.getTaskId());
        Robot robot = log.getRobotId() == null ? null : robotMap.get(log.getRobotId());
        return new MonitorExecutionLogDto(
            log.getId(),
            log.getTaskId(),
            task == null ? null : task.getTaskNo(),
            task == null ? null : task.getName(),
            log.getRobotId(),
            robot == null ? null : robot.getName(),
            log.getLevel(),
            log.getMessage(),
            log.getCreatedAt()
        );
    }

    private MonitorOperationLogDto mapOperationLog(OperationLog log) {
        return new MonitorOperationLogDto(
            log.getId(),
            log.getUserId(),
            log.getUsername(),
            log.getOperation(),
            log.getMethod(),
            log.getParams(),
            log.getIp(),
            log.getStatus(),
            log.getErrorMsg(),
            log.getDuration(),
            log.getCreatedAt()
        );
    }
}
