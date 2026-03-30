package com.rpa.management.dto;

import com.rpa.management.common.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusChangeRequest(
    @NotNull TaskStatus status,
    Integer progress
) {
}
