package com.rpa.management.controller;

import com.rpa.management.dto.ApiResponse;
import com.rpa.management.dto.CollectConfigDTO;
import com.rpa.management.service.CollectConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 采集配置控制器
 */
@Slf4j
@Tag(name = "采集配置", description = "数据采集配置管理接口")
@RestController
@RequestMapping("/collect")
@RequiredArgsConstructor
public class CollectConfigController {
    
    private final CollectConfigService collectConfigService;
    
    /**
     * 创建采集配置
     */
    @Operation(summary = "创建采集配置", description = "创建新的数据采集配置")
    @PostMapping("/config")
    public ApiResponse<CollectConfigDTO> createConfig(
            @RequestBody CollectConfigDTO dto,
            HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            dto.setCreateBy(userId);
            
            CollectConfigDTO config = collectConfigService.createConfig(dto);
            return ApiResponse.success("创建采集配置成功", config);
        } catch (Exception e) {
            log.error("创建采集配置失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 更新采集配置
     */
    @Operation(summary = "更新采集配置", description = "更新采集配置信息")
    @PutMapping("/config/{id}")
    public ApiResponse<CollectConfigDTO> updateConfig(
            @Parameter(description = "配置ID") @PathVariable Long id,
            @RequestBody CollectConfigDTO dto) {
        try {
            CollectConfigDTO config = collectConfigService.updateConfig(id, dto);
            return ApiResponse.success("更新采集配置成功", config);
        } catch (Exception e) {
            log.error("更新采集配置失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 删除采集配置
     */
    @Operation(summary = "删除采集配置", description = "删除指定的采集配置")
    @DeleteMapping("/config/{id}")
    public ApiResponse<Void> deleteConfig(
            @Parameter(description = "配置ID") @PathVariable Long id) {
        try {
            collectConfigService.deleteConfig(id);
            return ApiResponse.success("删除采集配置成功", null);
        } catch (Exception e) {
            log.error("删除采集配置失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 获取采集配置详情
     */
    @Operation(summary = "获取采集配置详情", description = "根据ID获取采集配置详细信息")
    @GetMapping("/config/{id}")
    public ApiResponse<CollectConfigDTO> getConfigById(
            @Parameter(description = "配置ID") @PathVariable Long id) {
        try {
            CollectConfigDTO config = collectConfigService.getConfigById(id);
            return ApiResponse.success(config);
        } catch (Exception e) {
            log.error("获取采集配置失败: {}", e.getMessage());
            return ApiResponse.error(404, e.getMessage());
        }
    }
    
    /**
     * 分页查询采集配置
     */
    @Operation(summary = "分页查询采集配置", description = "分页查询采集配置列表")
    @GetMapping("/configs")
    public ApiResponse<Page<CollectConfigDTO>> getConfigsByPage(
            @Parameter(description = "配置名称") @RequestParam(required = false) String name,
            @Parameter(description = "采集类型") @RequestParam(required = false) String collectType,
            @Parameter(description = "是否启用") @RequestParam(required = false) Boolean isEnabled,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        Page<CollectConfigDTO> configPage = collectConfigService.getConfigsByPage(
                name, collectType, isEnabled, page, size
        );
        return ApiResponse.success(configPage);
    }
    
    /**
     * 获取所有启用的配置
     */
    @Operation(summary = "获取启用的配置", description = "获取所有启用状态的采集配置")
    @GetMapping("/configs/enabled")
    public ApiResponse<List<CollectConfigDTO>> getEnabledConfigs() {
        List<CollectConfigDTO> configs = collectConfigService.getEnabledConfigs();
        return ApiResponse.success(configs);
    }
    
    /**
     * 执行采集任务
     */
    @Operation(summary = "执行采集任务", description = "手动执行数据采集任务")
    @PostMapping("/execute/{configId}")
    public ApiResponse<Void> executeCollect(
            @Parameter(description = "配置ID") @PathVariable Long configId,
            @Parameter(description = "任务ID") @RequestParam(required = false) Long taskId,
            @Parameter(description = "任务编号") @RequestParam(required = false) String taskCode,
            @Parameter(description = "任务名称") @RequestParam(required = false) String taskName,
            @Parameter(description = "机器人ID") @RequestParam(required = false) Long robotId,
            @Parameter(description = "机器人名称") @RequestParam(required = false) String robotName,
            HttpServletRequest request) {
        try {
            collectConfigService.executeCollect(configId, taskId, taskCode, taskName, robotId, robotName);
            return ApiResponse.success("采集任务执行成功", null);
        } catch (Exception e) {
            log.error("执行采集任务失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
}
