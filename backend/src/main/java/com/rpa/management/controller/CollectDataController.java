package com.rpa.management.controller;

import com.rpa.management.dto.ApiResponse;
import com.rpa.management.entity.CollectData;
import com.rpa.management.repository.CollectDataRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 采集数据控制器
 */
@Slf4j
@Tag(name = "采集数据", description = "采集数据查询接口")
@RestController
@RequestMapping("/collect")
@RequiredArgsConstructor
public class CollectDataController {
    
    private final CollectDataRepository collectDataRepository;
    
    /**
     * 分页查询采集数据
     */
    @Operation(summary = "分页查询采集数据", description = "分页查询采集的数据列表")
    @GetMapping("/data")
    public ApiResponse<Page<CollectData>> getCollectData(
            @Parameter(description = "配置ID") @RequestParam(required = false) Long configId,
            @Parameter(description = "任务ID") @RequestParam(required = false) Long taskId,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;
        
        if (startTime != null && !startTime.isEmpty()) {
            startDateTime = LocalDateTime.parse(startTime + "T00:00:00");
        }
        if (endTime != null && !endTime.isEmpty()) {
            endDateTime = LocalDateTime.parse(endTime + "T23:59:59");
        }
        
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, size), Sort.by("collectTime").descending());
        Page<CollectData> dataPage = collectDataRepository.findByConditions(
                configId, taskId, status, startDateTime, endDateTime, pageable
        );
        
        return ApiResponse.success(dataPage);
    }
    
    /**
     * 获取采集数据详情
     */
    @Operation(summary = "获取采集数据详情", description = "根据ID获取采集数据详情")
    @GetMapping("/data/{id}")
    public ApiResponse<CollectData> getCollectDataById(
            @Parameter(description = "数据ID") @PathVariable Long id) {
        CollectData data = collectDataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("采集数据不存在: " + id));
        return ApiResponse.success(data);
    }

    /**
     * 删除采集数据
     */
    @Operation(summary = "删除采集数据", description = "根据ID删除采集数据")
    @DeleteMapping("/data/{id}")
    public ApiResponse<Void> deleteCollectData(
            @Parameter(description = "数据ID") @PathVariable Long id) {
        if (!collectDataRepository.existsById(id)) {
            return ApiResponse.error(404, "采集数据不存在: " + id);
        }
        collectDataRepository.deleteById(id);
        return ApiResponse.success("删除成功", null);
    }
}
