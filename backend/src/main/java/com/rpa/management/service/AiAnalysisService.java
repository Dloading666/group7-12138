package com.rpa.management.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.rpa.management.client.AgentApiClient;
import com.rpa.management.dto.AiAnalysisMessageDTO;
import com.rpa.management.dto.CreateAiAnalysisTaskRequest;
import com.rpa.management.dto.CrawlResultDTO;
import com.rpa.management.dto.TaskDTO;
import com.rpa.management.entity.AiAnalysisMessage;
import com.rpa.management.entity.Robot;
import com.rpa.management.entity.Task;
import com.rpa.management.entity.Workflow;
import com.rpa.management.repository.AiAnalysisMessageRepository;
import com.rpa.management.repository.RobotRepository;
import com.rpa.management.repository.TaskRepository;
import com.rpa.management.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private final TaskService taskService;
    private final TaskRepository taskRepository;
    private final WorkflowRepository workflowRepository;
    private final RobotRepository robotRepository;
    private final CrawlResultService crawlResultService;
    private final AiAnalysisMessageRepository aiAnalysisMessageRepository;
    private final AgentApiClient agentApiClient;

    @Transactional
    public TaskDTO createAnalysisTask(CreateAiAnalysisTaskRequest request, Long userId, String userName) {
        CrawlResultDTO source = crawlResultService.getResultByTaskRecordId(request.getSourceTaskRecordId());
        if (!"completed".equals(source.getStatus())) {
            throw new RuntimeException("只能分析已完成的数据采集结果");
        }

        Workflow workflow = workflowRepository.findById(request.getWorkflowId())
                .orElseThrow(() -> new RuntimeException("分析流程不存在: " + request.getWorkflowId()));
        if (!"published".equals(workflow.getStatus())) {
            throw new RuntimeException("只能选择已发布的流程");
        }

        Robot robot = robotRepository.findById(request.getRobotId())
                .orElseThrow(() -> new RuntimeException("执行机器人不存在: " + request.getRobotId()));

        JSONObject params = new JSONObject();
        params.put("sourceTaskRecordId", source.getTaskRecordId());
        params.put("sourceTaskId", source.getTaskId());
        params.put("sourceTaskName", source.getTaskName());
        params.put("sourceTitle", source.getTitle());
        params.put("sourceFinalUrl", source.getFinalUrl());
        params.put("workflowId", workflow.getId());
        params.put("workflowName", workflow.getName());
        params.put("type", StringUtils.hasText(request.getQuery()) ? "qa" : "analysis");
        if (StringUtils.hasText(request.getQuery())) {
            params.put("query", request.getQuery().trim());
        }

        String taskName = buildTaskName(source, workflow);
        TaskDTO dto = TaskDTO.builder()
                .name(taskName)
                .type("ai_workflow")
                .robotId(robot.getId())
                .robotName(robot.getName())
                .priority("medium")
                .executeType("immediate")
                .description("基于采集结果进行 AI 分析")
                .params(params.toJSONString())
                .build();

        return taskService.createTask(dto, userId, userName);
    }

    public List<AiAnalysisMessageDTO> getMessages(Long analysisTaskId) {
        ensureAiTask(analysisTaskId);
        return aiAnalysisMessageRepository.findByAnalysisTaskIdOrderByCreateTimeAsc(analysisTaskId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<AiAnalysisMessageDTO> askQuestion(Long analysisTaskId, String question) {
        Task task = ensureAiTask(analysisTaskId);
        if (!"completed".equals(task.getStatus())) {
            throw new RuntimeException("请先等待 AI 分析任务执行完成");
        }

        String trimmedQuestion = question == null ? "" : question.trim();
        if (!StringUtils.hasText(trimmedQuestion)) {
            throw new RuntimeException("问题不能为空");
        }

        CrawlResultDTO source = resolveSource(task);
        AiAnalysisMessage userMessage = saveMessage(analysisTaskId, "user", trimmedQuestion);
        List<AiAnalysisMessage> history = aiAnalysisMessageRepository.findByAnalysisTaskIdOrderByCreateTimeAsc(analysisTaskId);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", buildSystemPrompt(source, task.getResult())));
        for (AiAnalysisMessage message : history) {
            messages.add(Map.of(
                    "role", normalizeRole(message.getRole()),
                    "content", message.getContent()
            ));
        }

        String answer = agentApiClient.chatCompletion(messages);
        saveMessage(analysisTaskId, "assistant", answer);

        log.info("AI follow-up answered: taskId={}, questionLength={}", analysisTaskId, trimmedQuestion.length());
        return aiAnalysisMessageRepository.findByAnalysisTaskIdOrderByCreateTimeAsc(analysisTaskId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void saveInitialResultMessageIfAbsent(Long analysisTaskId, String content) {
        if (!StringUtils.hasText(content)) {
            return;
        }
        if (aiAnalysisMessageRepository.countByAnalysisTaskId(analysisTaskId) > 0) {
            return;
        }
        saveMessage(analysisTaskId, "assistant", content);
    }

    private Task ensureAiTask(Long analysisTaskId) {
        Task task = taskRepository.findById(analysisTaskId)
                .orElseThrow(() -> new RuntimeException("AI 分析任务不存在: " + analysisTaskId));
        if (!"ai_workflow".equals(task.getType())) {
            throw new RuntimeException("当前任务不是 AI 分析任务");
        }
        return task;
    }

    private CrawlResultDTO resolveSource(Task task) {
        JSONObject params = parseParams(task.getParams());
        if (params == null) {
            throw new RuntimeException("AI 分析任务缺少来源参数");
        }

        Long sourceTaskRecordId = params.getLong("sourceTaskRecordId");
        if (sourceTaskRecordId != null) {
            return crawlResultService.getResultByTaskRecordId(sourceTaskRecordId);
        }

        String sourceTaskId = params.getString("sourceTaskId");
        if (StringUtils.hasText(sourceTaskId)) {
            return crawlResultService.getResultByTaskId(sourceTaskId);
        }

        throw new RuntimeException("AI 分析任务缺少来源采集结果");
    }

    private JSONObject parseParams(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return JSON.parseObject(raw);
        } catch (Exception ex) {
            throw new RuntimeException("AI 分析任务参数格式不正确");
        }
    }

    private String buildTaskName(CrawlResultDTO source, Workflow workflow) {
        String sourceName = StringUtils.hasText(source.getTitle()) ? source.getTitle() : source.getTaskName();
        if (!StringUtils.hasText(sourceName)) {
            sourceName = source.getTaskId();
        }
        return "AI分析 - " + sourceName + " - " + workflow.getName();
    }

    private String buildSystemPrompt(CrawlResultDTO source, String initialAnalysis) {
        String structuredData = source.getStructuredData() == null || source.getStructuredData().isEmpty()
                ? "[]"
                : JSON.toJSONString(source.getStructuredData());

        String safeSummary = trimLongText(source.getSummaryText(), 6000);
        String safeAnalysis = trimLongText(initialAnalysis, 8000);

        return """
                你是 RPA 管理平台里的 AI 分析助手。
                请只基于当前采集结果和已有分析内容回答问题，不要编造来源中没有的信息。
                如果问题超出上下文，请明确说明无法从当前采集结果判断。

                【采集结果标题】
                %s

                【采集结果 URL】
                %s

                【采集结果摘要】
                %s

                【结构化数据】
                %s

                【首次分析结论】
                %s
                """.formatted(
                nullSafe(source.getTitle()),
                nullSafe(source.getFinalUrl()),
                nullSafe(safeSummary),
                structuredData,
                nullSafe(safeAnalysis)
        );
    }

    private String trimLongText(String raw, int maxLength) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        return raw.length() <= maxLength ? raw : raw.substring(0, maxLength);
    }

    private String nullSafe(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }

    private String normalizeRole(String role) {
        return "assistant".equalsIgnoreCase(role) ? "assistant" : "user";
    }

    private AiAnalysisMessage saveMessage(Long analysisTaskId, String role, String content) {
        AiAnalysisMessage message = new AiAnalysisMessage();
        message.setAnalysisTaskId(analysisTaskId);
        message.setRole(role);
        message.setContent(content);
        return aiAnalysisMessageRepository.save(message);
    }

    private AiAnalysisMessageDTO toDTO(AiAnalysisMessage message) {
        return AiAnalysisMessageDTO.builder()
                .id(message.getId())
                .analysisTaskId(message.getAnalysisTaskId())
                .role(message.getRole())
                .content(message.getContent())
                .createTime(message.getCreateTime())
                .build();
    }
}
