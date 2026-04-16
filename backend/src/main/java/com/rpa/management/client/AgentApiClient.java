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
import java.util.List;
import java.util.Map;

/**
 * Python Agent (LangGraph) HTTP 客户端
 * 负责向 src/ 目录的 FastAPI 服务（port 5000）提交 ai_workflow 类型任务
 *
 * 调用链路：
 *   submitWorkflowTask() → POST /submit（非阻塞，立即返回 runId）
 *   Agent 执行完毕后 → POST /api/agent/callback（AgentCallbackController 处理）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentApiClient {

    @Value("${external.agent.base-url:http://localhost:5000}")
    private String agentBaseUrl;

    @Value("${external.agent.callback-url:http://localhost:8080/api/agent/callback}")
    private String callbackUrl;

    private final RestTemplate restTemplate;

    /**
     * 非阻塞提交 AI 工作流任务。
     * Agent 接收后立即返回 {runId, status:"accepted"}，
     * 工作流执行完毕后由 Agent 主动 POST callbackUrl 通知本系统。
     *
     * @param taskId     管理系统任务编号（如 TASK-20240101-001）
     * @param workflowId 工作流定义ID
     * @param params     任务参数（JSON）
     * @return Agent 分配的 runId（可用于后续取消）
     */
    public String submitWorkflowTask(String taskId, Long workflowId, JSONObject params) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", taskId);
        payload.put("workflowId", workflowId);
        payload.put("callbackUrl", callbackUrl);
        if (params != null) {
            payload.put("params", params);
        }

        log.info("提交 AI 工作流任务到 Agent: taskId={}, workflowId={}", taskId, workflowId);
        try {
            // /submit 端点：立即返回，不阻塞等待工作流完成
            ResponseEntity<String> response = restTemplate.postForEntity(
                    agentBaseUrl + "/submit", buildJsonEntity(payload), String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Agent 返回异常状态: " + response.getStatusCode());
            }

            // 解析 runId，供调用方记录（可用于取消）
            String runId = null;
            if (response.getBody() != null) {
                try {
                    JSONObject body = JSON.parseObject(response.getBody());
                    runId = body.getString("runId");
                } catch (Exception ignored) {}
            }
            log.info("AI 工作流任务已提交（等待回调）: taskId={}, runId={}", taskId, runId);
            return runId != null ? runId : "";

        } catch (Exception e) {
            log.error("提交 AI 工作流任务失败: taskId={}", taskId, e);
            throw new RuntimeException("提交 AI 工作流任务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 取消正在执行的 Agent 任务
     *
     * @param runId Agent 运行ID（由 submitWorkflowTask 返回）
     */
    public void cancelTask(String runId) {
        if (runId == null || runId.isBlank()) {
            log.warn("cancelTask: runId 为空，跳过取消");
            return;
        }
        try {
            String url = agentBaseUrl + "/cancel/" + runId;
            restTemplate.postForEntity(url, buildJsonEntity(new HashMap<>()), String.class);
            log.info("已发送取消请求: runId={}", runId);
        } catch (Exception e) {
            log.warn("取消 Agent 任务失败: runId={}", runId, e);
        }
    }

    /**
     * 检查 Agent 服务健康状态
     *
     * @return "ok" 表示服务正常，"unknown" 表示无法连接
     */
    public String checkHealth() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    agentBaseUrl + "/health", String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject body = JSON.parseObject(response.getBody());
                return body.getString("status");
            }
        } catch (Exception e) {
            log.warn("Agent 健康检查失败: {}", e.getMessage());
        }
        return "unknown";
    }

    public String chatCompletion(List<Map<String, String>> messages) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", "gpt-4o-mini");
        payload.put("messages", messages);
        payload.put("temperature", 0.3);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    agentBaseUrl + "/v1/chat/completions", buildJsonEntity(payload), String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Agent 问答接口返回异常: " + response.getStatusCode());
            }

            JSONObject body = JSON.parseObject(response.getBody());
            if (body == null) {
                throw new RuntimeException("Agent 问答接口返回空响应");
            }

            if (body.getJSONArray("choices") != null && !body.getJSONArray("choices").isEmpty()) {
                JSONObject firstChoice = body.getJSONArray("choices").getJSONObject(0);
                if (firstChoice != null && firstChoice.getJSONObject("message") != null) {
                    String content = firstChoice.getJSONObject("message").getString("content");
                    if (content != null) {
                        return content;
                    }
                }
            }

            throw new RuntimeException("Agent 问答接口未返回可用内容");
        } catch (Exception ex) {
            log.error("调用 Agent 问答接口失败", ex);
            throw new RuntimeException("调用 Agent 问答接口失败: " + ex.getMessage(), ex);
        }
    }

    private HttpEntity<Map<String, Object>> buildJsonEntity(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
