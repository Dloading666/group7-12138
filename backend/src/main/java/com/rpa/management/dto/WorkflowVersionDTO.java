package com.rpa.management.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WorkflowVersionDTO {

    private Long id;

    private Long workflowId;

    private Integer versionNumber;

    private String workflowCode;

    private String name;

    private String description;

    private String category;

    private String publishStatus;

    private Long userId;

    private String userName;

    private LocalDateTime publishTime;

    private String inputSchema;

    private String graph;

    private Long crawlRobotId;

    private Long analysisRobotId;

    private Long notificationRobotId;
}
