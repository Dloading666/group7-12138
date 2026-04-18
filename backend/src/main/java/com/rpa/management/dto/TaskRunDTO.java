package com.rpa.management.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskRunDTO {

    private Long id;

    private String runId;

    private Long taskId;

    private String taskCode;

    private String taskName;

    private Long workflowVersionId;

    private String workflowName;

    private String workflowCategory;

    private String status;

    private Integer progress;

    private String triggerType;

    private String engineRunId;

    private String inputConfig;

    private String workflowSnapshot;

    private String result;

    private String errorMessage;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer duration;

    private Long userId;

    private String userName;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
