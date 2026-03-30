package com.rpa.management.dto;

import com.rpa.management.common.enums.RobotStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RobotUpsertRequest(
    @NotBlank String name,
    String type,
    @NotNull RobotStatus status,
    String ipAddress,
    Integer port,
    String config,
    LocalDateTime lastHeartbeat,
    Integer taskCount,
    BigDecimal successRate
) {
}
