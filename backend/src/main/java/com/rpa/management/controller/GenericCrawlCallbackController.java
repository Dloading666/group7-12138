package com.rpa.management.controller;

import com.rpa.management.service.CrawlResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/crawl")
public class GenericCrawlCallbackController {

    private final CrawlResultService crawlResultService;

    @PostMapping("/callback")
    @Transactional
    public void handleCrawlCallback(@RequestBody Map<String, Object> payload) {
        log.info("Received generic crawl callback for taskId={}", payload.get("taskId"));
        crawlResultService.saveFromCallback(payload);
    }
}
