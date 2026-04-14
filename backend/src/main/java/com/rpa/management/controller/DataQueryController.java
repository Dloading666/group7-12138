package com.rpa.management.controller;

import com.rpa.management.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据查询控制器 (Mock)
 */
@Slf4j
@Tag(name = "数据查询", description = "数据查询接口")
@RestController
@RequestMapping("/data")
public class DataQueryController {

    @Operation(summary = "分页查询数据", description = "分页查询业务数据列表")
    @GetMapping("/query")
    public ApiResponse<Page<Map<String, Object>>> queryData(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String taskId,
            @RequestParam(required = false) String taxAreaId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        List<Map<String, Object>> mockList = new ArrayList<>();
        
        // 模拟几条数据以供展示
        Map<String, Object> data1 = new HashMap<>();
        data1.put("id", 1L);
        data1.put("taskId", "T20240412001");
        data1.put("taxId", "91110000123456789A");
        data1.put("companyName", "某某科技有限公司");
        data1.put("taxAreaId", "A100");
        data1.put("status", "completed");
        data1.put("createTime", LocalDateTime.now().minusDays(1).toString());
        data1.put("dataContent", "{\"totalTax\": \"1500.00\", \"recordDate\": \"2024-04-11\"}");
        mockList.add(data1);

        Map<String, Object> data2 = new HashMap<>();
        data2.put("id", 2L);
        data2.put("taskId", "T20240412002");
        data2.put("taxId", "91110000987654321B");
        data2.put("companyName", "测试企业管理有限公司");
        data2.put("taxAreaId", "A101");
        data2.put("status", "running");
        data2.put("createTime", LocalDateTime.now().toString());
        data2.put("dataContent", "{\"totalTax\": \"0.00\", \"recordDate\": \"2024-04-12\"}");
        mockList.add(data2);
        
        // 如果有关键字搜索，简单过滤一下
        if (keyword != null && !keyword.isEmpty()) {
            mockList.removeIf(item -> !String.valueOf(item.get("companyName")).contains(keyword) 
                    && !String.valueOf(item.get("taxId")).contains(keyword));
        }

        int total = mockList.size();
        
        // 安全处理page参数，防止page < 1导致PageRequest.of报错
        int safePage = Math.max(1, page);
        int pageIndex = safePage - 1;
        int safeSize = Math.max(1, size);
        
        int startIndex = pageIndex * safeSize;
        int endIndex = Math.min(startIndex + safeSize, total);
        
        List<Map<String, Object>> pageData;
        if (startIndex < total) {
            pageData = mockList.subList(startIndex, endIndex);
        } else {
            pageData = new ArrayList<>();
        }

        Page<Map<String, Object>> pageResult = new PageImpl<>(pageData, PageRequest.of(pageIndex, safeSize), total);
        return ApiResponse.success(pageResult);
    }
}
