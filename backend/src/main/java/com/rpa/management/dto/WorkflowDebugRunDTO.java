package com.rpa.management.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class WorkflowDebugRunDTO {

    private Long id;

    private String runId;

    private Long workflowId;

    private String workflowCode;

    private String workflowName;

    private String status;

    private Integer progress;

    private String inputConfig;

    private String graphSnapshot;

    private String result;

    private String errorMessage;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer duration;

    private Long userId;

    private String userName;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<WorkflowStepRunDTO> stepRuns;
}
