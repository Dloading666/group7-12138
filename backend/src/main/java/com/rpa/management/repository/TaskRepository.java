package com.rpa.management.repository;

import com.rpa.management.common.enums.TaskStatus;
import com.rpa.management.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> findByTaskNo(String taskNo);

    boolean existsByTaskNo(String taskNo);

    List<Task> findAllByOrderByCreatedAtDesc();

    List<Task> findAllByStatus(TaskStatus status);
}
