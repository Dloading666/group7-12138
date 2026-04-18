package com.rpa.management.repository;

import com.rpa.management.entity.WorkflowDebugRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowDebugRunRepository extends JpaRepository<WorkflowDebugRun, Long> {

    Optional<WorkflowDebugRun> findByRunId(String runId);

    List<WorkflowDebugRun> findByWorkflowIdOrderByCreateTimeDesc(Long workflowId);
}
