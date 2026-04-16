package com.spider.exc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spider.exc.domain.entity.IndicatorQuery;
import com.spider.exc.domain.entity.SpiderTask;
import com.spider.exc.domain.mapper.IndicatorQueryMapper;
import com.spider.exc.domain.mapper.SpiderTaskMapper;
import com.spider.exc.dto.SpiderTaskRequest;
import com.spider.exc.dto.SpiderTaskResultDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 外部任务调度服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpiderTaskService {

    private final SpiderTaskMapper spiderTaskMapper;
    private final IndicatorQueryMapper indicatorQueryMapper;
    private final SpiderTaxExecutor spiderTaxExecutor;

    /**
     * 接收 project-gl 提交的任务，触发异步执行
     */
    public Long submitTask(SpiderTaskRequest request) {
        SpiderTask task = new SpiderTask();
        task.setTaskId(request.getTaskId());
        task.setTaxNo(request.getTaxNo());
        task.setUscCode(request.getUscCode());
        task.setAppDate(request.getAppDate() != null ? LocalDate.parse(request.getAppDate()) : null);
        task.setCallbackUrl(request.getCallbackUrl());
        task.setStatus("pending");

        spiderTaskMapper.insert(task);
        log.info("收到 spider 任务: taskId={}, taxNo={}", request.getTaskId(), request.getTaxNo());

        // 异步执行四步流程
        executeTaskAsync(task.getId());

        return task.getId();
    }

    /**
     * 异步执行四步流程
     */
    @Async
    public void executeTaskAsync(Long spiderTaskId) {
        SpiderTask task = spiderTaskMapper.selectById(spiderTaskId);
        if (task == null) {
            log.error("任务不存在: {}", spiderTaskId);
            return;
        }

        try {
            task.setStatus("running");
            spiderTaskMapper.updateById(task);

            // 调用四步执行器
            spiderTaxExecutor.execute(task);

            task.setStatus("completed");
            spiderTaskMapper.updateById(task);

            // 回调 project-gl
            callbackProjectGl(task);

        } catch (Exception e) {
            log.error("任务执行失败: {}", e.getMessage(), e);
            task.setStatus("failed");
            task.setErrorMessage(e.getMessage());
            spiderTaskMapper.updateById(task);
        }
    }

    /**
     * 查询任务状态
     */
    public String getTaskStatus(String taskId) {
        SpiderTask task = spiderTaskMapper.selectOne(
                new LambdaQueryWrapper<SpiderTask>().eq(SpiderTask::getTaskId, taskId)
        );
        return task != null ? task.getStatus() : "not_found";
    }

    /**
     * 查询任务结果（IndicatorQuery）
     */
    public IndicatorQuery getTaskResult(String taskId) {
        SpiderTask task = spiderTaskMapper.selectOne(
                new LambdaQueryWrapper<SpiderTask>().eq(SpiderTask::getTaskId, taskId)
        );
        if (task == null || task.getQueryId() == null) {
            return null;
        }
        return indicatorQueryMapper.selectById(task.getQueryId());
    }

    /**
     * 查询所有任务结果列表
     */
    public java.util.List<SpiderTaskResultDTO> getTaskList() {
        java.util.List<SpiderTask> tasks = spiderTaskMapper.selectList(null);
        return tasks.stream().map(task -> {
            SpiderTaskResultDTO dto = new SpiderTaskResultDTO();
            dto.setId(task.getId());
            dto.setTaskId(task.getTaskId());
            dto.setTaxNo(task.getTaxNo());
            dto.setUscCode(task.getUscCode());
            dto.setAppDate(task.getAppDate());
            dto.setStatus(task.getStatus());
            dto.setErrorMessage(task.getErrorMessage());
            dto.setCreateTime(task.getCreateTime());
            dto.setUpdateTime(task.getUpdateTime());
            dto.setCollectionId(task.getCollectionId());
            dto.setParsingId(task.getParsingId());
            dto.setProcessingId(task.getProcessingId());
            dto.setQueryId(task.getQueryId());

            if (task.getQueryId() != null) {
                IndicatorQuery query = indicatorQueryMapper.selectById(task.getQueryId());
                if (query != null) {
                    dto.setBusinessJson(query.getBusinessJson());
                }
            }
            return dto;
        }).collect(java.util.stream.Collectors.toList());
    }

    /**
     * 回调 project-gl
     */
    private void callbackProjectGl(SpiderTask task) {
        if (task.getCallbackUrl() == null || task.getCallbackUrl().isEmpty()) {
            log.info("无回调地址，跳过回调: taskId={}", task.getTaskId());
            return;
        }

        try {
            org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("taskId", task.getTaskId());
            body.put("status", task.getStatus());
            body.put("errorMessage", task.getErrorMessage());
            body.put("queryId", task.getQueryId());
            body.put("collectionId", task.getCollectionId());
            body.put("parsingId", task.getParsingId());
            body.put("processingId", task.getProcessingId());

            // 从 businessJson 中提取企业名称
            if (task.getQueryId() != null) {
                com.spider.exc.domain.entity.IndicatorQuery query = indicatorQueryMapper.selectById(task.getQueryId());
                if (query != null && query.getBusinessJson() != null) {
                    com.alibaba.fastjson2.JSONObject bizJson = com.alibaba.fastjson2.JSON.parseObject(query.getBusinessJson());
                    if (bizJson != null) {
                        com.alibaba.fastjson2.JSONObject collectedData = bizJson.getJSONObject("collectedData");
                        if (collectedData != null) {
                            String enterpriseName = collectedData.getString("enterpriseName");
                            if (enterpriseName != null) {
                                body.put("enterpriseName", enterpriseName);
                            }
                        }
                    }
                }
            }

            org.springframework.http.HttpEntity<java.util.Map<String, Object>> entity =
                    new org.springframework.http.HttpEntity<>(body, headers);

            rt.postForEntity(task.getCallbackUrl(), entity, String.class);
            log.info("回调成功: taskId={}, url={}", task.getTaskId(), task.getCallbackUrl());
        } catch (Exception e) {
            log.warn("回调失败（不阻塞任务）: taskId={}, error={}", task.getTaskId(), e.getMessage());
        }
    }
}
