package com.rpa.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {

    private Long id;

    private String taskId;

    private String name;

    private String type;

    private Long workflowId;

    private Long workflowVersionId;

    private Integer workflowVersion;

    private String workflowName;

    private String workflowCategory;

    private String status;

    private Integer progress;

    private Long robotId;

    private String robotName;

    private String priority;

    private String executeType;

    private LocalDateTime scheduledTime;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer duration;

    private Long userId;

    private String userName;

    private String description;

    private String result;

    private String params;

    private String inputConfig;

    private String scheduleConfig;

    private Long latestRunId;

    private String latestRunStatus;

    private LocalDateTime lastRunTime;

    private LocalDateTime nextRunTime;

    private Long runCount;

    private String errorMessage;

    private LocalDateTime createTime;

    private String taxId;

    private String enterpriseName;

    private LocalDateTime updateTime;

    private String crawlUrl;

    private Integer crawlTimeout;

    private Boolean hasHeaders;

    private Boolean hasCookies;

    private Boolean hasPagination;

    private Integer extractionRuleCount;

    private Long sourceTaskRecordId;

    private String sourceTaskId;

    private String sourceTaskName;

    private String sourceTitle;

    private String sourceFinalUrl;

    private String query;
}
