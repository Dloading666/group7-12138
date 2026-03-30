package com.rpa.management.service;

import com.rpa.management.common.enums.TaskStatus;
import com.rpa.management.common.exception.BadRequestBusinessException;
import com.rpa.management.common.exception.ForbiddenBusinessException;
import com.rpa.management.common.exception.ResourceNotFoundException;
import com.rpa.management.dto.TaskDto;
import com.rpa.management.dto.TaskStatusChangeRequest;
import com.rpa.management.dto.TaskUpsertRequest;
import com.rpa.management.entity.Task;
import com.rpa.management.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public List<TaskDto> listAll() {
        return taskRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
            .map(TaskDto::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public TaskDto getById(Long id) {
        return TaskDto.from(findTask(id));
    }

    @Transactional
    public TaskDto create(TaskUpsertRequest request) {
        if (taskRepository.existsByTaskNo(request.taskNo())) {
            throw new BadRequestBusinessException("Task number already exists");
        }
        Task task = buildTask(new Task(), request);
        return TaskDto.from(taskRepository.save(task));
    }

    @Transactional
    public TaskDto update(Long id, TaskUpsertRequest request) {
        Task task = findTask(id);
        taskRepository.findByTaskNo(request.taskNo())
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> { throw new BadRequestBusinessException("Task number already exists"); });
        buildTask(task, request);
        return TaskDto.from(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long id) {
        Task task = findTask(id);
        if (task.getStatus() == TaskStatus.RUNNING) {
            throw new ForbiddenBusinessException("Running task cannot be deleted");
        }
        taskRepository.delete(task);
    }

    @Transactional
    public TaskDto changeStatus(Long id, TaskStatusChangeRequest request) {
        Task task = findTask(id);
        if (task.getStatus() == TaskStatus.RUNNING && request.status() == TaskStatus.PENDING) {
            throw new ForbiddenBusinessException("Running task cannot be reverted to pending");
        }
        if (request.status() == TaskStatus.RUNNING && task.getStatus() != TaskStatus.PENDING) {
            throw new ForbiddenBusinessException("Only pending task can start");
        }
        task.setStatus(request.status());
        if (request.progress() != null) {
            task.setProgress(request.progress());
        }
        if (request.status() == TaskStatus.RUNNING && task.getStartTime() == null) {
            task.setStartTime(LocalDateTime.now());
        }
        if ((request.status() == TaskStatus.COMPLETED || request.status() == TaskStatus.FAILED) && task.getEndTime() == null) {
            task.setEndTime(LocalDateTime.now());
            if (task.getStartTime() != null) {
                task.setDuration((int) Duration.between(task.getStartTime(), task.getEndTime()).toSeconds());
            }
        }
        return TaskDto.from(taskRepository.save(task));
    }

    @Transactional
    public TaskDto start(Long id) {
        return changeStatus(id, new TaskStatusChangeRequest(TaskStatus.RUNNING, 10));
    }

    @Transactional
    public TaskDto stop(Long id) {
        Task task = findTask(id);
        if (task.getStatus() != TaskStatus.RUNNING) {
            throw new ForbiddenBusinessException("Only running task can stop");
        }
        task.setStatus(TaskStatus.FAILED);
        task.setEndTime(LocalDateTime.now());
        if (task.getStartTime() != null) {
            task.setDuration((int) Duration.between(task.getStartTime(), task.getEndTime()).toSeconds());
        }
        return TaskDto.from(taskRepository.save(task));
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    }

    private Task buildTask(Task task, TaskUpsertRequest request) {
        task.setTaskNo(request.taskNo())
            .setName(request.name())
            .setType(request.type())
            .setStatus(request.status())
            .setProgress(request.progress() == null ? 0 : request.progress())
            .setPriority(request.priority())
            .setExecuteType(request.executeType())
            .setScheduleTime(request.scheduleTime())
            .setRobotId(request.robotId())
            .setCreatedByUserId(request.createdByUserId())
            .setParams(request.params())
            .setResult(request.result());
        return task;
    }
}
