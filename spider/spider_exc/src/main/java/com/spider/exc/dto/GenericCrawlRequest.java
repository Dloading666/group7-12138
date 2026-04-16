package com.spider.exc.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class GenericCrawlRequest {

    private String taskId;

    private String url;

    private Map<String, String> headers;

    private List<CookieRule> cookies;

    private List<ExtractionRule> extractionRules;

    private PaginationRule pagination;

    private String callbackUrl;

    private Integer timeout;

    @Data
    public static class CookieRule {
        private String name;
        private String value;
        private String url;
        private String domain;
        private String path;
        private Double expires;
        private Boolean httpOnly;
        private Boolean secure;
    }

    @Data
    public static class ExtractionRule {
        private String field;
        private String selector;
        private String type = "text";
        private String attr;
    }

    @Data
    public static class PaginationRule {
        private String type = "next";
        private String selector;
        private Integer maxPages = 1;
        private Integer waitTime = 2000;
    }
}
