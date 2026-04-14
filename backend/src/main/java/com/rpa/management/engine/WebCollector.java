package com.rpa.management.engine;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.rpa.management.entity.CollectConfig;
import com.rpa.management.entity.CollectData;
import com.rpa.management.repository.CollectDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * 网页采集引擎
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebCollector {
    
    private final CollectDataRepository collectDataRepository;
    
    /**
     * 执行网页采集
     * 
     * @param config 采集配置
     * @param taskId 任务ID
     * @param robotId 机器人ID
     * @return 采集的数据数量
     */
    public int collect(CollectConfig config, Long taskId, Long robotId) {
        try {
            log.info("开始网页采集: {}", config.getTargetUrl());
            
            // 解析采集规则
            JSONObject rules = JSONObject.parseObject(config.getCollectRules());
            String listSelector = rules.getString("listSelector");
            JSONArray fields = rules.getJSONArray("fields");
            
            // 发送HTTP请求获取HTML
            Document doc = Jsoup.connect(config.getTargetUrl())
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(config.getTimeout())
                    .followRedirects(true)
                    .get();
            
            // 提取数据
            List<CollectData> dataList = new ArrayList<>();
            Elements items = doc.select(listSelector);
            
            log.info("找到 {} 个列表项", items.size());
            
            for (Element item : items) {
                try {
                    JSONObject dataContent = new JSONObject();
                    
                    // 提取每个字段
                    if (fields != null) {
                        for (int i = 0; i < fields.size(); i++) {
                            JSONObject field = fields.getJSONObject(i);
                            String name = field.getString("name");
                            String selector = field.getString("selector");
                            String type = field.getString("type");
                            
                            Element fieldElement = item.selectFirst(selector);
                            if (fieldElement != null) {
                                String value = extractValue(fieldElement, type, field);
                                dataContent.put(name, value);
                            }
                        }
                    }
                    
                    // 生成数据hash
                    String dataHash = generateHash(dataContent.toJSONString());
                    
                    // 检查是否重复
                    if (!collectDataRepository.existsByDataHash(dataHash)) {
                        CollectData data = new CollectData();
                        data.setConfigId(config.getId());
                        data.setTaskId(taskId);
                        data.setRobotId(robotId);
                        data.setSourceUrl(config.getTargetUrl());
                        data.setDataContent(dataContent.toJSONString());
                        data.setDataHash(dataHash);
                        data.setStatus("valid");
                        
                        dataList.add(data);
                    }
                    
                } catch (Exception e) {
                    log.warn("提取数据项失败: {}", e.getMessage());
                }
            }
            
            // 批量保存数据
            if (!dataList.isEmpty()) {
                collectDataRepository.saveAll(dataList);
                log.info("保存了 {} 条数据", dataList.size());
            }
            
            return dataList.size();
            
        } catch (Exception e) {
            log.error("网页采集失败: {}", e.getMessage(), e);
            throw new RuntimeException("网页采集失败: " + e.getMessage());
        }
    }
    
    /**
     * 提取字段值
     */
    private String extractValue(Element element, String type, JSONObject field) {
        if (type == null) {
            type = "text";
        }
        
        switch (type.toLowerCase()) {
            case "text":
                return element.text().trim();
            case "html":
                return element.html().trim();
            case "attr":
                String attrName = field.getString("attr");
                return attrName != null ? element.attr(attrName) : "";
            case "href":
                return element.absUrl("href");
            case "src":
                return element.absUrl("src");
            default:
                return element.text().trim();
        }
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
}
