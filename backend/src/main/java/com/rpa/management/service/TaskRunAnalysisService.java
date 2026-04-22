package com.rpa.management.service;

import com.alibaba.fastjson2.JSON;
import com.rpa.management.client.AgentApiClient;
import com.rpa.management.dto.AiAnalysisMessageDTO;
import com.rpa.management.dto.ExecutionLogDTO;
import com.rpa.management.dto.TaskRunDTO;
import com.rpa.management.entity.AiAnalysisMessage;
import com.rpa.management.entity.Task;
import com.rpa.management.entity.TaskRun;
import com.rpa.management.repository.AiAnalysisMessageRepository;
import com.rpa.management.repository.TaskRepository;
import com.rpa.management.repository.TaskRunRepository;
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
public class TaskRunAnalysisService {

    private final TaskRunRepository taskRunRepository;
    private final TaskRepository taskRepository;
    private final AiAnalysisMessageRepository aiAnalysisMessageRepository;
    private final ExecutionLogService executionLogService;
    private final CrawlResultService crawlResultService;
    private final AgentApiClient agentApiClient;
    private final TaskRunService taskRunService;

    public List<AiAnalysisMessageDTO> getMessages(Long runId) {
        ensureCompletedRun(runId);
        return aiAnalysisMessageRepository.findByTaskRunIdOrderByCreateTimeAsc(runId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<AiAnalysisMessageDTO> askQuestion(Long runId, String question) {
        TaskRun run = ensureCompletedRun(runId);
        Task task = taskRepository.findById(run.getTaskId())
                .orElseThrow(() -> new RuntimeException("任务不存在: " + run.getTaskId()));

        String trimmedQuestion = question == null ? "" : question.trim();
        if (!StringUtils.hasText(trimmedQuestion)) {
            throw new RuntimeException("问题不能为空");
        }

        saveMessage(runId, "user", trimmedQuestion);
        List<AiAnalysisMessage> history = aiAnalysisMessageRepository.findByTaskRunIdOrderByCreateTimeAsc(runId);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", buildSystemPrompt(task, run)));
        for (AiAnalysisMessage message : history) {
            messages.add(Map.of(
                    "role", normalizeRole(message.getRole()),
                    "content", message.getContent()
            ));
        }

        String answer = agentApiClient.chatCompletion(messages, null);
        saveMessage(runId, "assistant", answer);
        return getMessages(runId);
    }

    public TaskRunDTO getRun(Long runId) {
        return taskRunService.getRun(runId);
    }

    private TaskRun ensureCompletedRun(Long runId) {
        TaskRun run = taskRunRepository.findById(runId)
                .orElseThrow(() -> new RuntimeException("任务运行不存在: " + runId));
        if (!"completed".equals(run.getStatus())) {
            throw new RuntimeException("只有已完成的任务运行才支持 AI 分析");
        }
        return run;
    }

    private String buildSystemPrompt(Task task, TaskRun run) {
        String crawlResult = "";
        try {
            if (run.getId() != null) {
                crawlResult = JSON.toJSONString(crawlResultService.getResultByTaskRunId(run.getId()));
            }
        } catch (Exception ignored) {
        }

        List<ExecutionLogDTO> logs = executionLogService.getLogsByTaskRunId(run.getId());
        String logsText = logs.stream()
                .map(item -> String.format("[%s] %s", item.getLevel(), item.getMessage()))
                .collect(Collectors.joining("\n"));

        return """
                你是 RPA 平台中的任务运行分析助手。
                你只能基于当前任务运行的结果、执行参数、执行日志和补充抓取结果回答。
                请优先帮助用户完成：
                1. 摘要
                2. 风险点提取
                3. 问答
                4. 结论生成
                如果信息不足，请明确指出“无法从当前运行上下文判断”。

                【任务名称】
                %s

                【流程名称】
                %s

                【任务运行结果】
                %s

                【执行参数】
                %s

                【执行日志】
                %s

                【补充抓取结果】
                %s
                """.formatted(
                nullSafe(task.getName()),
                nullSafe(task.getWorkflowName()),
                nullSafe(run.getResult()),
                nullSafe(run.getInputConfig()),
                nullSafe(trimLongText(logsText, 12000)),
                nullSafe(trimLongText(crawlResult, 12000))
        );
    }

    private AiAnalysisMessage saveMessage(Long runId, String role, String content) {
        AiAnalysisMessage message = new AiAnalysisMessage();
        // Keep legacy analysis_task_id populated for older MySQL schemas that still require it.
        message.setAnalysisTaskId(runId);
        message.setTaskRunId(runId);
        message.setRole(role);
        message.setContent(content);
        return aiAnalysisMessageRepository.save(message);
    }

    private AiAnalysisMessageDTO toDTO(AiAnalysisMessage message) {
        return AiAnalysisMessageDTO.builder()
                .id(message.getId())
                .analysisTaskId(message.getAnalysisTaskId())
                .taskRunId(message.getTaskRunId())
                .role(message.getRole())
                .content(message.getContent())
                .createTime(message.getCreateTime())
                .build();
    }

    private String normalizeRole(String role) {
        return "assistant".equalsIgnoreCase(role) ? "assistant" : "user";
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
}
