package com.rpa.management.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 数据采集配置实体
 */
@Data
@Entity
@Table(name = "sys_collect_config")
public class CollectConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 配置名称
     */
    @Column(nullable = false, length = 100)
    private String name;
    
    /**
     * 关联的任务ID
     */
    @Column(name = "task_id")
    private Long taskId;
    
    /**
     * 关联的机器人ID
     */
    @Column(name = "robot_id")
    private Long robotId;
    
    /**
     * 采集类型：web-网页采集, api-API接口, database-数据库
     */
    @Column(name = "collect_type", nullable = false, length = 20)
    private String collectType = "web";
    
    /**
     * 目标URL（网页采集）
     */
    @Column(name = "target_url", length = 500)
    private String targetUrl;
    
    /**
     * 请求方法：GET, POST
     */
    @Column(name = "request_method", length = 10)
    private String requestMethod = "GET";
    
    /**
     * 请求头（JSON格式）
     */
    @Lob
    @Column(name = "request_headers")
    private String requestHeaders;
    
    /**
     * 请求参数（JSON格式）
     */
    @Lob
    @Column(name = "request_params")
    private String requestParams;
    
    /**
     * 请求体（POST请求）
     */
    @Lob
    @Column(name = "request_body")
    private String requestBody;
    
    /**
     * 采集规则（JSON格式，包含选择器配置）
     * 格式示例：
     * {
     *   "listSelector": ".item-list .item",
     *   "fields": [
     *     {"name": "title", "selector": "h3.title", "type": "text"},
     *     {"name": "price", "selector": ".price", "type": "text"},
     *     {"name": "image", "selector": "img", "type": "attr", "attr": "src"}
     *   ]
     * }
     */
    @Lob
    @Column(name = "collect_rules")
    private String collectRules;
    
    /**
     * 字段映射配置（JSON格式）
     */
    @Lob
    @Column(name = "field_mapping")
    private String fieldMapping;
    
    /**
     * 数据清洗规则（JSON格式）
     */
    @Lob
    @Column(name = "data_clean_rules")
    private String dataCleanRules;
    
    /**
     * 分页配置（JSON格式）
     * {
     *   "type": "url_param",  // url_param, click, scroll
     *   "paramName": "page",
     *   "startPage": 1,
     *   "endPage": 10,
     *   "pageSize": 20
     * }
     */
    @Lob
    @Column(name = "page_config")
    private String pageConfig;
    
    /**
     * 定时表达式（Cron）
     */
    @Column(name = "cron_expression", length = 100)
    private String cronExpression;
    
    /**
     * 是否启用
     */
    @Column(name = "is_enabled")
    private Boolean isEnabled = true;
    
    /**
     * 超时时间（毫秒）
     */
    @Column(name = "timeout")
    private Integer timeout = 30000;
    
    /**
     * 重试次数
     */
    @Column(name = "retry_count")
    private Integer retryCount = 3;
    
    /**
     * 代理配置（JSON格式）
     */
    @Lob
    @Column(name = "proxy_config")
    private String proxyConfig;
    
    /**
     * 输出配置（JSON格式）
     */
    @Lob
    @Column(name = "output_config")
    private String outputConfig;

    /**
     * 税务爬虫专用配置（JSON格式）
     * 当 collectType = 'spider-tax' 时使用
     * 格式: { "taxNo": "", "uscCode": "", "appDate": "" }
     */
    @Lob
    @Column(name = "spider_config")
    private String spiderConfig;
    
    /**
     * 最后执行时间
     */
    @Column(name = "last_execute_time")
    private LocalDateTime lastExecuteTime;
    
    /**
     * 最后执行状态
     */
    @Column(name = "last_execute_status", length = 20)
    private String lastExecuteStatus;
    
    /**
     * 总采集次数
     */
    @Column(name = "total_count")
    private Long totalCount = 0L;
    
    /**
     * 成功次数
     */
    @Column(name = "success_count")
    private Long successCount = 0L;
    
    /**
     * 失败次数
     */
    @Column(name = "fail_count")
    private Long failCount = 0L;
    
    /**
     * 创建者ID
     */
    @Column(name = "create_by")
    private Long createBy;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @UpdateTimestamp
    private LocalDateTime updateTime;
}
