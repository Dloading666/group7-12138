package com.rpa.management.engine;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpa.management.client.AgentApiClient;
import com.rpa.management.client.SpiderApiClient;
import com.rpa.management.dto.CrawlResultDTO;
import com.rpa.management.entity.Robot;
import com.rpa.management.entity.Task;
import com.rpa.management.entity.TaskRun;
import com.rpa.management.entity.WorkflowDebugRun;
import com.rpa.management.entity.WorkflowStepRun;
import com.rpa.management.repository.RobotRepository;
import com.rpa.management.service.CrawlResultService;
import com.rpa.management.service.ExecutionLogService;
import com.rpa.management.service.TaskRunService;
import com.rpa.management.service.WorkflowDebugRunService;
import com.rpa.management.service.WorkflowService;
import com.rpa.management.service.WorkflowStepRunService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowRunExecutor {

    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{\\s*([^{}]+?)\\s*}}");
    private static final Pattern QUOTED_TEMPLATE_PATTERN = Pattern.compile("\"\\{\\{\\s*([^{}]+?)\\s*}}\"");
    private static final Pattern JSON_STRING_FIELD_PATTERN = Pattern.compile(
            "\"(subject|body)\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private final TaskRunService taskRunService;
    private final WorkflowDebugRunService workflowDebugRunService;
    private final WorkflowStepRunService workflowStepRunService;
    private final WorkflowService workflowService;
    private final ExecutionLogService executionLogService;
    private final SpiderApiClient spiderApiClient;
    private final AgentApiClient agentApiClient;
    private final CrawlResultService crawlResultService;
    private final RobotRepository robotRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    @Value("${mail.resend.api-key:}")
    private String resendApiKey;
    @Value("${mail.resend.from-email:}")
    private String resendFromEmail;

    public void execute(Task task, TaskRun run) {
        executeInternal(buildTaskContext(task, run));
    }

    @Async
    public CompletableFuture<Void> executeDebugRunAsync(Long debugRunId) {
        WorkflowDebugRun debugRun = workflowDebugRunService.markRunning(debugRunId);
        executeInternal(buildDebugContext(debugRun));
        return CompletableFuture.completedFuture(null);
    }

    protected void executeInternal(ParentExecutionContext parentContext) {
        try {
            String normalizedGraph = workflowService.normalizeGraph(parentContext.graphSnapshot());
            workflowService.validateExecutableGraph(normalizedGraph);
            GraphModel graphModel = parseGraph(normalizedGraph);
            JSONObject context = createInitialContext(parentContext.inputConfig());
            runGraph(parentContext, graphModel, context);
        } catch (Exception ex) {
            log.error("Workflow graph execution failed", ex);
            parentContext.fail(ex.getMessage());
            executionLogService.error(
                    parentContext.logTaskId(),
                    parentContext.logTaskRunId(),
                    parentContext.logTaskCode(),
                    parentContext.logTaskName(),
                    null,
                    null,
                    "Workflow execution failed: " + ex.getMessage()
            );
        }
    }

    private ParentExecutionContext buildTaskContext(Task task, TaskRun run) {
        return new ParentExecutionContext() {
            @Override
            public String inputConfig() {
                return firstNonBlank(run.getInputConfig(), task.getInputConfig());
            }

            @Override
            public String graphSnapshot() {
                return run.getWorkflowSnapshot();
            }

            @Override
            public WorkflowStepRun createStepRun(GraphNode node, String branchKey, String inputSnapshot, BoundRobot robot) {
                return workflowStepRunService.createTaskStepRun(
                        run.getId(),
                        node.id,
                        node.type,
                        node.label,
                        branchKey,
                        inputSnapshot,
                        robot != null ? robot.id() : null,
                        robot != null ? robot.name() : null,
                        robot != null ? robot.type() : null
                );
            }

            @Override
            public void updateProgress(int progress) {
                taskRunService.updateProgress(task, run.getId(), progress);
            }

            @Override
            public void complete(String result) {
                taskRunService.completeRun(task, run.getId(), result);
            }

            @Override
            public void fail(String message) {
                taskRunService.failRun(task, run.getId(), message);
            }

            @Override
            public Long logTaskId() {
                return task.getId();
            }

            @Override
            public Long logTaskRunId() {
                return run.getId();
            }

            @Override
            public String logTaskCode() {
                return task.getTaskId();
            }

            @Override
            public String logTaskName() {
                return task.getName();
            }
        };
    }

    private ParentExecutionContext buildDebugContext(WorkflowDebugRun debugRun) {
        return new ParentExecutionContext() {
            @Override
            public String inputConfig() {
                return debugRun.getInputConfig();
            }

            @Override
            public String graphSnapshot() {
                return debugRun.getGraphSnapshot();
            }

            @Override
            public WorkflowStepRun createStepRun(GraphNode node, String branchKey, String inputSnapshot, BoundRobot robot) {
                return workflowStepRunService.createDebugStepRun(
                        debugRun.getId(),
                        node.id,
                        node.type,
                        node.label,
                        branchKey,
                        inputSnapshot,
                        robot != null ? robot.id() : null,
                        robot != null ? robot.name() : null,
                        robot != null ? robot.type() : null
                );
            }

            @Override
            public void updateProgress(int progress) {
                workflowDebugRunService.updateProgress(debugRun.getId(), progress);
            }

            @Override
            public void complete(String result) {
                workflowDebugRunService.complete(debugRun.getId(), result);
            }

            @Override
            public void fail(String message) {
                workflowDebugRunService.fail(debugRun.getId(), message);
            }

            @Override
            public Long logTaskId() {
                return null;
            }

            @Override
            public Long logTaskRunId() {
                return null;
            }

            @Override
            public String logTaskCode() {
                return debugRun.getRunId();
            }

            @Override
            public String logTaskName() {
                return debugRun.getWorkflowName();
            }
        };
    }

    private void runGraph(ParentExecutionContext parentContext, GraphModel graphModel, JSONObject context) {
        Map<String, String> nodeStates = new LinkedHashMap<>();
        Map<String, EdgeRuntimeState> edgeStates = new LinkedHashMap<>();
        ArrayDeque<String> queue = new ArrayDeque<>();

        graphModel.nodes.forEach((nodeId, node) -> nodeStates.put(nodeId, "pending"));
        graphModel.edges.forEach(edge -> edgeStates.put(edge.id, new EdgeRuntimeState(edge)));

        for (GraphNode node : graphModel.nodes.values()) {
            if (graphModel.incomingEdges.getOrDefault(node.id, List.of()).isEmpty()) {
                queue.add(node.id);
            }
        }

        int totalNodes = Math.max(1, graphModel.nodes.size());
        while (!queue.isEmpty()) {
            String nodeId = queue.removeFirst();
            if (!"pending".equals(nodeStates.get(nodeId))) {
                continue;
            }

            GraphNode node = graphModel.nodes.get(nodeId);
            List<EdgeRuntimeState> incoming = getIncomingStates(graphModel, edgeStates, nodeId);
            if (!incoming.isEmpty() && incoming.stream().anyMatch(edge -> "unresolved".equals(edge.status))) {
                continue;
            }

            boolean hasChosenIncoming = incoming.isEmpty() || incoming.stream().anyMatch(edge -> "chosen".equals(edge.status));
            if (!hasChosenIncoming) {
                BoundRobot boundRobot = resolveBoundRobot(graphModel.robotBindings, node);
                WorkflowStepRun skippedStep = parentContext.createStepRun(
                        node,
                        resolveBranchKey(incoming),
                        buildNodeInputSnapshot(context, node, incoming),
                        boundRobot
                );
                workflowStepRunService.skip(skippedStep.getId(), "{\"skipped\":true}");
                rememberNodeResult(context, node, "skipped", JSONObject.of("skipped", true), resolveBranchKey(incoming));
                nodeStates.put(nodeId, "skipped");
                resolveOutgoingEdges(graphModel, edgeStates, queue, node, "skipped", Set.of(), resolveBranchKey(incoming));
                parentContext.updateProgress(calculateProgress(nodeStates, totalNodes));
                continue;
            }

            String branchKey = resolveBranchKey(incoming);
            String inputSnapshot = buildNodeInputSnapshot(context, node, incoming);
            BoundRobot boundRobot = resolveBoundRobot(graphModel.robotBindings, node);
            WorkflowStepRun stepRun = parentContext.createStepRun(node, branchKey, inputSnapshot, boundRobot);
            workflowStepRunService.start(stepRun);
            executionLogService.info(
                    parentContext.logTaskId(),
                    parentContext.logTaskRunId(),
                    parentContext.logTaskCode(),
                    parentContext.logTaskName(),
                    null,
                    null,
                    "Execute workflow node: " + node.id + " / " + node.type + formatRobotLogSuffix(boundRobot)
            );

            try {
                NodeExecutionResult result = executeNode(context, node, incoming, branchKey, stepRun);
                workflowStepRunService.complete(stepRun.getId(), JSON.toJSONString(result.output));
                rememberNodeResult(context, node, "completed", result.output, branchKey);
                nodeStates.put(nodeId, "completed");

                boolean handled = resolveOutgoingEdges(
                        graphModel,
                        edgeStates,
                        queue,
                        node,
                        "completed",
                        result.chosenHandles,
                        branchKey
                );

                if (hasOutgoingEdges(graphModel, node.id)
                        && !"parallel_split".equals(node.type)
                        && !"end".equals(node.type)
                        && !handled) {
                    throw new RuntimeException("No downstream edge matched node output handle: " + node.id);
                }
            } catch (Exception ex) {
                workflowStepRunService.fail(stepRun.getId(), ex.getMessage());
                rememberNodeResult(context, node, "failed", JSONObject.of("error", ex.getMessage()), branchKey);
                nodeStates.put(nodeId, "failed");
                boolean handled = resolveOutgoingEdges(
                        graphModel,
                        edgeStates,
                        queue,
                        node,
                        "failed",
                        Set.of("error"),
                        branchKey
                );
                if (!handled) {
                    throw new RuntimeException(ex.getMessage(), ex);
                }
            }

            parentContext.updateProgress(calculateProgress(nodeStates, totalNodes));
        }

        List<String> unresolvedNodes = nodeStates.entrySet().stream()
                .filter(entry -> "pending".equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        if (!unresolvedNodes.isEmpty()) {
            throw new RuntimeException("Workflow graph could not resolve downstream nodes: " + unresolvedNodes);
        }

        parentContext.complete(formatResult(context, graphModel));
    }

    private int calculateProgress(Map<String, String> nodeStates, int totalNodes) {
        long finished = nodeStates.values().stream()
                .filter(status -> !"pending".equals(status) && !"running".equals(status))
                .count();
        return Math.min(95, (int) Math.round((finished * 95.0) / totalNodes));
    }

    private boolean resolveOutgoingEdges(GraphModel graphModel,
                                         Map<String, EdgeRuntimeState> edgeStates,
                                         ArrayDeque<String> queue,
                                         GraphNode node,
                                         String nodeStatus,
                                         Set<String> chosenHandles,
                                         String branchKey) {
        boolean choseAtLeastOne = false;
        for (EdgeDef edge : graphModel.outgoingEdges.getOrDefault(node.id, List.of())) {
            EdgeRuntimeState runtimeState = edgeStates.get(edge.id);
            if (!"unresolved".equals(runtimeState.status)) {
                continue;
            }

            boolean chosen;
            if ("failed".equals(nodeStatus)) {
                chosen = chosenHandles.contains(edge.sourceHandle);
            } else if ("skipped".equals(nodeStatus)) {
                chosen = false;
            } else if ("parallel_split".equals(node.type)) {
                chosen = !"error".equals(edge.sourceHandle);
            } else if (chosenHandles.isEmpty()) {
                chosen = Set.of(defaultSuccessHandle(node.type), "out").contains(edge.sourceHandle);
            } else {
                chosen = chosenHandles.contains(edge.sourceHandle);
            }

            runtimeState.status = chosen ? "chosen" : "skipped";
            runtimeState.branchKey = chosen ? resolveEdgeBranchKey(node, edge, branchKey) : null;
            if (chosen) {
                choseAtLeastOne = true;
            }
            maybeQueueTarget(graphModel, edgeStates, queue, edge.target);
        }
        return choseAtLeastOne;
    }

    private boolean hasOutgoingEdges(GraphModel graphModel, String nodeId) {
        return !graphModel.outgoingEdges.getOrDefault(nodeId, List.of()).isEmpty();
    }

    private void maybeQueueTarget(GraphModel graphModel,
                                  Map<String, EdgeRuntimeState> edgeStates,
                                  ArrayDeque<String> queue,
                                  String targetNodeId) {
        List<EdgeRuntimeState> incomingStates = getIncomingStates(graphModel, edgeStates, targetNodeId);
        if (!incomingStates.isEmpty() && incomingStates.stream().allMatch(edge -> !"unresolved".equals(edge.status))) {
            queue.add(targetNodeId);
        }
    }

    private List<EdgeRuntimeState> getIncomingStates(GraphModel graphModel,
                                                     Map<String, EdgeRuntimeState> edgeStates,
                                                     String nodeId) {
        List<EdgeRuntimeState> states = new ArrayList<>();
        for (EdgeDef edge : graphModel.incomingEdges.getOrDefault(nodeId, List.of())) {
            states.add(edgeStates.get(edge.id));
        }
        return states;
    }

    private NodeExecutionResult executeNode(JSONObject context,
                                            GraphNode node,
                                            List<EdgeRuntimeState> incoming,
                                            String branchKey,
                                            WorkflowStepRun stepRun) {
        return switch (node.type) {
            case "start" -> NodeExecutionResult.success((JSONObject) context.get("input"), Set.of("out"));
            case "end" -> NodeExecutionResult.success(buildEndOutput(context, incoming), Set.of());
            case "http_request" -> executeHttpNode(context, node);
            case "web_crawl" -> executeCrawlNode(context, node, stepRun);
            case "ai_filter" -> executeAiFilterNode(context, node);
            case "condition" -> executeConditionNode(context, node);
            case "parallel_split" -> executeParallelSplitNode(node);
            case "merge" -> executeMergeNode(context, incoming);
            case "transform" -> executeTransformNode(context, node);
            case "notification" -> executeNotificationNode(context, node);
            default -> throw new RuntimeException("Unsupported node type: " + node.type);
        };
    }

    private NodeExecutionResult executeHttpNode(JSONObject context, GraphNode node) {
        JSONObject config = node.config;
        String provider = config.getString("provider");
        if ("resend_email".equalsIgnoreCase(provider)) {
            return executeResendEmailNode(context, config);
        }
        JSONObject compatibleResendConfig = adaptLegacyResendConfig(config);
        if (compatibleResendConfig != null) {
            return executeResendEmailNode(context, compatibleResendConfig);
        }

        String url = resolveString(config.getString("url"), context);
        if (!StringUtils.hasText(url)) {
            throw new RuntimeException("HTTP node url is required");
        }

        HttpMethod method;
        try {
            method = HttpMethod.valueOf(resolveString(config.getString("method"), context, "GET").toUpperCase());
        } catch (Exception ex) {
            method = HttpMethod.GET;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JSONObject headerConfig = parseJsonObject(config.get("headers"));
        if (headerConfig != null) {
            headerConfig.forEach((key, value) -> headers.add(key, renderTemplate(String.valueOf(value), context)));
        }

        Object body = config.get("body");
        if (body instanceof String textBody) {
            body = isJsonRequest(headers, textBody)
                    ? renderJsonTemplate(textBody, context)
                    : renderTemplate(textBody, context);
        }
        HttpEntity<Object> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, method, requestEntity, String.class);

        JSONObject output = new JSONObject();
        output.put("statusCode", response.getStatusCode().value());
        output.put("body", response.getBody());
        output.put("headers", response.getHeaders().toSingleValueMap());
        return NodeExecutionResult.success(output, Set.of("success", "out"));
    }

    private JSONObject adaptLegacyResendConfig(JSONObject config) {
        if (!looksLikeLegacyResendHttpNode(config)) {
            return null;
        }

        JSONObject adapted = new JSONObject();
        adapted.put("provider", "resend_email");
        String legacyUrl = config.getString("url");
        adapted.put("url", looksLikePlaceholderMailServiceUrl(legacyUrl)
                ? "https://api.resend.com/emails"
                : firstNonBlank(legacyUrl, "https://api.resend.com/emails"));
        adapted.put("method", "POST");
        adapted.put("headers", new JSONObject());
        adapted.put("body", "");
        adapted.put("timeout", config.getInteger("timeout") != null ? config.getInteger("timeout") : 30000);

        JSONObject bodyConfig = parseJsonObject(config.get("body"));
        String to = firstNonBlank(
                config.getString("to"),
                extractStringValue(bodyConfig, "to")
        );
        String subjectTemplate = firstNonBlank(
                config.getString("subjectTemplate"),
                config.getString("subject"),
                extractStringValue(bodyConfig, "subject"),
                extractStringValue(bodyConfig, "title")
        );
        String textTemplate = firstNonBlank(
                config.getString("textTemplate"),
                config.getString("bodyTemplate"),
                config.getString("text"),
                config.getString("body"),
                extractStringValue(bodyConfig, "text"),
                extractStringValue(bodyConfig, "content"),
                extractStringValue(bodyConfig, "body")
        );
        String htmlTemplate = firstNonBlank(
                config.getString("htmlTemplate"),
                config.getString("html"),
                extractStringValue(bodyConfig, "html")
        );

        adapted.put("to", looksLikeTemplateValue(to) ? to : "{{ input.to_email }}");
        if (StringUtils.hasText(to) && !looksLikeTemplateValue(to)) {
            adapted.put("defaultTo", to);
        }
        adapted.put("subjectTemplate", firstNonBlank(subjectTemplate, "网站采集通知"));
        adapted.put("textTemplate", firstNonBlank(textTemplate, ""));
        adapted.put("htmlTemplate", firstNonBlank(htmlTemplate, ""));
        return adapted;
    }

    private boolean looksLikeLegacyResendHttpNode(JSONObject config) {
        if (config == null) {
            return false;
        }
        String url = config.getString("url");
        if (StringUtils.hasText(url) && (url.contains("api.resend.com/emails") || looksLikePlaceholderMailServiceUrl(url))) {
            return true;
        }
        JSONObject headers = parseJsonObject(config.get("headers"));
        if (headers != null) {
            Object authorization = headers.get("Authorization");
            if (authorization == null) {
                authorization = headers.get("authorization");
            }
            if (authorization != null && String.valueOf(authorization).contains("RESEND_API_KEY")) {
                return true;
            }
        }
        String body = config.getString("body");
        if (StringUtils.hasText(body)
                && body.contains("\"subject\"")
                && (body.contains("\"html\"") || body.contains("\"text\"") || body.contains("\"content\"") || body.contains("\"body\""))) {
            return true;
        }

        JSONObject bodyConfig = parseJsonObject(config.get("body"));
        return StringUtils.hasText(firstNonBlank(config.getString("to"), extractStringValue(bodyConfig, "to")))
                && StringUtils.hasText(firstNonBlank(
                config.getString("subjectTemplate"),
                config.getString("subject"),
                extractStringValue(bodyConfig, "subject"),
                extractStringValue(bodyConfig, "title")
        ))
                && StringUtils.hasText(firstNonBlank(
                config.getString("textTemplate"),
                config.getString("bodyTemplate"),
                config.getString("text"),
                config.getString("body"),
                config.getString("html"),
                extractStringValue(bodyConfig, "text"),
                extractStringValue(bodyConfig, "content"),
                extractStringValue(bodyConfig, "body"),
                extractStringValue(bodyConfig, "html")
        ));
    }

    private NodeExecutionResult executeResendEmailNode(JSONObject context, JSONObject config) {
        if (!StringUtils.hasText(resendApiKey)) {
            throw new RuntimeException("RESEND_API_KEY is not configured");
        }
        if (!StringUtils.hasText(resendFromEmail)) {
            throw new RuntimeException("RESEND_FROM_EMAIL is not configured");
        }

        String toEmail = resolveString(firstNonBlank(config.getString("to"), "{{ input.to_email }}"), context);
        if (!StringUtils.hasText(toEmail)) {
            toEmail = resolveString(config.getString("defaultTo"), context);
        }
        if (!StringUtils.hasText(toEmail)) {
            throw new RuntimeException("Resend email recipient is required");
        }

        String subject = resolveString(
                firstNonBlank(config.getString("subjectTemplate"), "网站采集通知"),
                context,
                "网站采集通知"
        );
        String text = resolveString(config.getString("textTemplate"), context);
        String html = resolveString(config.getString("htmlTemplate"), context);
        if (!StringUtils.hasText(text) && !StringUtils.hasText(html)) {
            throw new RuntimeException("Resend email requires textTemplate or htmlTemplate");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(resendApiKey);

        JSONObject extraHeaders = parseJsonObject(config.get("headers"));
        if (extraHeaders != null) {
            extraHeaders.forEach((key, value) -> {
                String headerName = String.valueOf(key);
                if ("authorization".equalsIgnoreCase(headerName) || "content-type".equalsIgnoreCase(headerName)) {
                    return;
                }
                headers.add(headerName, renderTemplate(String.valueOf(value), context));
            });
        }

        JSONObject body = new JSONObject();
        body.put("from", resendFromEmail);
        JSONArray recipients = new JSONArray();
        recipients.add(toEmail);
        body.put("to", recipients);
        body.put("subject", subject);
        if (StringUtils.hasText(html)) {
            body.put("html", html);
        }
        if (StringUtils.hasText(text)) {
            body.put("text", text);
        }

        String url = firstNonBlank(config.getString("url"), "https://api.resend.com/emails");
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(body.toJSONString(), headers),
                String.class
        );

        JSONObject output = new JSONObject();
        output.put("provider", "resend_email");
        output.put("statusCode", response.getStatusCode().value());
        output.put("body", response.getBody());
        output.put("headers", response.getHeaders().toSingleValueMap());
        return NodeExecutionResult.success(output, Set.of("success", "out"));
    }

    private NodeExecutionResult executeCrawlNode(JSONObject context, GraphNode node, WorkflowStepRun stepRun) {
        JSONObject config = node.config;
        JSONObject crawlConfig = new JSONObject();
        crawlConfig.put("url", resolveString(config.getString("url"), context));
        crawlConfig.put("timeout", config.getInteger("timeout") != null ? config.getInteger("timeout") : 30000);
        crawlConfig.put("headers", parseJsonObject(config.get("headers")));
        if (config.get("cookies") != null) {
            crawlConfig.put("cookies", parseJsonArray(config.get("cookies")));
        }
        if (config.get("extractionRules") != null) {
            crawlConfig.put("extractionRules", normalizeExtractionRules(config.get("extractionRules")));
        }
        if (config.get("pagination") != null) {
            crawlConfig.put("pagination", parseJsonObject(config.get("pagination")));
        }
        if (config.get("login") != null) {
            crawlConfig.put("login", parseJsonObject(config.get("login")));
        }

        String externalTaskId = stepRun.getStepRunId();
        workflowStepRunService.bindEngineTaskId(stepRun.getId(), externalTaskId);
        spiderApiClient.submitGenericCrawlTask(externalTaskId, crawlConfig);
        CrawlResultDTO crawlResult = waitForCrawlCompletion(externalTaskId, crawlConfig.getInteger("timeout"));

        JSONObject output = new JSONObject();
        output.put("taskId", externalTaskId);
        output.put("finalUrl", crawlResult.getFinalUrl());
        output.put("title", crawlResult.getTitle());
        output.put("summaryText", crawlResult.getSummaryText());
        output.put("structuredData", crawlResult.getStructuredData());
        output.put("totalCount", crawlResult.getTotalCount());
        output.put("crawledPages", crawlResult.getCrawledPages());
        return NodeExecutionResult.success(output, Set.of("success", "out"));
    }

    private CrawlResultDTO waitForCrawlCompletion(String externalTaskId, Integer timeoutMillis) {
        int maxWaitSeconds = Math.max(90, ((timeoutMillis != null ? timeoutMillis : 30000) / 1000) + 90);
        int waited = 0;
        while (waited < maxWaitSeconds) {
            sleep(2000L);
            waited += 2;
            try {
                CrawlResultDTO crawlResult = crawlResultService.getResultByTaskId(externalTaskId);
                if ("completed".equals(crawlResult.getStatus())) {
                    return crawlResult;
                }
                if ("failed".equals(crawlResult.getStatus())) {
                    throw new RuntimeException(
                            StringUtils.hasText(crawlResult.getErrorMessage())
                                    ? crawlResult.getErrorMessage()
                                    : "Crawl failed"
                    );
                }
            } catch (RuntimeException ignored) {
            }
        }
        throw new RuntimeException("Crawl timeout");
    }

    private NodeExecutionResult executeAiFilterNode(JSONObject context, GraphNode node) {
        JSONObject config = node.config;
        String model = config.getString("model");
        String systemPrompt = resolveString(
                firstNonBlank(config.getString("systemPrompt"), config.getString("systemPromptTemplate")),
                context,
                "You are an RPA workflow node. Follow the instruction and return concise output."
        );
        String userPrompt = resolveString(
                firstNonBlank(config.getString("userPromptTemplate"), config.getString("userPrompt")),
                context
        );
        if (!StringUtils.hasText(userPrompt)) {
            String inputPath = config.getString("inputPath");
            Object value = StringUtils.hasText(inputPath) ? readPathValue(context, inputPath) : context.get("input");
            userPrompt = value == null ? "{}" : JSON.toJSONString(value);
        }

        Integer maxTokens = config.getInteger("maxTokens");
        String raw = agentApiClient.chatCompletion(List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ), model, maxTokens != null && maxTokens > 0 ? maxTokens : 1024);

        String outputFormat = config.getString("outputFormat");
        Object structured = null;
        if ("json".equalsIgnoreCase(outputFormat)) {
            structured = parseStructuredOutput(raw);
        }

        JSONObject wrapped = new JSONObject();
        wrapped.put("model", model);
        wrapped.put("content", raw);
        wrapped.put("structured", structured);
        return NodeExecutionResult.success(wrapped, Set.of("success", "out"));
    }

    private Object parseStructuredOutput(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }

        try {
            return JSON.parse(raw);
        } catch (Exception ignored) {
        }

        String text = raw.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```(?:json)?\\s*", "");
            text = text.replaceFirst("\\s*```$", "");
            try {
                return JSON.parse(text.trim());
            } catch (Exception ignored) {
            }
        }

        int objectStart = text.indexOf('{');
        int objectEnd = text.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            try {
                return JSON.parse(text.substring(objectStart, objectEnd + 1));
            } catch (Exception ignored) {
            }
        }

        int arrayStart = text.indexOf('[');
        int arrayEnd = text.lastIndexOf(']');
        if (arrayStart >= 0 && arrayEnd > arrayStart) {
            try {
                return JSON.parse(text.substring(arrayStart, arrayEnd + 1));
            } catch (Exception ignored) {
            }
        }

        JSONObject fallback = extractSubjectBodyFallback(text);
        return fallback.isEmpty() ? null : fallback;
    }

    private JSONObject extractSubjectBodyFallback(String text) {
        JSONObject fallback = new JSONObject();
        Matcher matcher = JSON_STRING_FIELD_PATTERN.matcher(text);
        while (matcher.find()) {
            String key = matcher.group(1);
            String rawValue = matcher.group(2);
            String decoded = decodeJsonString(rawValue);
            if (StringUtils.hasText(decoded)) {
                fallback.put(key.toLowerCase(), decoded);
            }
        }
        return fallback;
    }

    private String decodeJsonString(String rawValue) {
        if (rawValue == null) {
            return "";
        }
        try {
            return JSON.parse("\"" + rawValue + "\"").toString();
        } catch (Exception ignored) {
            return rawValue
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        }
    }

    private NodeExecutionResult executeConditionNode(JSONObject context, GraphNode node) {
        JSONObject config = node.config;
        String leftPath = firstNonBlank(config.getString("leftPath"), config.getString("field"));
        String operator = firstNonBlank(config.getString("operator"), "exists");
        Object actual = readPathValue(context, leftPath);
        Object expected = config.containsKey("rightPath")
                ? readPathValue(context, config.getString("rightPath"))
                : config.get("rightValue");
        if (expected == null && config.containsKey("value")) {
            expected = config.get("value");
        }

        boolean matched = switch (operator) {
            case "equals" -> String.valueOf(actual).equals(String.valueOf(expected));
            case "contains" -> actual != null && String.valueOf(actual).contains(String.valueOf(expected));
            case "not_empty" -> actual != null && StringUtils.hasText(String.valueOf(actual));
            case "greater_than" -> toDouble(actual) > toDouble(expected);
            case "less_than" -> toDouble(actual) < toDouble(expected);
            default -> actual != null;
        };

        JSONObject output = new JSONObject();
        output.put("matched", matched);
        output.put("actual", actual);
        output.put("expected", expected);
        return NodeExecutionResult.success(output, Set.of(matched ? "true" : "false"));
    }

    private NodeExecutionResult executeParallelSplitNode(GraphNode node) {
        JSONObject output = new JSONObject();
        JSONArray branches = parseJsonArray(node.config.get("branches"));
        output.put("branches", branches == null || branches.isEmpty() ? List.of("branch_a", "branch_b") : branches);
        return NodeExecutionResult.success(output, Set.of());
    }

    private NodeExecutionResult executeMergeNode(JSONObject context, List<EdgeRuntimeState> incoming) {
        JSONObject branches = new JSONObject();
        for (EdgeRuntimeState edge : incoming) {
            if (!"chosen".equals(edge.status)) {
                continue;
            }
            Object sourceOutput = readPathValue(context, "nodes." + edge.edge.source + ".output");
            branches.put(StringUtils.hasText(edge.branchKey) ? edge.branchKey : edge.edge.source, sourceOutput);
        }

        JSONObject output = new JSONObject();
        output.put("branches", branches);
        return NodeExecutionResult.success(output, Set.of("out"));
    }

    private NodeExecutionResult executeTransformNode(JSONObject context, GraphNode node) {
        JSONObject mappings = parseJsonObject(node.config.get("mappings"));
        if (mappings == null || mappings.isEmpty()) {
            return NodeExecutionResult.success(new JSONObject(), Set.of("success", "out"));
        }

        JSONObject transformed = new JSONObject();
        mappings.forEach((targetField, sourcePath) -> transformed.put(
                targetField,
                readPathValue(context, String.valueOf(sourcePath))
        ));
        return NodeExecutionResult.success(transformed, Set.of("success", "out"));
    }

    private NodeExecutionResult executeNotificationNode(JSONObject context, GraphNode node) {
        JSONObject output = new JSONObject();
        output.put("channel", firstNonBlank(node.config.getString("channel"), "email"));
        output.put("to", resolveString(firstNonBlank(node.config.getString("to"), node.config.getString("recipients")), context));
        output.put("subject", resolveString(node.config.getString("subject"), context, "Workflow notification"));
        output.put("body", resolveString(
                firstNonBlank(node.config.getString("bodyTemplate"), node.config.getString("body")),
                context,
                JSON.toJSONString(context.get("nodes"))
        ));

        JSONArray notifications = context.getJSONArray("notifications");
        notifications.add(output);
        return NodeExecutionResult.success(output, Set.of("success", "out"));
    }

    private JSONObject buildEndOutput(JSONObject context, List<EdgeRuntimeState> incoming) {
        JSONObject output = new JSONObject();
        JSONArray from = new JSONArray();
        for (EdgeRuntimeState edge : incoming) {
            if ("chosen".equals(edge.status)) {
                from.add(JSONObject.of(
                        "sourceNodeId", edge.edge.source,
                        "branchKey", edge.branchKey,
                        "output", readPathValue(context, "nodes." + edge.edge.source + ".output")
                ));
            }
        }
        output.put("incoming", from);
        output.put("notifications", context.getJSONArray("notifications"));
        return output;
    }

    private JSONArray normalizeExtractionRules(Object raw) {
        JSONArray source = parseJsonArray(raw);
        JSONArray normalized = new JSONArray();
        for (int i = 0; i < source.size(); i++) {
            JSONObject item = source.getJSONObject(i);
            if (item == null) {
                continue;
            }

            String field = firstNonBlank(item.getString("field"), item.getString("fieldName"));
            String selector = item.getString("selector");
            if (!StringUtils.hasText(selector)) {
                continue;
            }

            JSONObject rule = new JSONObject();
            rule.put("field", StringUtils.hasText(field) ? field : "value_" + (i + 1));
            rule.put("selector", selector);

            String type = item.getString("type");
            String attr = item.getString("attr");
            String attribute = item.getString("attribute");
            if (!StringUtils.hasText(type) && StringUtils.hasText(attribute)) {
                if ("text".equalsIgnoreCase(attribute) || "html".equalsIgnoreCase(attribute)) {
                    type = attribute.toLowerCase();
                } else {
                    type = "attr";
                    attr = attribute;
                }
            }

            rule.put("type", StringUtils.hasText(type) ? type : "text");
            if (StringUtils.hasText(attr)) {
                rule.put("attr", attr);
            }
            normalized.add(rule);
        }
        return normalized;
    }

    private JSONObject createInitialContext(String rawInputConfig) {
        JSONObject context = new JSONObject();
        context.put("input", parseJsonObject(rawInputConfig));
        context.put("env", buildEnvironmentContext());
        context.put("nodes", new JSONObject());
        context.put("notifications", new JSONArray());
        return context;
    }

    private JSONObject buildEnvironmentContext() {
        JSONObject env = new JSONObject();
        env.put("RESEND_API_KEY", resendApiKey);
        env.put("RESEND_FROM_EMAIL", resendFromEmail);
        return env;
    }

    private String buildNodeInputSnapshot(JSONObject context, GraphNode node, List<EdgeRuntimeState> incoming) {
        JSONObject snapshot = new JSONObject();
        snapshot.put("input", context.get("input"));
        snapshot.put("nodes", context.get("nodes"));
        snapshot.put("currentNode", JSONObject.of(
                "id", node.id,
                "type", node.type,
                "label", node.label
        ));

        JSONArray incomingArray = new JSONArray();
        for (EdgeRuntimeState edge : incoming) {
            if (!"chosen".equals(edge.status)) {
                continue;
            }
            incomingArray.add(JSONObject.of(
                    "sourceNodeId", edge.edge.source,
                    "sourceHandle", edge.edge.sourceHandle,
                    "branchKey", edge.branchKey,
                    "output", readPathValue(context, "nodes." + edge.edge.source + ".output")
            ));
        }
        snapshot.put("incoming", incomingArray);
        return JSON.toJSONString(snapshot);
    }

    private void rememberNodeResult(JSONObject context, GraphNode node, String status, JSONObject output, String branchKey) {
        JSONObject nodes = context.getJSONObject("nodes");
        nodes.put(node.id, JSONObject.of(
                "type", node.type,
                "label", node.label,
                "status", status,
                "branchKey", branchKey,
                "output", output
        ));
    }

    private String resolveBranchKey(List<EdgeRuntimeState> incoming) {
        List<EdgeRuntimeState> chosen = incoming.stream()
                .filter(edge -> "chosen".equals(edge.status))
                .toList();
        if (chosen.size() > 1) {
            return "merged";
        }
        if (chosen.size() == 1 && StringUtils.hasText(chosen.get(0).branchKey)) {
            return chosen.get(0).branchKey;
        }
        return "main";
    }

    private String resolveEdgeBranchKey(GraphNode node, EdgeDef edge, String currentBranchKey) {
        if ("parallel_split".equals(node.type) || "condition".equals(node.type)) {
            return edge.sourceHandle;
        }
        if ("merge".equals(node.type)) {
            return "merged";
        }
        return StringUtils.hasText(currentBranchKey) ? currentBranchKey : "main";
    }

    private String formatResult(JSONObject context, GraphModel graphModel) {
        JSONObject result = new JSONObject();
        JSONArray endResults = new JSONArray();
        for (GraphNode node : graphModel.nodes.values()) {
            if ("end".equals(node.type)) {
                Object value = readPathValue(context, "nodes." + node.id + ".output");
                if (value != null) {
                    endResults.add(JSONObject.of(
                            "nodeId", node.id,
                            "label", node.label,
                            "output", value
                    ));
                }
            }
        }
        result.put("input", context.get("input"));
        result.put("nodes", context.get("nodes"));
        result.put("endResults", endResults);
        result.put("notifications", context.getJSONArray("notifications"));
        return JSON.toJSONString(result);
    }

    private BoundRobot resolveBoundRobot(RobotBindings bindings, GraphNode node) {
        if (bindings == null || node == null) {
            return null;
        }

        String role = switch (node.type) {
            case "web_crawl" -> "crawl";
            case "ai_filter" -> "analysis";
            case "http_request" -> "notification";
            default -> null;
        };
        if (!StringUtils.hasText(role)) {
            return null;
        }

        Long robotId = bindings.robotIdForRole(role);
        if (robotId == null) {
            return null;
        }

        Robot robot = robotRepository.findById(robotId)
                .orElseThrow(() -> new RuntimeException("Bound " + role + " robot not found: " + robotId));
        String expectedType = expectedRobotType(role);
        if (StringUtils.hasText(expectedType) && !expectedType.equals(robot.getType())) {
            throw new RuntimeException("Bound " + role + " robot type mismatch: expected " + expectedType + ", actual " + robot.getType());
        }
        if (!Set.of("online", "running").contains(robot.getStatus())) {
            throw new RuntimeException("Bound " + role + " robot is offline: " + robot.getName());
        }
        return new BoundRobot(robot.getId(), robot.getName(), robot.getType(), role);
    }

    private String expectedRobotType(String role) {
        return switch (role) {
            case "crawl" -> "data_collector";
            case "analysis" -> "report_generator";
            case "notification" -> "notification";
            default -> "";
        };
    }

    private String formatRobotLogSuffix(BoundRobot robot) {
        if (robot == null || !StringUtils.hasText(robot.name())) {
            return "";
        }
        return " via robot: " + robot.name() + " (" + robot.type() + ")";
    }

    private GraphModel parseGraph(String rawGraph) {
        try {
            JsonNode root = objectMapper.readTree(rawGraph);
            GraphModel model = new GraphModel();
            JsonNode rawRobotBindings = root.path("robotBindings");
            model.robotBindings = new RobotBindings(
                    asLong(rawRobotBindings.path("crawlRobotId")),
                    asLong(rawRobotBindings.path("analysisRobotId")),
                    asLong(rawRobotBindings.path("notificationRobotId"))
            );
            for (JsonNode node : root.path("nodes")) {
                GraphNode graphNode = new GraphNode(
                        node.path("id").asText(),
                        node.path("type").asText(),
                        node.path("label").asText(),
                        node.path("description").asText(),
                        JSON.parseObject(node.path("config").toString())
                );
                model.nodes.put(graphNode.id, graphNode);
            }
            for (JsonNode edge : root.path("edges")) {
                EdgeDef edgeDef = new EdgeDef(
                        edge.path("id").asText(),
                        edge.path("source").asText(),
                        edge.path("sourceHandle").asText("success"),
                        edge.path("target").asText(),
                        edge.path("targetHandle").asText("in")
                );
                model.edges.add(edgeDef);
                model.outgoingEdges.computeIfAbsent(edgeDef.source, key -> new ArrayList<>()).add(edgeDef);
                model.incomingEdges.computeIfAbsent(edgeDef.target, key -> new ArrayList<>()).add(edgeDef);
            }
            return model;
        } catch (Exception ex) {
            throw new RuntimeException("Invalid graph snapshot", ex);
        }
    }

    private Long asLong(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.asLong();
        }
        if (value.isTextual() && StringUtils.hasText(value.asText())) {
            try {
                return Long.parseLong(value.asText());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private JSONObject parseJsonObject(Object raw) {
        if (raw == null) {
            return new JSONObject();
        }
        if (raw instanceof JSONObject jsonObject) {
            return jsonObject;
        }
        try {
            return JSON.parseObject(String.valueOf(raw));
        } catch (Exception ex) {
            return new JSONObject();
        }
    }

    private JSONArray parseJsonArray(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof JSONArray jsonArray) {
            return jsonArray;
        }
        if (raw instanceof List<?> list) {
            return JSONArray.from(list);
        }
        try {
            return JSON.parseArray(String.valueOf(raw));
        } catch (Exception ex) {
            return null;
        }
    }

    private String resolveString(String value, JSONObject context) {
        return resolveString(value, context, "");
    }

    private String resolveString(String value, JSONObject context, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        if (value.startsWith("$.")) {
            Object resolved = readPathValue(context, value.substring(2));
            return resolved == null ? fallback : String.valueOf(resolved);
        }
        return renderTemplate(value, context);
    }

    private String renderTemplate(String template, JSONObject context) {
        if (!StringUtils.hasText(template)) {
            return template;
        }
        Matcher matcher = TEMPLATE_PATTERN.matcher(template);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            Object value = readPathValue(context, matcher.group(1));
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String renderJsonTemplate(String template, JSONObject context) {
        if (!StringUtils.hasText(template)) {
            return template;
        }

        Matcher quotedMatcher = QUOTED_TEMPLATE_PATTERN.matcher(template);
        StringBuffer quotedBuffer = new StringBuffer();
        while (quotedMatcher.find()) {
            Object value = readPathValue(context, quotedMatcher.group(1));
            quotedMatcher.appendReplacement(
                    quotedBuffer,
                    Matcher.quoteReplacement(value == null ? "null" : JSON.toJSONString(value))
            );
        }
        quotedMatcher.appendTail(quotedBuffer);

        Matcher matcher = TEMPLATE_PATTERN.matcher(quotedBuffer.toString());
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            Object value = readPathValue(context, matcher.group(1));
            matcher.appendReplacement(
                    buffer,
                    Matcher.quoteReplacement(value == null ? "null" : JSON.toJSONString(value))
            );
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private boolean isJsonRequest(HttpHeaders headers, String bodyTemplate) {
        MediaType contentType = headers.getContentType();
        String trimmed = bodyTemplate == null ? "" : bodyTemplate.trim();
        return contentType != null
                && MediaType.APPLICATION_JSON.isCompatibleWith(contentType)
                && (trimmed.startsWith("{") || trimmed.startsWith("["));
    }

    private Object readPathValue(Object source, String path) {
        if (source == null || !StringUtils.hasText(path)) {
            return null;
        }
        Object current = source instanceof JSONObject ? source : JSON.toJSON(source);
        for (String segment : path.split("\\.")) {
            if (current instanceof JSONObject jsonObject) {
                Object direct = jsonObject.get(segment);
                if (direct == null) {
                    direct = readLegacyCompatibleValue(jsonObject, segment);
                }
                current = direct;
            } else if (current instanceof Map<?, ?> map) {
                current = map.get(segment);
            } else if (current instanceof List<?> list) {
                try {
                    int index = Integer.parseInt(segment);
                    current = index >= 0 && index < list.size() ? list.get(index) : null;
                } catch (NumberFormatException ex) {
                    return null;
                }
            } else {
                return null;
            }
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private Object readLegacyCompatibleValue(JSONObject jsonObject, String segment) {
        if (!StringUtils.hasText(segment)) {
            return null;
        }
        if ("subject".equals(segment) || "title".equals(segment) || "body".equals(segment) || "content".equals(segment)) {
            JSONObject structured = jsonObject.getJSONObject("structured");
            if (structured != null) {
                if ("subject".equals(segment) || "title".equals(segment)) {
                    return structured.get("subject");
                }
                return structured.get("body");
            }
            if ("content".equals(segment)) {
                return firstNonBlank(jsonObject.getString("summaryText"), jsonObject.getString("title"));
            }
        }
        return null;
    }

    private boolean looksLikePlaceholderMailServiceUrl(String url) {
        return StringUtils.hasText(url) && url.contains("api.mail-service.com/send");
    }

    private boolean looksLikeTemplateValue(String value) {
        return StringUtils.hasText(value) && (value.contains("{{") || value.startsWith("$."));
    }

    private String extractStringValue(JSONObject jsonObject, String key) {
        if (jsonObject == null || !StringUtils.hasText(key)) {
            return null;
        }
        Object value = jsonObject.get(key);
        if (value instanceof JSONArray jsonArray) {
            if (jsonArray.isEmpty()) {
                return null;
            }
            Object first = jsonArray.get(0);
            return first == null ? null : String.valueOf(first);
        }
        return value == null ? null : String.valueOf(value);
    }

    private double toDouble(Object value) {
        if (value == null) {
            return 0d;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ex) {
            return 0d;
        }
    }

    private String defaultSuccessHandle(String nodeType) {
        return switch (nodeType) {
            case "start", "merge" -> "out";
            default -> "success";
        };
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Workflow execution interrupted");
        }
    }

    private interface ParentExecutionContext {
        String inputConfig();

        String graphSnapshot();

        WorkflowStepRun createStepRun(GraphNode node, String branchKey, String inputSnapshot, BoundRobot robot);

        void updateProgress(int progress);

        void complete(String result);

        void fail(String message);

        Long logTaskId();

        Long logTaskRunId();

        String logTaskCode();

        String logTaskName();
    }

    private record GraphNode(String id, String type, String label, String description, JSONObject config) {
    }

    private record EdgeDef(String id, String source, String sourceHandle, String target, String targetHandle) {
    }

    private static class EdgeRuntimeState {
        private final EdgeDef edge;
        private String status = "unresolved";
        private String branchKey;

        private EdgeRuntimeState(EdgeDef edge) {
            this.edge = edge;
        }
    }

    private static class GraphModel {
        private final Map<String, GraphNode> nodes = new LinkedHashMap<>();
        private final List<EdgeDef> edges = new ArrayList<>();
        private final Map<String, List<EdgeDef>> outgoingEdges = new HashMap<>();
        private final Map<String, List<EdgeDef>> incomingEdges = new HashMap<>();
        private RobotBindings robotBindings = new RobotBindings(null, null, null);
    }

    private record RobotBindings(Long crawlRobotId, Long analysisRobotId, Long notificationRobotId) {
        private Long robotIdForRole(String role) {
            return switch (role) {
                case "crawl" -> crawlRobotId;
                case "analysis" -> analysisRobotId;
                case "notification" -> notificationRobotId;
                default -> null;
            };
        }
    }

    private record BoundRobot(Long id, String name, String type, String role) {
    }

    private record NodeExecutionResult(JSONObject output, Set<String> chosenHandles) {
        private static NodeExecutionResult success(JSONObject output, Set<String> chosenHandles) {
            return new NodeExecutionResult(output, chosenHandles);
        }
    }
}
