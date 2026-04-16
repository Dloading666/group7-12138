package com.rpa.management.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.rpa.management.dto.CollectConfigDTO;
import com.rpa.management.entity.CollectConfig;
import com.rpa.management.entity.Task;
import com.rpa.management.repository.CollectConfigRepository;
import com.rpa.management.repository.CollectDataRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tax-only collection config service.
 * Generic website crawling has been moved to TaskService + RobotExecutor + spider_exc.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectConfigService {

    private static final String TAX_COLLECT_TYPE = "spider-tax";

    private final CollectConfigRepository collectConfigRepository;
    private final CollectDataRepository collectDataRepository;
    private final ExecutionLogService executionLogService;
    private final TaskRepository taskRepository;
    private final com.rpa.management.client.SpiderApiClient spiderApiClient;

    @Transactional
    public CollectConfigDTO createConfig(CollectConfigDTO dto) {
        validateTaxOnlyConfig(dto.getCollectType(), dto.getSpiderConfig());

        CollectConfig config = new CollectConfig();
        applyDto(config, dto);
        config = collectConfigRepository.save(config);

        log.info("Created tax spider config id={}, name={}", config.getId(), config.getName());
        return toDTO(config);
    }

    @Transactional
    public CollectConfigDTO updateConfig(Long id, CollectConfigDTO dto) {
        validateTaxOnlyConfig(dto.getCollectType(), dto.getSpiderConfig());

        CollectConfig config = collectConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("采集配置不存在: " + id));
        applyDto(config, dto);
        config = collectConfigRepository.save(config);

        log.info("Updated tax spider config id={}, name={}", config.getId(), config.getName());
        return toDTO(config);
    }

    @Transactional
    public void deleteConfig(Long id) {
        collectDataRepository.deleteByConfigId(id);
        collectConfigRepository.deleteById(id);
        log.info("Deleted tax spider config id={}", id);
    }

    public CollectConfigDTO getConfigById(Long id) {
        CollectConfig config = collectConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("采集配置不存在: " + id));
        ensureTaxOnly(config);
        return toDTO(config);
    }

    public Page<CollectConfigDTO> getConfigsByPage(String name, String collectType,
                                                   Boolean isEnabled, int page, int size) {
        String effectiveType = normalizeCollectType(collectType);
        Pageable pageable = PageRequest.of(
                Math.max(0, page - 1),
                Math.max(1, size),
                Sort.by("createTime").descending()
        );
        return collectConfigRepository.findByConditions(name, effectiveType, isEnabled, pageable)
                .map(this::toDTO);
    }

    public List<CollectConfigDTO> getEnabledConfigs() {
        return collectConfigRepository.findByIsEnabledTrue().stream()
                .filter(config -> TAX_COLLECT_TYPE.equals(config.getCollectType()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void executeCollect(Long configId, Long taskId, String taskCode, String taskName,
                               Long robotId, String robotName) {
        CollectConfig config = collectConfigRepository.findById(configId)
                .orElseThrow(() -> new RuntimeException("采集配置不存在: " + configId));
        ensureTaxOnly(config);

        config.setTotalCount(defaultLong(config.getTotalCount()) + 1);
        config.setLastExecuteTime(LocalDateTime.now());

        try {
            executionLogService.info(taskId, taskCode, taskName, robotId, robotName,
                    "开始提交税务专用采集任务: " + config.getName());

            executeSpiderTaxCollect(config, taskName, robotId, robotName);

            config.setSuccessCount(defaultLong(config.getSuccessCount()) + 1);
            config.setLastExecuteStatus("success");
            executionLogService.info(taskId, taskCode, taskName, robotId, robotName,
                    "税务专用采集任务已提交，等待 spider_exc 回调结果");
        } catch (Exception ex) {
            config.setFailCount(defaultLong(config.getFailCount()) + 1);
            config.setLastExecuteStatus("failed");
            executionLogService.error(taskId, taskCode, taskName, robotId, robotName,
                    "税务专用采集任务执行失败: " + ex.getMessage());
            throw ex;
        } finally {
            collectConfigRepository.save(config);
        }
    }

    private void executeSpiderTaxCollect(CollectConfig config, String taskName,
                                         Long robotId, String robotName) {
        JSONObject spiderConfig = parseSpiderConfig(config.getSpiderConfig());
        String taxNo = spiderConfig.getString("taxNo");
        String uscCode = spiderConfig.getString("uscCode");
        String appDate = spiderConfig.getString("appDate");

        Task task = new Task();
        task.setTaskId(generateTaskId());
        task.setName(StringUtils.hasText(taskName) ? taskName : "税务专用采集-" + taxNo);
        task.setType("data-collection");
        task.setStatus("pending");
        task.setRobotId(robotId);
        task.setRobotName(robotName);
        task.setPriority("medium");
        task.setExecuteType("immediate");
        task.setTaxId(taxNo);
        task.setDescription("税务专用采集任务");
        taskRepository.save(task);

        spiderApiClient.submitSpiderTask(task.getTaskId(), taxNo, uscCode, appDate);
        log.info("Submitted tax spider task taskId={}, taxNo={}", task.getTaskId(), taxNo);
    }

    private void applyDto(CollectConfig config, CollectConfigDTO dto) {
        config.setName(dto.getName());
        config.setTaskId(dto.getTaskId());
        config.setRobotId(dto.getRobotId());
        config.setCollectType(TAX_COLLECT_TYPE);
        config.setTargetUrl(dto.getTargetUrl());
        config.setRequestMethod(dto.getRequestMethod());
        config.setRequestHeaders(dto.getRequestHeaders());
        config.setRequestParams(dto.getRequestParams());
        config.setRequestBody(dto.getRequestBody());
        config.setCollectRules(dto.getCollectRules());
        config.setFieldMapping(dto.getFieldMapping());
        config.setDataCleanRules(dto.getDataCleanRules());
        config.setPageConfig(dto.getPageConfig());
        config.setCronExpression(dto.getCronExpression());
        config.setIsEnabled(dto.getIsEnabled() != null ? dto.getIsEnabled() : Boolean.TRUE);
        config.setTimeout(dto.getTimeout() != null ? dto.getTimeout() : 30000);
        config.setRetryCount(dto.getRetryCount() != null ? dto.getRetryCount() : 3);
        config.setProxyConfig(dto.getProxyConfig());
        config.setOutputConfig(dto.getOutputConfig());
        config.setSpiderConfig(dto.getSpiderConfig());
        config.setCreateBy(dto.getCreateBy());
    }

    private void validateTaxOnlyConfig(String collectType, String spiderConfig) {
        String effectiveType = normalizeCollectType(collectType);
        if (!TAX_COLLECT_TYPE.equals(effectiveType)) {
            throw new RuntimeException("旧版模拟采集链路已下线，通用网站采集请改用 /crawl/task，CollectConfig 仅保留 spider-tax");
        }
        parseSpiderConfig(spiderConfig);
    }

    private String normalizeCollectType(String collectType) {
        if (!StringUtils.hasText(collectType)) {
            return TAX_COLLECT_TYPE;
        }
        if (!TAX_COLLECT_TYPE.equals(collectType)) {
            throw new RuntimeException("旧版模拟采集链路已下线，CollectConfig 仅支持 spider-tax");
        }
        return collectType;
    }

    private JSONObject parseSpiderConfig(String spiderConfig) {
        if (!StringUtils.hasText(spiderConfig)) {
            throw new RuntimeException("税务专用采集缺少 spiderConfig");
        }
        try {
            JSONObject config = JSON.parseObject(spiderConfig);
            if (!StringUtils.hasText(config.getString("taxNo"))) {
                throw new RuntimeException("税务专用采集缺少 taxNo");
            }
            return config;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("spiderConfig 必须是合法 JSON");
        }
    }

    private void ensureTaxOnly(CollectConfig config) {
        if (!TAX_COLLECT_TYPE.equals(config.getCollectType())) {
            throw new RuntimeException("旧版模拟采集配置已下线，当前配置不再支持执行");
        }
    }

    private long defaultLong(Long value) {
        return value != null ? value : 0L;
    }

    private String generateTaskId() {
        return "T" + System.currentTimeMillis() + String.format("%04d", (int) (Math.random() * 10000));
    }

    private CollectConfigDTO toDTO(CollectConfig config) {
        return CollectConfigDTO.builder()
                .id(config.getId())
                .name(config.getName())
                .taskId(config.getTaskId())
                .robotId(config.getRobotId())
                .collectType(config.getCollectType())
                .targetUrl(config.getTargetUrl())
                .requestMethod(config.getRequestMethod())
                .requestHeaders(config.getRequestHeaders())
                .requestParams(config.getRequestParams())
                .requestBody(config.getRequestBody())
                .collectRules(config.getCollectRules())
                .fieldMapping(config.getFieldMapping())
                .dataCleanRules(config.getDataCleanRules())
                .pageConfig(config.getPageConfig())
                .cronExpression(config.getCronExpression())
                .isEnabled(config.getIsEnabled())
                .timeout(config.getTimeout())
                .retryCount(config.getRetryCount())
                .proxyConfig(config.getProxyConfig())
                .outputConfig(config.getOutputConfig())
                .spiderConfig(config.getSpiderConfig())
                .lastExecuteTime(config.getLastExecuteTime())
                .lastExecuteStatus(config.getLastExecuteStatus())
                .totalCount(config.getTotalCount())
                .successCount(config.getSuccessCount())
                .failCount(config.getFailCount())
                .createBy(config.getCreateBy())
                .createTime(config.getCreateTime())
                .updateTime(config.getUpdateTime())
                .build();
    }
}
