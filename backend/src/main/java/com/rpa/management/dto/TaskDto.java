package com.rpa.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {

    private Long id;

    /**
     * 任务编号
     */
    private String taskId;

    /**
     * 任务名称
     */
    private String name;

    /**
     * 任务类型
     */
    private String type;

    /**
     * 状态
     */
    private String status;

    /**
     * 进度
     */
    private Integer progress;

    /**
     * 机器人ID
     */
    private Long robotId;

    /**
     * 机器人名称
     */
    private String robotName;

    /**
     * 优先级
     */
    private String priority;

    /**
     * 执行类型
     */
    private String executeType;

    /**
     * 计划执行时间
     */
    private LocalDateTime scheduledTime;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 执行时长（秒）
     */
    private Integer duration;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 描述
     */
    private String description;

    /**
     * 执行结果
     */
    private String result;

    /**
     * 任务参数(JSON)
     */
    private String params;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 税号
     */
    private String taxId;

    /**
     * 企业名称
     */
    private String enterpriseName;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 采集URL
     */
    private String crawlUrl;

    /**
     * 采集超时时间
     */
    private Integer crawlTimeout;

    /**
     * 是否有请求头
     */
    private Boolean hasHeaders;

    /**
     * 是否有Cookies
     */
    private Boolean hasCookies;

    /**
     * 是否有分页
     */
    private Boolean hasPagination;

    /**
     * 提取规则数量
     */
    private Integer extractionRuleCount;

    /**
     * AI 分析来源任务主表 ID
     */
    private Long sourceTaskRecordId;

    /**
     * AI 分析来源任务编号
     */
    private String sourceTaskId;

    /**
     * AI 分析来源任务名称
     */
    private String sourceTaskName;

    /**
     * AI 分析来源标题
     */
    private String sourceTitle;

    /**
     * AI 分析来源 URL
     */
    private String sourceFinalUrl;

    /**
     * AI 分析流程 ID
     */
    private Long workflowId;

    /**
     * AI 分析流程名称
     */
    private String workflowName;

    /**
     * AI 分析问题
     */
    private String query;
}
