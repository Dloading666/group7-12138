package com.rpa.management.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpiderApiClient {

    @Value("${external.spider.base-url:http://localhost:8081}")
    private String spiderBaseUrl;

    @Value("${external.spider.callback-url:http://localhost:8080/api/spider/task/callback}")
    private String callbackUrl;

    private final RestTemplate restTemplate;

    public Long submitSpiderTask(String taskId, String taxNo, String uscCode, String appDate) {
        String url = spiderBaseUrl + "/api/spider/task/submit";

        Map<String, Object> request = new HashMap<>();
        request.put("taskId", taskId);
        request.put("taxNo", taxNo);
        request.put("uscCode", uscCode);
        request.put("appDate", appDate);
        request.put("callbackUrl", callbackUrl);

        ResponseEntity<String> response = restTemplate.postForEntity(url, buildJsonEntity(request), String.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JSONObject body = JSON.parseObject(response.getBody());
            return body.getLong("data");
        }
        throw new RuntimeException("提交税务爬虫任务失败: " + response.getStatusCode());
    }

    public void submitGenericCrawlTask(String taskId, JSONObject params) {
        Map<String, Object> request = new HashMap<>();
        request.put("taskId", taskId);
        request.put("url", params.getString("url"));
        request.put("timeout", params.getInteger("timeout"));
        request.put("headers", params.getJSONObject("headers"));
        request.put("cookies", params.getJSONArray("cookies"));
        request.put("extractionRules", params.getJSONArray("extractionRules"));
        request.put("pagination", params.getJSONObject("pagination"));
        request.put("callbackUrl", callbackUrl.replace("/spider/task/callback", "/crawl/callback"));

        ResponseEntity<String> response = restTemplate.postForEntity(spiderBaseUrl + "/api/crawl", buildJsonEntity(request), String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("提交真实网站采集任务失败: " + response.getStatusCode());
        }
    }

    public String getTaskStatus(String taskId) {
        String url = spiderBaseUrl + "/api/spider/task/" + taskId + "/status";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JSONObject body = JSON.parseObject(response.getBody());
            return body.getString("data");
        }
        return "unknown";
    }

    public JSONObject getTaskResult(String taskId) {
        String url = spiderBaseUrl + "/api/spider/task/" + taskId + "/result";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return JSON.parseObject(response.getBody());
        }
        return null;
    }

    public JSONObject getSpiderResultFromExc(String taskId) {
        String url = spiderBaseUrl + "/api/spider/task/" + taskId + "/result";
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return JSON.parseObject(response.getBody());
            }
        } catch (Exception ex) {
            log.error("获取 spider 结果失败: taskId={}", taskId, ex);
        }
        return null;
    }

    public JSONObject getSpiderResultsList() {
        String url = spiderBaseUrl + "/api/spider/task/results";
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return JSON.parseObject(response.getBody());
            }
        } catch (Exception ex) {
            log.error("获取 spider 任务结果列表失败", ex);
        }
        return null;
    }

    private HttpEntity<Map<String, Object>> buildJsonEntity(Map<String, Object> request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(request, headers);
    }
}
