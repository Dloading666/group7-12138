package com.spider.exc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * spider_exc 返回给 project-gl 的任务结果DTO（包含 SpiderTask + IndicatorQuery）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpiderTaskResultDTO {

    // SpiderTask 字段
    private Long id;
    private String taskId;          // project-gl 侧任务ID
    private String taxNo;
    private String uscCode;
    private LocalDate appDate;
    private String status;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // IndicatorQuery 字段（业务JSON）
    private String businessJson;    // 完整业务JSON字符串
    private Long collectionId;
    private Long parsingId;
    private Long processingId;
    private Long queryId;
}
