package com.rpa.management.service;

import com.rpa.management.common.enums.RobotStatus;
import com.rpa.management.common.exception.ForbiddenBusinessException;
import com.rpa.management.common.exception.ResourceNotFoundException;
import com.rpa.management.dto.RobotDto;
import com.rpa.management.dto.RobotStatusChangeRequest;
import com.rpa.management.dto.RobotUpsertRequest;
import com.rpa.management.entity.Robot;
import com.rpa.management.repository.RobotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RobotService {

    private final RobotRepository robotRepository;

    @Transactional(readOnly = true)
    public List<RobotDto> listAll() {
        return robotRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
            .map(RobotDto::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public RobotDto getById(Long id) {
        return RobotDto.from(findRobot(id));
    }

    @Transactional
    public RobotDto create(RobotUpsertRequest request) {
        Robot robot = buildRobot(new Robot(), request);
        return RobotDto.from(robotRepository.save(robot));
    }

    @Transactional
    public RobotDto update(Long id, RobotUpsertRequest request) {
        Robot robot = findRobot(id);
        buildRobot(robot, request);
        return RobotDto.from(robotRepository.save(robot));
    }

    @Transactional
    public void delete(Long id) {
        Robot robot = findRobot(id);
        if (robot.getStatus() == RobotStatus.ONLINE || robot.getStatus() == RobotStatus.BUSY) {
            throw new ForbiddenBusinessException("Busy robot cannot be deleted");
        }
        robotRepository.delete(robot);
    }

    @Transactional
    public RobotDto changeStatus(Long id, RobotStatusChangeRequest request) {
        Robot robot = findRobot(id);
        robot.setStatus(request.status());
        return RobotDto.from(robotRepository.save(robot));
    }

    @Transactional
    public RobotDto start(Long id) {
        Robot robot = findRobot(id);
        robot.setStatus(RobotStatus.ONLINE);
        return RobotDto.from(robotRepository.save(robot));
    }

    @Transactional
    public RobotDto stop(Long id) {
        Robot robot = findRobot(id);
        robot.setStatus(RobotStatus.OFFLINE);
        return RobotDto.from(robotRepository.save(robot));
    }

    private Robot findRobot(Long id) {
        return robotRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Robot not found"));
    }

    private Robot buildRobot(Robot robot, RobotUpsertRequest request) {
        robot.setName(request.name())
            .setType(request.type())
            .setStatus(request.status())
            .setIpAddress(request.ipAddress())
            .setPort(request.port())
            .setConfig(request.config())
            .setLastHeartbeat(request.lastHeartbeat())
            .setTaskCount(request.taskCount() == null ? 0 : request.taskCount())
            .setSuccessRate(request.successRate() == null ? java.math.BigDecimal.ZERO : request.successRate());
        return robot;
    }
}
