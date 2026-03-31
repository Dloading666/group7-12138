package com.rpa.management.service;

import com.rpa.management.common.exception.BadRequestBusinessException;
import com.rpa.management.common.exception.ResourceNotFoundException;
import com.rpa.management.common.enums.ExecuteType;
import com.rpa.management.common.enums.TaskPriority;
import com.rpa.management.common.enums.TaskStatus;
import com.rpa.management.dto.TaskUpsertRequest;
import com.rpa.management.dto.WorkflowDto;
import com.rpa.management.dto.WorkflowSaveRequest;
import com.rpa.management.entity.Task;
import com.rpa.management.repository.RobotRepository;
import com.rpa.management.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final TaskRepository taskRepository;
    private final TaskService taskService;
    private final RobotRepository robotRepository;

    @Transactional(readOnly = true)
    public List<WorkflowDto> listAll() {
        return taskRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
            .filter(task -> task.getTaskNo() != null && task.getTaskNo().startsWith("WF-"))
            .map(this::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public WorkflowDto getById(Long id) {
        return toDto(findTask(id));
    }

    @Transactional
    public WorkflowDto create(WorkflowSaveRequest request) {
        if (taskRepository.existsByTaskNo(request.workflowNo())) {
            throw new BadRequestBusinessException("Workflow number already exists");
        }
        taskService.create(toTaskUpsertRequest(request));
        return getByWorkflowNo(request.workflowNo());
    }

    @Transactional
    public WorkflowDto update(Long id, WorkflowSaveRequest request) {
        findTask(id);
        taskRepository.findByTaskNo(request.workflowNo())
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> { throw new BadRequestBusinessException("Workflow number already exists"); });
        taskService.update(id, toTaskUpsertRequest(request));
        return getById(id);
    }

    @Transactional
    public void delete(Long id) {
        taskRepository.delete(findTask(id));
    }

    private WorkflowDto getByWorkflowNo(String workflowNo) {
        return toDto(taskRepository.findByTaskNo(workflowNo)
            .orElseThrow(() -> new ResourceNotFoundException("Workflow not found")));
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Workflow not found"));
    }

    private WorkflowDto toDto(Task task) {
        String robotName = task.getRobotId() == null ? null : robotRepository.findById(task.getRobotId())
            .map(robot -> robot.getName())
            .orElse(null);
        return new WorkflowDto(
            task.getId(),
            task.getTaskNo(),
            task.getName(),
            task.getType(),
            task.getStatus(),
            task.getProgress(),
            task.getPriority(),
            task.getExecuteType(),
            task.getScheduleTime(),
            task.getRobotId(),
            robotName,
            task.getCreatedByUserId(),
            task.getParams(),
            task.getResult(),
            task.getCreatedAt(),
            task.getStartTime(),
            task.getEndTime(),
            task.getDuration()
        );
    }

    private TaskUpsertRequest toTaskUpsertRequest(WorkflowSaveRequest request) {
        return new TaskUpsertRequest(
            request.workflowNo(),
            request.name(),
            request.type(),
            request.status() == null ? TaskStatus.PENDING : request.status(),
            request.progress(),
            request.priority() == null ? TaskPriority.MEDIUM : request.priority(),
            request.executeType() == null ? ExecuteType.IMMEDIATE : request.executeType(),
            request.scheduleTime(),
            request.robotId(),
            request.createdByUserId(),
            request.definitionJson(),
            request.result()
        );
    }
}
