package com.rpa.management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_workflow_version")
public class WorkflowVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_id", nullable = false)
    private Long workflowId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "workflow_code", nullable = false, length = 100)
    private String workflowCode;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(length = 100)
    private String category;

    @Column(name = "publish_status", length = 20)
    private String publishStatus;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_name", length = 100)
    private String userName;

    @CreationTimestamp
    @Column(name = "publish_time", updatable = false)
    private LocalDateTime publishTime;

    @Lob
    @Column(name = "input_schema", columnDefinition = "LONGTEXT")
    private String inputSchema;

    @Lob
    @Column(name = "graph", columnDefinition = "LONGTEXT")
    private String graph;
}
