package com.rpa.management.dto;

import com.rpa.management.common.enums.RobotStatus;
import jakarta.validation.constraints.NotNull;

public record RobotStatusChangeRequest(
    @NotNull RobotStatus status
) {
}
