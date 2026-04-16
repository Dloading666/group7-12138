package com.spider.exc.controller;

import com.spider.exc.dto.GenericCrawlRequest;
import com.spider.exc.dto.GenericCrawlResponse;
import com.spider.exc.service.GenericCrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/crawl")
public class GenericCrawlerController {

    private final GenericCrawlerService genericCrawlerService;

    @PostMapping
    public Map<String, Object> crawl(@RequestBody GenericCrawlRequest request) {
        log.info("Received generic crawl request taskId={}, url={}", request.getTaskId(), request.getUrl());
        if (request.getTaskId() != null && request.getCallbackUrl() != null && !request.getCallbackUrl().isBlank()) {
            new Thread(() -> genericCrawlerService.crawl(request), "generic-crawl-" + request.getTaskId()).start();
            Map<String, Object> response = new HashMap<>();
            response.put("taskId", request.getTaskId());
            response.put("status", "pending");
            response.put("message", "crawl task submitted");
            return response;
        }

        GenericCrawlResponse result = genericCrawlerService.crawl(request);
        return toResponse(result);
    }

    @PostMapping("/simple")
    public Map<String, Object> simpleCrawl(@RequestBody Map<String, Object> request) {
        GenericCrawlRequest crawlRequest = new GenericCrawlRequest();
        crawlRequest.setTaskId((String) request.getOrDefault("taskId", "T" + System.currentTimeMillis()));
        crawlRequest.setUrl((String) request.get("url"));
        crawlRequest.setCallbackUrl((String) request.get("callbackUrl"));
        GenericCrawlResponse result = genericCrawlerService.crawl(crawlRequest);
        return toResponse(result);
    }

    private Map<String, Object> toResponse(GenericCrawlResponse result) {
        Map<String, Object> response = new HashMap<>();
        response.put("taskId", result.getTaskId());
        response.put("status", result.getStatus());
        response.put("finalUrl", result.getFinalUrl());
        response.put("title", result.getTitle());
        response.put("summaryText", result.getSummaryText());
        response.put("rawHtml", result.getRawHtml());
        response.put("structuredData", result.getStructuredData());
        response.put("totalCount", result.getTotalCount());
        response.put("crawledPages", result.getCrawledPages());
        if (result.getErrorMessage() != null) {
            response.put("errorMessage", result.getErrorMessage());
        }
        return response;
    }
}
