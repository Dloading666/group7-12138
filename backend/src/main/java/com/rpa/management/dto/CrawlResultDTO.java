package com.rpa.management.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class CrawlResultDTO {

    private Long id;

    private Long taskRecordId;

    private Long taskRunId;

    private String taskId;

    private String taskName;

    private String finalUrl;

    private String title;

    private String summaryText;

    private String rawHtml;

    private List<Map<String, Object>> structuredData;

    private Integer totalCount;

    private Integer crawledPages;

    private String status;

    private String errorMessage;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
