package com.rpa.management.repository;

import com.rpa.management.common.enums.RobotStatus;
import com.rpa.management.entity.Robot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RobotRepository extends JpaRepository<Robot, Long> {

    List<Robot> findAllByOrderByCreatedAtDesc();

    /** 找任务数最少的空闲机器人，用于自动分配 */
    Optional<Robot> findFirstByStatusOrderByTaskCountAsc(RobotStatus status);

    /** 查询在线/忙碌机器人，用于心跳更新 */
    List<Robot> findAllByStatusIn(List<RobotStatus> statuses);
}
