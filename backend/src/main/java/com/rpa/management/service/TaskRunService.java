package com.rpa.management.service;

import com.rpa.management.dto.TaskRunDTO;
import com.rpa.management.entity.Task;
import com.rpa.management.entity.TaskRun;
import com.rpa.management.repository.TaskRepository;
import com.rpa.management.repository.TaskRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskRunService {

    private final TaskRunRepository taskRunRepository;
    private final TaskRepository taskRepository;
    private final WorkflowStepRunService workflowStepRunService;

    @Transactional
    public TaskRun createRun(Task task, String triggerType, String workflowSnapshot) {
        TaskRun run = new TaskRun();
        run.setRunId(generateRunId());
        run.setTaskId(task.getId());
        run.setTaskCode(task.getTaskId());
        run.setTaskName(task.getName());
        run.setWorkflowVersionId(task.getWorkflowVersionId());
        run.setWorkflowName(task.getWorkflowName());
        run.setWorkflowCategory(task.getWorkflowCategory());
        run.setTriggerType(StringUtils.hasText(triggerType) ? triggerType : "manual");
        run.setInputConfig(resolveInputConfig(task));
        run.setWorkflowSnapshot(workflowSnapshot);
        run.setUserId(task.getUserId());
        run.setUserName(task.getUserName());
        return taskRunRepository.save(run);
    }

    @Transactional
    public TaskRun ensureBackfillRun(Task task) {
        List<TaskRun> runs = taskRunRepository.findByTaskIdOrderByCreateTimeDesc(task.getId());
        if (!runs.isEmpty()) {
            return runs.get(0);
        }
        if (!StringUtils.hasText(task.getResult())
                && !"completed".equals(task.getStatus())
                && !"failed".equals(task.getStatus())
                && task.getStartTime() == null) {
            return null;
        }

        TaskRun run = new TaskRun();
        run.setRunId(generateRunId());
        run.setTaskId(task.getId());
        run.setTaskCode(task.getTaskId());
        run.setTaskName(task.getName());
        run.setWorkflowVersionId(task.getWorkflowVersionId());
        run.setWorkflowName(task.getWorkflowName());
        run.setWorkflowCategory(task.getWorkflowCategory());
        run.setStatus(StringUtils.hasText(task.getStatus()) ? task.getStatus() : "completed");
        run.setProgress(task.getProgress() != null ? task.getProgress() : ("completed".equals(task.getStatus()) ? 100 : 0));
        run.setTriggerType("legacy_backfill");
        run.setInputConfig(resolveInputConfig(task));
        run.setResult(task.getResult());
        run.setErrorMessage(task.getErrorMessage());
        run.setStartTime(task.getStartTime() != null ? task.getStartTime() : task.getCreateTime());
        run.setEndTime(task.getEndTime());
        run.setDuration(task.getDuration() != null ? task.getDuration() : 0);
        run.setUserId(task.getUserId());
        run.setUserName(task.getUserName());
        TaskRun saved = taskRunRepository.save(run);
        task.setLatestRunId(saved.getId());
        task.setLatestRunStatus(saved.getStatus());
        task.setLastRunTime(saved.getEndTime() != null ? saved.getEndTime() : saved.getCreateTime());
        return saved;
    }

    @Transactional
    public TaskRun markRunning(Task task, TaskRun run) {
        LocalDateTime now = LocalDateTime.now();
        run.setStatus("running");
        run.setProgress(0);
        if (run.getStartTime() == null) {
            run.setStartTime(now);
        }
        TaskRun saved = taskRunRepository.save(run);
        syncTaskFromRun(task, saved);
        task.setStartTime(saved.getStartTime());
        task.setEndTime(null);
        task.setDuration(0);
        task.setErrorMessage(null);
        task.setResult(null);
        taskRepository.save(task);
        return saved;
    }

    @Transactional
    public TaskRun updateProgress(Task task, Long runId, int progress) {
        TaskRun run = getRunEntity(runId);
        run.setProgress(Math.max(0, Math.min(100, progress)));
        TaskRun saved = taskRunRepository.save(run);
        syncTaskFromRun(task, saved);
        taskRepository.save(task);
        return saved;
    }

    @Transactional
    public TaskRun completeRun(Task task, Long runId, String result) {
        TaskRun run = getRunEntity(runId);
        run.setStatus("completed");
        run.setProgress(100);
        run.setResult(result);
        run.setErrorMessage(null);
        if (run.getStartTime() == null) {
            run.setStartTime(LocalDateTime.now());
        }
        run.setEndTime(LocalDateTime.now());
        run.setDuration(calculateDuration(run.getStartTime(), run.getEndTime()));
        TaskRun saved = taskRunRepository.save(run);
        syncTaskFromRun(task, saved);
        task.setResult(result);
        task.setErrorMessage(null);
        task.setEndTime(saved.getEndTime());
        task.setDuration(saved.getDuration());
        taskRepository.save(task);
        return saved;
    }

    @Transactional
    public TaskRun failRun(Task task, Long runId, String errorMessage) {
        TaskRun run = getRunEntity(runId);
        run.setStatus("failed");
        if (run.getStartTime() == null) {
            run.setStartTime(LocalDateTime.now());
        }
        run.setEndTime(LocalDateTime.now());
        run.setDuration(calculateDuration(run.getStartTime(), run.getEndTime()));
        run.setErrorMessage(errorMessage);
        if (!StringUtils.hasText(run.getResult())) {
            run.setResult("执行失败");
        }
        TaskRun saved = taskRunRepository.save(run);
        syncTaskFromRun(task, saved);
        task.setErrorMessage(errorMessage);
        task.setResult(saved.getResult());
        task.setEndTime(saved.getEndTime());
        task.setDuration(saved.getDuration());
        taskRepository.save(task);
        return saved;
    }

    @Transactional
    public void bindEngineRunId(Long runId, String engineRunId) {
        if (!StringUtils.hasText(engineRunId)) {
            return;
        }
        TaskRun run = getRunEntity(runId);
        run.setEngineRunId(engineRunId);
        taskRunRepository.save(run);
    }

    public List<TaskRunDTO> listRuns(Long taskId) {
        return taskRunRepository.findByTaskIdOrderByCreateTimeDesc(taskId)
                .stream()
                .map(run -> toDTO(run, false))
                .collect(Collectors.toList());
    }

    public long countRuns(Long taskId) {
        return taskRunRepository.countByTaskId(taskId);
    }

    public Optional<TaskRun> findByRunId(String runId) {
        return taskRunRepository.findByRunId(runId);
    }

    public Optional<TaskRun> findByEngineRunId(String engineRunId) {
        return taskRunRepository.findByEngineRunId(engineRunId);
    }

    public TaskRun getRunEntity(Long runId) {
        return taskRunRepository.findById(runId)
                .orElseThrow(() -> new RuntimeException("任务运行不存在: " + runId));
    }

    public TaskRunDTO getRun(Long runId) {
        return toDTO(getRunEntity(runId), true);
    }

    @Transactional
    public void deleteByTaskId(Long taskId) {
        taskRunRepository.deleteByTaskId(taskId);
    }

    public void syncTaskFromRun(Task task, TaskRun run) {
        task.setLatestRunId(run.getId());
        task.setLatestRunStatus(run.getStatus());
        task.setStatus(run.getStatus());
        task.setProgress(run.getProgress());
        task.setLastRunTime(run.getEndTime() != null ? run.getEndTime() : run.getCreateTime());
        task.setStartTime(run.getStartTime());
        task.setEndTime(run.getEndTime());
        task.setDuration(run.getDuration());
    }

    public TaskRunDTO toDTO(TaskRun run) {
        return toDTO(run, false);
    }

    public TaskRunDTO toDTO(TaskRun run, boolean includeStepRuns) {
        return TaskRunDTO.builder()
                .id(run.getId())
                .runId(run.getRunId())
                .taskId(run.getTaskId())
                .taskCode(run.getTaskCode())
                .taskName(run.getTaskName())
                .workflowVersionId(run.getWorkflowVersionId())
                .workflowName(run.getWorkflowName())
                .workflowCategory(run.getWorkflowCategory())
                .status(run.getStatus())
                .progress(run.getProgress())
                .triggerType(run.getTriggerType())
                .engineRunId(run.getEngineRunId())
                .inputConfig(run.getInputConfig())
                .workflowSnapshot(run.getWorkflowSnapshot())
                .result(run.getResult())
                .errorMessage(run.getErrorMessage())
                .startTime(run.getStartTime())
                .endTime(run.getEndTime())
                .duration(run.getDuration())
                .userId(run.getUserId())
                .userName(run.getUserName())
                .createTime(run.getCreateTime())
                .updateTime(run.getUpdateTime())
                .stepRuns(includeStepRuns ? workflowStepRunService.listByTaskRunId(run.getId()) : null)
                .build();
    }

    private String resolveInputConfig(Task task) {
        if (StringUtils.hasText(task.getInputConfig())) {
            return task.getInputConfig();
        }
        return task.getParams();
    }

    private Integer calculateDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return (int) Duration.between(start, end).getSeconds();
    }

    private String generateRunId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "R" + timestamp + random;
    }
}
