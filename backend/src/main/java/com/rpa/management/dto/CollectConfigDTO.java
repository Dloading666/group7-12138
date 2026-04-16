package com.rpa.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 采集配置DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectConfigDTO {
    
    private Long id;
    
    private String name;
    
    private Long taskId;
    
    private Long robotId;
    
    private String collectType;
    
    private String targetUrl;
    
    private String requestMethod;
    
    private String requestHeaders;
    
    private String requestParams;
    
    private String requestBody;
    
    private String collectRules;
    
    private String fieldMapping;
    
    private String dataCleanRules;
    
    private String pageConfig;
    
    private String cronExpression;
    
    private Boolean isEnabled;
    
    private Integer timeout;
    
    private Integer retryCount;
    
    private String proxyConfig;

    private String outputConfig;

    /**
     * 税务爬虫专用配置（JSON格式）
     * 当 collectType = 'spider-tax' 时使用
     */
    private String spiderConfig;
    
    private LocalDateTime lastExecuteTime;
    
    private String lastExecuteStatus;
    
    private Long totalCount;
    
    private Long successCount;
    
    private Long failCount;
    
    private Long createBy;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
