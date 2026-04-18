package com.rpa.management.service;

import com.rpa.management.dto.WorkflowDebugRunDTO;
import com.rpa.management.entity.Workflow;
import com.rpa.management.entity.WorkflowDebugRun;
import com.rpa.management.repository.WorkflowDebugRunRepository;
import com.rpa.management.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkflowDebugRunService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowDebugRunRepository workflowDebugRunRepository;
    private final WorkflowStepRunService workflowStepRunService;

    @Transactional
    public WorkflowDebugRun createRun(Long workflowId, String inputConfig, Long userId, String userName) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("Workflow not found: " + workflowId));
        WorkflowDebugRun run = new WorkflowDebugRun();
        run.setRunId(generateRunId());
        run.setWorkflowId(workflow.getId());
        run.setWorkflowCode(workflow.getWorkflowCode());
        run.setWorkflowName(workflow.getName());
        run.setInputConfig(StringUtils.hasText(inputConfig) ? inputConfig : "{}");
        run.setGraphSnapshot(StringUtils.hasText(workflow.getGraph()) ? workflow.getGraph() : workflow.getConfig());
        run.setUserId(userId);
        run.setUserName(userName);
        return workflowDebugRunRepository.save(run);
    }

    @Transactional
    public WorkflowDebugRun markRunning(Long id) {
        WorkflowDebugRun run = getEntity(id);
        run.setStatus("running");
        run.setProgress(0);
        run.setStartTime(LocalDateTime.now());
        run.setErrorMessage(null);
        run.setResult(null);
        return workflowDebugRunRepository.save(run);
    }

    @Transactional
    public WorkflowDebugRun updateProgress(Long id, int progress) {
        WorkflowDebugRun run = getEntity(id);
        run.setProgress(Math.max(0, Math.min(100, progress)));
        return workflowDebugRunRepository.save(run);
    }

    @Transactional
    public WorkflowDebugRun complete(Long id, String result) {
        WorkflowDebugRun run = getEntity(id);
        run.setStatus("completed");
        run.setProgress(100);
        run.setResult(result);
        run.setErrorMessage(null);
        if (run.getStartTime() == null) {
            run.setStartTime(LocalDateTime.now());
        }
        run.setEndTime(LocalDateTime.now());
        run.setDuration((int) Duration.between(run.getStartTime(), run.getEndTime()).getSeconds());
        return workflowDebugRunRepository.save(run);
    }

    @Transactional
    public WorkflowDebugRun fail(Long id, String errorMessage) {
        WorkflowDebugRun run = getEntity(id);
        run.setStatus("failed");
        run.setErrorMessage(errorMessage);
        if (run.getStartTime() == null) {
            run.setStartTime(LocalDateTime.now());
        }
        run.setEndTime(LocalDateTime.now());
        run.setDuration((int) Duration.between(run.getStartTime(), run.getEndTime()).getSeconds());
        return workflowDebugRunRepository.save(run);
    }

    public List<WorkflowDebugRunDTO> listRuns(Long workflowId) {
        return workflowDebugRunRepository.findByWorkflowIdOrderByCreateTimeDesc(workflowId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public WorkflowDebugRun getEntity(Long id) {
        return workflowDebugRunRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow debug run not found: " + id));
    }

    public WorkflowDebugRunDTO getByRunId(String runId) {
        WorkflowDebugRun run = workflowDebugRunRepository.findByRunId(runId)
                .orElseThrow(() -> new RuntimeException("Workflow debug run not found: " + runId));
        return toDTO(run);
    }

    public WorkflowDebugRunDTO toDTO(WorkflowDebugRun run) {
        return WorkflowDebugRunDTO.builder()
                .id(run.getId())
                .runId(run.getRunId())
                .workflowId(run.getWorkflowId())
                .workflowCode(run.getWorkflowCode())
                .workflowName(run.getWorkflowName())
                .status(run.getStatus())
                .progress(run.getProgress())
                .inputConfig(run.getInputConfig())
                .graphSnapshot(run.getGraphSnapshot())
                .result(run.getResult())
                .errorMessage(run.getErrorMessage())
                .startTime(run.getStartTime())
                .endTime(run.getEndTime())
                .duration(run.getDuration())
                .userId(run.getUserId())
                .userName(run.getUserName())
                .createTime(run.getCreateTime())
                .updateTime(run.getUpdateTime())
                .stepRuns(workflowStepRunService.listByDebugRunId(run.getId()))
                .build();
    }

    private String generateRunId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "D" + timestamp + random;
    }
}
