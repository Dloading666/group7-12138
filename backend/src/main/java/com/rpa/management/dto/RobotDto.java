package com.rpa.management.dto;

import com.rpa.management.common.enums.RobotStatus;
import com.rpa.management.entity.Robot;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RobotDto(
    Long id,
    String name,
    String type,
    RobotStatus status,
    String ipAddress,
    Integer port,
    String config,
    LocalDateTime lastHeartbeat,
    Integer taskCount,
    BigDecimal successRate
) {
    public static RobotDto from(Robot robot) {
        return new RobotDto(
            robot.getId(),
            robot.getName(),
            robot.getType(),
            robot.getStatus(),
            robot.getIpAddress(),
            robot.getPort(),
            robot.getConfig(),
            robot.getLastHeartbeat(),
            robot.getTaskCount(),
            robot.getSuccessRate()
        );
    }
}
