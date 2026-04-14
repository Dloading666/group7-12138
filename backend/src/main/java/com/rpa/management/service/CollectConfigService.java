package com.rpa.management.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.rpa.management.dto.CollectConfigDTO;
import com.rpa.management.entity.CollectConfig;
import com.rpa.management.entity.CollectData;
import com.rpa.management.repository.CollectConfigRepository;
import com.rpa.management.repository.CollectDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 采集配置服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectConfigService {
    
    private final CollectConfigRepository collectConfigRepository;
    private final CollectDataRepository collectDataRepository;
    private final ExecutionLogService executionLogService;
    
    /**
     * 创建采集配置
     */
    @Transactional
    public CollectConfigDTO createConfig(CollectConfigDTO dto) {
        CollectConfig config = new CollectConfig();
        config.setName(dto.getName());
        config.setTaskId(dto.getTaskId());
        config.setRobotId(dto.getRobotId());
        config.setCollectType(dto.getCollectType());
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
        config.setIsEnabled(dto.getIsEnabled() != null ? dto.getIsEnabled() : true);
        config.setTimeout(dto.getTimeout() != null ? dto.getTimeout() : 30000);
        config.setRetryCount(dto.getRetryCount() != null ? dto.getRetryCount() : 3);
        config.setProxyConfig(dto.getProxyConfig());
        config.setOutputConfig(dto.getOutputConfig());
        
        config = collectConfigRepository.save(config);
        log.info("创建采集配置成功: {}", config.getName());
        
        return toDTO(config);
    }
    
    /**
     * 更新采集配置
     */
    @Transactional
    public CollectConfigDTO updateConfig(Long id, CollectConfigDTO dto) {
        CollectConfig config = collectConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("采集配置不存在: " + id));
        
        config.setName(dto.getName());
        config.setTaskId(dto.getTaskId());
        config.setRobotId(dto.getRobotId());
        config.setCollectType(dto.getCollectType());
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
        config.setIsEnabled(dto.getIsEnabled());
        config.setTimeout(dto.getTimeout());
        config.setRetryCount(dto.getRetryCount());
        config.setProxyConfig(dto.getProxyConfig());
        config.setOutputConfig(dto.getOutputConfig());
        
        config = collectConfigRepository.save(config);
        log.info("更新采集配置成功: {}", config.getName());
        
        return toDTO(config);
    }
    
    /**
     * 删除采集配置
     */
    @Transactional
    public void deleteConfig(Long id) {
        // 删除关联的采集数据
        collectDataRepository.deleteByConfigId(id);
        collectConfigRepository.deleteById(id);
        log.info("删除采集配置成功: {}", id);
    }
    
    /**
     * 获取采集配置
     */
    public CollectConfigDTO getConfigById(Long id) {
        CollectConfig config = collectConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("采集配置不存在: " + id));
        return toDTO(config);
    }
    
    /**
     * 分页查询配置
     */
    public Page<CollectConfigDTO> getConfigsByPage(String name, String collectType, 
                                                     Boolean isEnabled, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createTime").descending());
        Page<CollectConfig> configPage = collectConfigRepository.findByConditions(
                name, collectType, isEnabled, pageable
        );
        return configPage.map(this::toDTO);
    }
    
    /**
     * 获取所有启用的配置
     */
    public List<CollectConfigDTO> getEnabledConfigs() {
        return collectConfigRepository.findByIsEnabledTrue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 执行采集任务
     */
    @Transactional
    public void executeCollect(Long configId, Long taskId, String taskCode, String taskName, 
                                Long robotId, String robotName) {
        CollectConfig config = collectConfigRepository.findById(configId)
                .orElseThrow(() -> new RuntimeException("采集配置不存在: " + configId));
        
        config.setTotalCount(config.getTotalCount() + 1);
        config.setLastExecuteTime(LocalDateTime.now());
        
        try {
            // 记录日志
            executionLogService.info(taskId, taskCode, taskName, robotId, robotName, 
                    "开始执行采集任务: " + config.getName());
            
            // 根据采集类型执行不同的采集逻辑
            int collectedCount = 0;
            switch (config.getCollectType()) {
                case "web":
                    collectedCount = executeWebCollect(config, taskId, robotId);
                    break;
                case "api":
                    collectedCount = executeApiCollect(config, taskId, robotId);
                    break;
                case "database":
                    collectedCount = executeDatabaseCollect(config, taskId, robotId);
                    break;
                default:
                    throw new RuntimeException("不支持的采集类型: " + config.getCollectType());
            }
            
            config.setSuccessCount(config.getSuccessCount() + 1);
            config.setLastExecuteStatus("success");
            
            executionLogService.info(taskId, taskCode, taskName, robotId, robotName, 
                    "采集任务执行成功，共采集 " + collectedCount + " 条数据");
            
        } catch (Exception e) {
            config.setFailCount(config.getFailCount() + 1);
            config.setLastExecuteStatus("failed");
            
            executionLogService.error(taskId, taskCode, taskName, robotId, robotName, 
                    "采集任务执行失败: " + e.getMessage());
            
            throw e;
        } finally {
            collectConfigRepository.save(config);
        }
    }
    
    /**
     * 执行网页采集
     */
    private int executeWebCollect(CollectConfig config, Long taskId, Long robotId) {
        log.info("执行网页采集: {}", config.getTargetUrl());
        
        // 这里简化实现，实际应该使用HttpClient或Jsoup等工具进行网页采集
        // 模拟采集过程
        String targetUrl = config.getTargetUrl();
        String collectRules = config.getCollectRules();
        
        // TODO: 实际的网页采集逻辑
        // 1. 发送HTTP请求获取HTML
        // 2. 解析HTML提取数据
        // 3. 数据清洗和去重
        // 4. 保存到数据库
        
        // 模拟保存一条采集数据
        CollectData data = new CollectData();
        data.setConfigId(config.getId());
        data.setTaskId(taskId);
        data.setRobotId(robotId);
        data.setSourceUrl(targetUrl);
        
        // 生成模拟数据
        JSONObject mockData = new JSONObject();
        mockData.put("title", "示例标题");
        mockData.put("content", "示例内容");
        mockData.put("url", targetUrl);
        data.setDataContent(mockData.toJSONString());
        
        // 生成数据hash用于去重
        data.setDataHash(generateHash(mockData.toJSONString()));
        
        // 检查是否重复
        if (!collectDataRepository.existsByDataHash(data.getDataHash())) {
            collectDataRepository.save(data);
        }
        
        return 1;
    }
    
    /**
     * 执行API采集
     */
    private int executeApiCollect(CollectConfig config, Long taskId, Long robotId) {
        log.info("执行API采集: {}", config.getTargetUrl());
        
        // TODO: 实现API接口采集
        // 1. 构建请求参数
        // 2. 发送HTTP请求
        // 3. 解析响应数据
        // 4. 数据清洗和保存
        
        return 0;
    }
    
    /**
     * 执行数据库采集
     */
    private int executeDatabaseCollect(CollectConfig config, Long taskId, Long robotId) {
        log.info("执行数据库采集");
        
        // TODO: 实现数据库采集
        // 1. 建立数据库连接
        // 2. 执行查询SQL
        // 3. 处理结果集
        // 4. 数据转换和保存
        
        return 0;
    }
    
    /**
     * 生成数据hash
     */
    private String generateHash(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(content.hashCode());
        }
    }
    
    /**
     * 转换为DTO
     */
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
