package com.rpa.management.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WorkflowStepRunDTO {

    private Long id;

    private String stepRunId;

    private Long taskRunId;

    private Long debugRunId;

    private String nodeId;

    private String nodeType;

    private String nodeLabel;

    private String branchKey;

    private String engineTaskId;

    private String status;

    private String inputSnapshot;

    private String outputSnapshot;

    private String errorMessage;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer duration;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
