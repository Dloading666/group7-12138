package com.spider.exc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenericCrawlResponse {

    private String taskId;

    private String status;

    private String errorMessage;

    private String finalUrl;

    private String title;

    private String summaryText;

    private String rawHtml;

    private List<Map<String, Object>> structuredData;

    private Integer totalPages;

    private Integer crawledPages;

    private Integer totalCount;
}
