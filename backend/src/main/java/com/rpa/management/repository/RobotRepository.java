package com.rpa.management.repository;

import com.rpa.management.entity.Robot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RobotRepository extends JpaRepository<Robot, Long> {

    List<Robot> findAllByOrderByCreatedAtDesc();
}
