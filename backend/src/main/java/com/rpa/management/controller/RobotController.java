package com.rpa.management.controller;

import com.rpa.management.dto.ApiResponse;
import com.rpa.management.dto.RobotDTO;
import com.rpa.management.service.RobotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 机器人管理控制器
 */
@Slf4j
@Tag(name = "机器人管理", description = "机器人的增删改查和控制接口")
@RestController
@RequestMapping("/robots")
@RequiredArgsConstructor
public class RobotController {
    
    private final RobotService robotService;
    
    /**
     * 创建机器人
     */
    @Operation(summary = "创建机器人", description = "创建新的机器人")
    @PostMapping
    public ApiResponse<RobotDTO> createRobot(@RequestBody RobotDTO dto) {
        try {
            RobotDTO robot = robotService.createRobot(dto);
            return ApiResponse.success("创建机器人成功", robot);
        } catch (Exception e) {
            log.error("创建机器人失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 更新机器人
     */
    @Operation(summary = "更新机器人", description = "更新机器人信息")
    @PutMapping("/{id}")
    public ApiResponse<RobotDTO> updateRobot(
            @Parameter(description = "机器人ID") @PathVariable Long id,
            @RequestBody RobotDTO dto) {
        try {
            RobotDTO robot = robotService.updateRobot(id, dto);
            return ApiResponse.success("更新机器人成功", robot);
        } catch (Exception e) {
            log.error("更新机器人失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 删除机器人
     */
    @Operation(summary = "删除机器人", description = "删除指定机器人")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRobot(@Parameter(description = "机器人ID") @PathVariable Long id) {
        try {
            robotService.deleteRobot(id);
            return ApiResponse.success("删除机器人成功", null);
        } catch (Exception e) {
            log.error("删除机器人失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 获取机器人详情
     */
    @Operation(summary = "获取机器人详情", description = "根据ID获取机器人详细信息")
    @GetMapping("/{id}")
    public ApiResponse<RobotDTO> getRobotById(@Parameter(description = "机器人ID") @PathVariable Long id) {
        try {
            RobotDTO robot = robotService.getRobotById(id);
            return ApiResponse.success(robot);
        } catch (Exception e) {
            log.error("获取机器人失败: {}", e.getMessage());
            return ApiResponse.error(404, e.getMessage());
        }
    }
    
    /**
     * 获取所有机器人
     */
    @Operation(summary = "获取所有机器人", description = "获取所有机器人列表")
    @GetMapping("/all")
    public ApiResponse<List<RobotDTO>> getAllRobots() {
        List<RobotDTO> robots = robotService.getAllRobots();
        return ApiResponse.success(robots);
    }
    
    /**
     * 分页查询机器人
     */
    @Operation(summary = "分页查询机器人", description = "分页查询机器人列表，支持条件筛选")
    @GetMapping
    public ApiResponse<Page<RobotDTO>> getRobotsByPage(
            @Parameter(description = "机器人名称") @RequestParam(required = false) String name,
            @Parameter(description = "机器人类型") @RequestParam(required = false) String type,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        Page<RobotDTO> robotPage = robotService.getRobotsByPage(name, type, status, page, size);
        return ApiResponse.success(robotPage);
    }
    
    /**
     * 更新机器人状态
     */
    @Operation(summary = "更新机器人状态", description = "单独更新机器人状态字段")
    @PatchMapping("/{id}/status")
    public ApiResponse<RobotDTO> updateRobotStatus(
            @Parameter(description = "机器人ID") @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        try {
            String status = body.get("status");
            if (status == null || status.isBlank()) {
                return ApiResponse.error(400, "状态不能为空");
            }
            RobotDTO robot = robotService.updateRobotStatus(id, status);
            return ApiResponse.success("更新状态成功", robot);
        } catch (Exception e) {
            log.error("更新机器人状态失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 启动机器人
     */
    @Operation(summary = "启动机器人", description = "启动机器人运行")
    @PostMapping("/{id}/start")
    public ApiResponse<RobotDTO> startRobot(@Parameter(description = "机器人ID") @PathVariable Long id) {
        try {
            RobotDTO robot = robotService.startRobot(id);
            return ApiResponse.success("机器人启动成功", robot);
        } catch (Exception e) {
            log.error("启动机器人失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 停止机器人
     */
    @Operation(summary = "停止机器人", description = "停止机器人运行")
    @PostMapping("/{id}/stop")
    public ApiResponse<RobotDTO> stopRobot(@Parameter(description = "机器人ID") @PathVariable Long id) {
        try {
            RobotDTO robot = robotService.stopRobot(id);
            return ApiResponse.success("机器人停止成功", robot);
        } catch (Exception e) {
            log.error("停止机器人失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 获取机器人统计
     */
    @Operation(summary = "获取机器人统计", description = "获取机器人统计数据")
    @GetMapping("/stats")
    public ApiResponse<RobotService.RobotStats> getRobotStats() {
        RobotService.RobotStats stats = robotService.getRobotStats();
        return ApiResponse.success(stats);
    }
}
