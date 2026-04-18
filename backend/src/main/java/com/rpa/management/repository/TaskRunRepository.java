package com.rpa.management.repository;

import com.rpa.management.entity.TaskRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRunRepository extends JpaRepository<TaskRun, Long> {

    Optional<TaskRun> findByRunId(String runId);

    Optional<TaskRun> findByEngineRunId(String engineRunId);

    List<TaskRun> findByTaskIdOrderByCreateTimeDesc(Long taskId);

    long countByTaskId(Long taskId);

    void deleteByTaskId(Long taskId);
}
