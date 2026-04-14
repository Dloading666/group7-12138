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
    
    private LocalDateTime lastExecuteTime;
    
    private String lastExecuteStatus;
    
    private Long totalCount;
    
    private Long successCount;
    
    private Long failCount;
    
    private Long createBy;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
