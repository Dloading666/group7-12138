package com.rpa.management.repository;

import com.rpa.management.entity.WorkflowVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowVersionRepository extends JpaRepository<WorkflowVersion, Long> {

    List<WorkflowVersion> findByWorkflowIdOrderByVersionNumberDesc(Long workflowId);

    List<WorkflowVersion> findByPublishStatusOrderByPublishTimeDesc(String publishStatus);
}
