package com.rpa.management.service;

import com.rpa.management.dto.WorkflowStepRunDTO;
import com.rpa.management.entity.WorkflowStepRun;
import com.rpa.management.repository.WorkflowStepRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkflowStepRunService {

    private final WorkflowStepRunRepository workflowStepRunRepository;

    @Transactional
    public WorkflowStepRun createTaskStepRun(Long taskRunId,
                                             String nodeId,
                                             String nodeType,
                                             String nodeLabel,
                                             String branchKey,
                                             String inputSnapshot,
                                             Long robotId,
                                             String robotName,
                                             String robotType) {
        WorkflowStepRun stepRun = createBaseStepRun(nodeId, nodeType, nodeLabel, branchKey, inputSnapshot, robotId, robotName, robotType);
        stepRun.setTaskRunId(taskRunId);
        return workflowStepRunRepository.save(stepRun);
    }

    @Transactional
    public WorkflowStepRun createDebugStepRun(Long debugRunId,
                                              String nodeId,
                                              String nodeType,
                                              String nodeLabel,
                                              String branchKey,
                                              String inputSnapshot,
                                              Long robotId,
                                              String robotName,
                                              String robotType) {
        WorkflowStepRun stepRun = createBaseStepRun(nodeId, nodeType, nodeLabel, branchKey, inputSnapshot, robotId, robotName, robotType);
        stepRun.setDebugRunId(debugRunId);
        return workflowStepRunRepository.save(stepRun);
    }

    @Transactional
    public WorkflowStepRun start(WorkflowStepRun stepRun) {
        stepRun.setStatus("running");
        stepRun.setStartTime(LocalDateTime.now());
        return workflowStepRunRepository.save(stepRun);
    }

    @Transactional
    public WorkflowStepRun bindEngineTaskId(Long id, String engineTaskId) {
        WorkflowStepRun stepRun = getEntity(id);
        stepRun.setEngineTaskId(engineTaskId);
        return workflowStepRunRepository.save(stepRun);
    }

    @Transactional
    public WorkflowStepRun complete(Long id, String outputSnapshot) {
        WorkflowStepRun stepRun = getEntity(id);
        stepRun.setStatus("completed");
        stepRun.setOutputSnapshot(outputSnapshot);
        stepRun.setErrorMessage(null);
        if (stepRun.getStartTime() == null) {
            stepRun.setStartTime(LocalDateTime.now());
        }
        stepRun.setEndTime(LocalDateTime.now());
        stepRun.setDuration((int) Duration.between(stepRun.getStartTime(), stepRun.getEndTime()).getSeconds());
        return workflowStepRunRepository.save(stepRun);
    }

    @Transactional
    public WorkflowStepRun skip(Long id, String outputSnapshot) {
        WorkflowStepRun stepRun = getEntity(id);
        stepRun.setStatus("skipped");
        stepRun.setOutputSnapshot(outputSnapshot);
        stepRun.setErrorMessage(null);
        if (stepRun.getStartTime() == null) {
            stepRun.setStartTime(LocalDateTime.now());
        }
        stepRun.setEndTime(LocalDateTime.now());
        stepRun.setDuration((int) Duration.between(stepRun.getStartTime(), stepRun.getEndTime()).getSeconds());
        return workflowStepRunRepository.save(stepRun);
    }

    @Transactional
    public WorkflowStepRun fail(Long id, String errorMessage) {
        WorkflowStepRun stepRun = getEntity(id);
        stepRun.setStatus("failed");
        stepRun.setErrorMessage(errorMessage);
        if (stepRun.getStartTime() == null) {
            stepRun.setStartTime(LocalDateTime.now());
        }
        stepRun.setEndTime(LocalDateTime.now());
        stepRun.setDuration((int) Duration.between(stepRun.getStartTime(), stepRun.getEndTime()).getSeconds());
        return workflowStepRunRepository.save(stepRun);
    }

    public List<WorkflowStepRunDTO> listByTaskRunId(Long taskRunId) {
        return workflowStepRunRepository.findByTaskRunIdOrderByCreateTimeAsc(taskRunId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<WorkflowStepRunDTO> listByDebugRunId(Long debugRunId) {
        return workflowStepRunRepository.findByDebugRunIdOrderByCreateTimeAsc(debugRunId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public WorkflowStepRunDTO toDTO(WorkflowStepRun stepRun) {
        return WorkflowStepRunDTO.builder()
                .id(stepRun.getId())
                .stepRunId(stepRun.getStepRunId())
                .taskRunId(stepRun.getTaskRunId())
                .debugRunId(stepRun.getDebugRunId())
                .nodeId(stepRun.getNodeId())
                .nodeType(stepRun.getNodeType())
                .nodeLabel(stepRun.getNodeLabel())
                .branchKey(stepRun.getBranchKey())
                .engineTaskId(stepRun.getEngineTaskId())
                .robotId(stepRun.getRobotId())
                .robotName(stepRun.getRobotName())
                .robotType(stepRun.getRobotType())
                .status(stepRun.getStatus())
                .inputSnapshot(stepRun.getInputSnapshot())
                .outputSnapshot(stepRun.getOutputSnapshot())
                .errorMessage(stepRun.getErrorMessage())
                .startTime(stepRun.getStartTime())
                .endTime(stepRun.getEndTime())
                .duration(stepRun.getDuration())
                .createTime(stepRun.getCreateTime())
                .updateTime(stepRun.getUpdateTime())
                .build();
    }

    private WorkflowStepRun createBaseStepRun(String nodeId,
                                              String nodeType,
                                              String nodeLabel,
                                              String branchKey,
                                              String inputSnapshot,
                                              Long robotId,
                                              String robotName,
                                              String robotType) {
        WorkflowStepRun stepRun = new WorkflowStepRun();
        stepRun.setStepRunId(generateStepRunId());
        stepRun.setNodeId(nodeId);
        stepRun.setNodeType(nodeType);
        stepRun.setNodeLabel(nodeLabel);
        stepRun.setBranchKey(branchKey);
        stepRun.setInputSnapshot(inputSnapshot);
        stepRun.setRobotId(robotId);
        stepRun.setRobotName(robotName);
        stepRun.setRobotType(robotType);
        return stepRun;
    }

    private WorkflowStepRun getEntity(Long id) {
        return workflowStepRunRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow step run not found: " + id));
    }

    private String generateStepRunId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "S" + timestamp + random;
    }
}
