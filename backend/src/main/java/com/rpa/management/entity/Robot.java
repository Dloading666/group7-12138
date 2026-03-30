package com.rpa.management.entity;

import com.rpa.management.common.entity.BaseEntity;
import com.rpa.management.common.enums.RobotStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@Entity
@Table(name = "robots", indexes = {
    @Index(name = "idx_robots_status", columnList = "status"),
    @Index(name = "idx_robots_type", columnList = "type")
})
public class Robot extends BaseEntity {

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "robot_type", length = 64)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RobotStatus status = RobotStatus.OFFLINE;

    @Column(length = 64)
    private String ipAddress;

    private Integer port;

    @Column(columnDefinition = "LONGTEXT")
    private String config;

    private LocalDateTime lastHeartbeat;

    private Integer taskCount = 0;

    private BigDecimal successRate = BigDecimal.ZERO;
}
