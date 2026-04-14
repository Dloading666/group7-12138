package com.rpa.management.service;

import com.rpa.management.dto.RobotDTO;
import com.rpa.management.entity.Robot;
import com.rpa.management.repository.RobotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 机器人服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RobotService {
    
    private final RobotRepository robotRepository;
    
    // 机器人类型映射
    private static final Map<String, String> TYPE_MAP = new HashMap<>() {{
        put("data_collector", "数据采集机器人");
        put("report_generator", "报表生成机器人");
        put("task_scheduler", "任务调度机器人");
        put("notification", "消息通知机器人");
    }};
    
    // 状态映射
    private static final Map<String, String> STATUS_MAP = new HashMap<>() {{
        put("online", "在线");
        put("offline", "离线");
        put("running", "运行中");
    }};
    
    /**
     * 创建机器人
     */
    @Transactional
    public RobotDTO createRobot(RobotDTO dto) {
        // 如果传了robotCode，使用传的；否则自动生成
        String robotCode = dto.getRobotCode();
        if (!StringUtils.hasText(robotCode)) {
            robotCode = generateRobotCode(dto.getType());
        } else {
            // 检查编号是否已存在
            if (robotRepository.existsByRobotCode(robotCode)) {
                throw new RuntimeException("机器人编号已存在: " + robotCode);
            }
        }
        
        Robot robot = new Robot();
        robot.setRobotCode(robotCode);
        robot.setName(dto.getName());
        robot.setType(dto.getType());
        robot.setDescription(dto.getDescription());
        robot.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "offline");
        robot.setTotalTasks(0L);
        robot.setSuccessTasks(0L);
        robot.setFailedTasks(0L);
        robot.setSuccessRate(0.0);
        
        robot = robotRepository.save(robot);
        log.info("创建机器人成功: {}", robot.getRobotCode());
        
        return toDTO(robot);
    }
    
    /**
     * 生成机器人编号
     */
    private String generateRobotCode(String type) {
        String prefix = switch (type) {
            case "data_collector" -> "DC";
            case "report_generator" -> "RG";
            case "task_scheduler" -> "TS";
            case "notification" -> "NT";
            default -> "RB";
        };
        long count = robotRepository.count() + 1;
        return String.format("%s-%03d", prefix, count);
    }
    
    /**
     * 更新机器人
     */
    @Transactional
    public RobotDTO updateRobot(Long id, RobotDTO dto) {
        Robot robot = robotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("机器人不存在: " + id));
        
        if (StringUtils.hasText(dto.getName())) {
            robot.setName(dto.getName());
        }
        if (StringUtils.hasText(dto.getType())) {
            robot.setType(dto.getType());
        }
        if (dto.getDescription() != null) {
            robot.setDescription(dto.getDescription());
        }
        robot = robotRepository.save(robot);

        // 状态单独用 JPQL 更新，避免 Hibernate 一级缓存脏检测失效
        if (StringUtils.hasText(dto.getStatus())) {
            robotRepository.updateStatus(robot.getId(), dto.getStatus());
            robot = robotRepository.findById(robot.getId()).orElse(robot);
        }

        log.info("更新机器人成功: {}", robot.getRobotCode());
        return toDTO(robot);
    }
    
    /**
     * 单独更新状态
     */
    @Transactional
    public RobotDTO updateRobotStatus(Long id, String status) {
        Robot robot = robotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("机器人不存在: " + id));
        if ("running".equals(robot.getStatus())) {
            throw new RuntimeException("机器人运行中，不可修改状态");
        }
        robot.setStatus(status);
        robot = robotRepository.saveAndFlush(robot);
        log.info("更新机器人状态: {} -> {}", robot.getRobotCode(), status);
        return toDTO(robot);
    }

    /**
     * 启动机器人
     */
    @Transactional
    public RobotDTO startRobot(Long id) {
        Robot robot = robotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("机器人不存在: " + id));
        
        if ("running".equals(robot.getStatus())) {
            throw new RuntimeException("机器人已在运行中");
        }
        
        robot.setStatus("online");
        robot = robotRepository.save(robot);
        log.info("启动机器人成功: {}", robot.getRobotCode());
        
        return toDTO(robot);
    }
    
    /**
     * 停止机器人
     */
    @Transactional
    public RobotDTO stopRobot(Long id) {
        Robot robot = robotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("机器人不存在: " + id));
        
        if ("offline".equals(robot.getStatus())) {
            throw new RuntimeException("机器人已处于离线状态");
        }
        
        robot.setStatus("offline");
        robot = robotRepository.save(robot);
        log.info("停止机器人成功: {}", robot.getRobotCode());
        
        return toDTO(robot);
    }
    
    /**
     * 删除机器人
     */
    @Transactional
    public void deleteRobot(Long id) {
        Robot robot = robotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("机器人不存在: " + id));
        
        if ("running".equals(robot.getStatus())) {
            throw new RuntimeException("运行中的机器人不能删除，请先停止");
        }
        
        robotRepository.deleteById(id);
        log.info("删除机器人成功: {}", robot.getRobotCode());
    }
    
    /**
     * 获取机器人详情
     */
    public RobotDTO getRobotById(Long id) {
        Robot robot = robotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("机器人不存在: " + id));
        return toDTO(robot);
    }
    
    /**
     * 获取所有机器人
     */
    public List<RobotDTO> getAllRobots() {
        return robotRepository.findAll(Sort.by("createTime").descending()).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 分页查询机器人
     */
    public Page<RobotDTO> getRobotsByPage(String name, String type, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createTime").descending());
        String nameParam = StringUtils.hasText(name) ? name : null;
        String typeParam = StringUtils.hasText(type) ? type : null;
        String statusParam = StringUtils.hasText(status) ? status : null;
        Page<Robot> robotPage = robotRepository.findByConditions(nameParam, typeParam, statusParam, pageable);
        return robotPage.map(this::toDTO);
    }
    
    /**
     * 获取统计数据
     */
    public RobotStats getRobotStats() {
        RobotStats stats = new RobotStats();
        stats.setTotal(robotRepository.count());
        stats.setOnline(robotRepository.countOnlineRobots());
        stats.setOffline(robotRepository.countByStatus("offline"));
        stats.setRunning(robotRepository.countByStatus("running"));
        return stats;
    }
    
    /**
     * 模拟执行任务（用于测试）
     */
    @Transactional
    public RobotDTO executeTask(Long id, boolean success) {
        Robot robot = robotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("机器人不存在: " + id));
        
        robot.setTotalTasks(robot.getTotalTasks() + 1);
        if (success) {
            robot.setSuccessTasks(robot.getSuccessTasks() + 1);
        } else {
            robot.setFailedTasks(robot.getFailedTasks() + 1);
        }
        
        // 计算成功率
        if (robot.getTotalTasks() > 0) {
            robot.setSuccessRate((double) robot.getSuccessTasks() / robot.getTotalTasks() * 100);
        }
        
        robot.setLastExecuteTime(LocalDateTime.now());
        robot.setStatus("online");
        
        robot = robotRepository.save(robot);
        log.info("机器人执行任务: {}, 成功: {}", robot.getRobotCode(), success);
        
        return toDTO(robot);
    }
    
    /**
     * 转换为DTO
     */
    private RobotDTO toDTO(Robot robot) {
        return RobotDTO.builder()
                .id(robot.getId())
                .robotCode(robot.getRobotCode())
                .name(robot.getName())
                .type(robot.getType())
                .typeDisplayName(TYPE_MAP.getOrDefault(robot.getType(), robot.getType()))
                .description(robot.getDescription())
                .status(robot.getStatus())
                .statusDisplayName(STATUS_MAP.getOrDefault(robot.getStatus(), robot.getStatus()))
                .totalTasks(robot.getTotalTasks())
                .successTasks(robot.getSuccessTasks())
                .failedTasks(robot.getFailedTasks())
                .successRate(robot.getSuccessRate())
                .lastExecuteTime(robot.getLastExecuteTime())
                .currentTaskId(robot.getCurrentTaskId())
                .lastHeartbeat(robot.getLastHeartbeat())
                .createTime(robot.getCreateTime())
                .updateTime(robot.getUpdateTime())
                .build();
    }
    
    /**
     * 机器人统计数据
     */
    @lombok.Data
    public static class RobotStats {
        private long total;
        private long online;
        private long offline;
        private long running;
    }
}
