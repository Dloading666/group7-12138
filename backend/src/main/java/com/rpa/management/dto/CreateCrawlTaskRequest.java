package com.rpa.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class CreateCrawlTaskRequest {

    @NotBlank(message = "任务名称不能为空")
    private String name;

    @NotNull(message = "请选择数据采集机器人")
    private Long robotId;

    private String executeType = "immediate";

    private LocalDateTime scheduledTime;

    @NotBlank(message = "请输入目标 URL")
    private String url;

    private Map<String, String> headers;

    private List<CookieDTO> cookies;

    private PaginationDTO pagination;

    private List<ExtractionRuleDTO> extractionRules;

    private Integer timeout = 30000;

    private String description;

    @Data
    public static class CookieDTO {
        private String name;
        private String value;
        private String domain;
        private String path;
        private Double expires;
        private Boolean httpOnly;
        private Boolean secure;
    }

    @Data
    public static class PaginationDTO {
        private String type = "next";
        private String selector;
        private Integer maxPages = 1;
        private Integer waitTime = 2000;
    }

    @Data
    public static class ExtractionRuleDTO {
        private String field;
        private String selector;
        private String type = "text";
        private String attr;
    }
}
