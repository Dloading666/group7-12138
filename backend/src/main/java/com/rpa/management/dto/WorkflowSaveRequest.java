package com.rpa.management.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.rpa.management.common.enums.ExecuteType;
import com.rpa.management.common.enums.TaskPriority;
import com.rpa.management.common.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record WorkflowSaveRequest(
    @JsonAlias({"taskNo", "code"})
    @NotBlank String workflowNo,
    @NotBlank String name,
    String type,
    TaskStatus status,
    Integer progress,
    TaskPriority priority,
    ExecuteType executeType,
    LocalDateTime scheduleTime,
    Long robotId,
    Long createdByUserId,
    @JsonAlias({"params", "definition", "definitionJson"})
    String definitionJson,
    String result
) {
}
