package com.rpa.management.repository;

import com.rpa.management.entity.WorkflowVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowVersionRepository extends JpaRepository<WorkflowVersion, Long> {

    List<WorkflowVersion> findByWorkflowIdOrderByVersionNumberDesc(Long workflowId);

    List<WorkflowVersion> findByPublishStatusOrderByPublishTimeDesc(String publishStatus);

    @Query("""
            SELECT version
            FROM WorkflowVersion version
            WHERE version.publishStatus = :publishStatus
              AND EXISTS (
                  SELECT workflow.id
                  FROM Workflow workflow
                  WHERE workflow.id = version.workflowId
              )
            ORDER BY version.publishTime DESC
            """)
    List<WorkflowVersion> findPublishedVersionsForActiveWorkflows(@Param("publishStatus") String publishStatus);
}
