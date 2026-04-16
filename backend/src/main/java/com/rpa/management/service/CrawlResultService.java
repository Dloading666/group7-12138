package com.rpa.management.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.rpa.management.dto.CrawlResultDTO;
import com.rpa.management.entity.CrawlResult;
import com.rpa.management.entity.Task;
import com.rpa.management.repository.CrawlResultRepository;
import com.rpa.management.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlResultService {

    private final CrawlResultRepository crawlResultRepository;
    private final TaskRepository taskRepository;

    @Transactional
    public void saveFromCallback(Map<String, Object> payload) {
        String taskId = valueAsString(payload.get("taskId"));
        if (!StringUtils.hasText(taskId)) {
            throw new IllegalArgumentException("crawl callback taskId is required");
        }

        String status = valueAsString(payload.get("status"));
        String finalUrl = valueAsString(payload.get("finalUrl"));
        String title = valueAsString(payload.get("title"));
        String summaryText = valueAsString(payload.get("summaryText"));
        String rawHtml = valueAsString(payload.get("rawHtml"));
        String errorMessage = valueAsString(payload.get("errorMessage"));
        Integer totalCount = valueAsInteger(payload.get("totalCount"));
        Integer crawledPages = valueAsInteger(payload.get("crawledPages"));
        Object structuredData = payload.get("structuredData");
        if (structuredData == null) {
            structuredData = payload.get("data");
        }

        Task task = taskRepository.findByTaskId(taskId).orElse(null);

        CrawlResult crawlResult = crawlResultRepository.findByTaskId(taskId)
                .orElseGet(CrawlResult::new);
        crawlResult.setTaskId(taskId);
        crawlResult.setTaskRecordId(task != null ? task.getId() : null);
        crawlResult.setTaskName(task != null ? task.getName() : null);
        crawlResult.setStatus(StringUtils.hasText(status) ? status : "failed");
        crawlResult.setFinalUrl(finalUrl);
        crawlResult.setTitle(title);
        crawlResult.setSummaryText(summaryText);
        crawlResult.setRawHtml(rawHtml);
        crawlResult.setStructuredData(structuredData != null ? JSON.toJSONString(structuredData) : null);
        crawlResult.setTotalCount(totalCount != null ? totalCount : 0);
        crawlResult.setCrawledPages(crawledPages != null ? crawledPages : 0);
        crawlResult.setErrorMessage(errorMessage);
        crawlResultRepository.save(crawlResult);

        if (task == null) {
            log.warn("Received crawl callback for unknown taskId={}", taskId);
            return;
        }

        task.setStatus(crawlResult.getStatus());
        task.setProgress("completed".equals(crawlResult.getStatus()) ? 100 : task.getProgress());
        task.setEndTime(LocalDateTime.now());
        if (task.getStartTime() != null) {
            task.setDuration((int) Duration.between(task.getStartTime(), task.getEndTime()).getSeconds());
        }

        if ("completed".equals(crawlResult.getStatus())) {
            task.setErrorMessage(null);
            task.setResult(buildTaskSummary(crawlResult));
        } else {
            task.setErrorMessage(StringUtils.hasText(errorMessage) ? errorMessage : "抓取失败");
            task.setResult("抓取失败");
        }

        taskRepository.save(task);
    }

    public Page<CrawlResultDTO> getResults(String keyword, String taskId, String status,
                                           LocalDateTime startTime, LocalDateTime endTime,
                                           int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, size), Sort.by("createTime").descending());
        return crawlResultRepository.findByConditions(
                emptyToNull(keyword),
                emptyToNull(taskId),
                emptyToNull(status),
                startTime,
                endTime,
                pageable
        ).map(this::toDto);
    }

    public CrawlResultDTO getResultByTaskId(String taskId) {
        CrawlResult crawlResult = crawlResultRepository.findByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("未找到抓取结果: " + taskId));
        return toDto(crawlResult);
    }

    public CrawlResultDTO getResultByTaskRecordId(Long taskRecordId) {
        CrawlResult crawlResult = crawlResultRepository.findByTaskRecordId(taskRecordId)
                .orElseThrow(() -> new RuntimeException("未找到抓取结果: " + taskRecordId));
        return toDto(crawlResult);
    }

    @Transactional
    public void deleteByTaskId(String taskId) {
        crawlResultRepository.deleteByTaskId(taskId);
    }

    public int getTotalCount(String taskId) {
        return crawlResultRepository.findByTaskId(taskId)
                .map(result -> result.getTotalCount() != null ? result.getTotalCount() : 0)
                .orElse(0);
    }

    private CrawlResultDTO toDto(CrawlResult crawlResult) {
        return CrawlResultDTO.builder()
                .id(crawlResult.getId())
                .taskRecordId(crawlResult.getTaskRecordId())
                .taskId(crawlResult.getTaskId())
                .taskName(crawlResult.getTaskName())
                .finalUrl(crawlResult.getFinalUrl())
                .title(crawlResult.getTitle())
                .summaryText(crawlResult.getSummaryText())
                .rawHtml(crawlResult.getRawHtml())
                .structuredData(parseStructuredData(crawlResult.getStructuredData()))
                .totalCount(crawlResult.getTotalCount())
                .crawledPages(crawlResult.getCrawledPages())
                .status(crawlResult.getStatus())
                .errorMessage(crawlResult.getErrorMessage())
                .createTime(crawlResult.getCreateTime())
                .updateTime(crawlResult.getUpdateTime())
                .build();
    }

    private List<Map<String, Object>> parseStructuredData(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        try {
            JSONArray array = JSON.parseArray(raw);
            java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
            for (Object item : array) {
                if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) item;
                    result.add(map);
                }
            }
            return result;
        } catch (Exception ex) {
            log.warn("Failed to parse structured crawl data", ex);
            return Collections.emptyList();
        }
    }

    private String buildTaskSummary(CrawlResult crawlResult) {
        String title = StringUtils.hasText(crawlResult.getTitle()) ? crawlResult.getTitle() : crawlResult.getFinalUrl();
        if (!StringUtils.hasText(title)) {
            title = "真实网站抓取完成";
        }
        return String.format("抓取完成：%s（%d 条，%d 页）",
                title,
                crawlResult.getTotalCount() != null ? crawlResult.getTotalCount() : 0,
                crawlResult.getCrawledPages() != null ? crawlResult.getCrawledPages() : 0);
    }

    private String valueAsString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String str) {
            return str;
        }
        return String.valueOf(value);
    }

    private Integer valueAsInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str && StringUtils.hasText(str)) {
            return Integer.parseInt(str);
        }
        return null;
    }

    private String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
