package com.rpa.management.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.rpa.management.client.AgentApiClient;
import com.rpa.management.dto.WorkflowAssistantDraftRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowAssistantService {

    private final AgentApiClient agentApiClient;

    public Map<String, Object> createDraft(WorkflowAssistantDraftRequest request) {
        if (request == null || !StringUtils.hasText(request.getPrompt())) {
            throw new RuntimeException("流程需求描述不能为空");
        }

        String systemPrompt = """
                你是 RPA 平台里的流程设计助手。
                请把用户需求转换成可编辑的流程草稿，只返回 JSON，不要输出解释性文字。

                返回格式必须是：
                {
                  "name": "流程名称",
                  "description": "流程说明",
                  "category": "data_collection|analysis|report|sync|approval|monitor|notification|file|transform|other",
                  "inputSchema": {
                    "type": "object",
                    "properties": {},
                    "required": []
                  },
                  "graph": {
                    "version": 2,
                    "nodes": [
                      {
                        "id": "start_1",
                        "type": "start",
                        "label": "开始",
                        "description": "",
                        "position": { "x": 120, "y": 180 },
                        "config": {}
                      },
                      {
                        "id": "end_1",
                        "type": "end",
                        "label": "结束",
                        "description": "",
                        "position": { "x": 420, "y": 180 },
                        "config": {}
                      }
                    ],
                    "edges": [
                      {
                        "id": "edge_start_end",
                        "source": "start_1",
                        "sourceHandle": "out",
                        "target": "end_1",
                        "targetHandle": "in"
                      }
                    ]
                  },
                  "warnings": ["需要人工确认的限制"]
                }

                规则：
                1. graph 必须是 version=2 的通用节点图结构，nodes 和 edges 必须存在。
                2. 优先使用这些节点类型：start、end、http_request、web_crawl、ai_filter、condition、parallel_split、merge、transform、notification。
                3. 如果需求超出第一版执行能力，也要保留草稿，但把限制写进 warnings。
                4. inputSchema 必须能驱动前端动态表单。
                5. position 请给出合理的从左到右布局。
                6. 返回 JSON 时不要使用 Markdown 代码块。
                """;

        String userPrompt = """
                用户需求：
                %s

                当前草稿 graph（可选）：
                %s

                当前草稿 inputSchema（可选）：
                %s
                """.formatted(
                request.getPrompt().trim(),
                StringUtils.hasText(request.getCurrentGraph()) ? request.getCurrentGraph() : "{}",
                StringUtils.hasText(request.getCurrentInputSchema()) ? request.getCurrentInputSchema() : "{}"
        );

        String raw = agentApiClient.chatCompletion(List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ), null);

        return parseJson(raw);
    }

    private JSONObject parseJson(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new RuntimeException("AI 未返回流程草稿");
        }
        try {
            return JSON.parseObject(raw);
        } catch (Exception ignored) {
        }

        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            try {
                return JSON.parseObject(raw.substring(start, end + 1));
            } catch (Exception ex) {
                log.warn("Failed to parse workflow assistant response", ex);
            }
        }
        throw new RuntimeException("AI 返回的流程草稿无法解析为 JSON");
    }
}
