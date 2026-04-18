package com.rpa.management.repository;

import com.rpa.management.entity.WorkflowStepRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowStepRunRepository extends JpaRepository<WorkflowStepRun, Long> {

    Optional<WorkflowStepRun> findByStepRunId(String stepRunId);

    List<WorkflowStepRun> findByTaskRunIdOrderByCreateTimeAsc(Long taskRunId);

    List<WorkflowStepRun> findByDebugRunIdOrderByCreateTimeAsc(Long debugRunId);
}
