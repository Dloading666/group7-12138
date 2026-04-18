package com.rpa.management.engine;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpa.management.client.AgentApiClient;
import com.rpa.management.client.SpiderApiClient;
import com.rpa.management.dto.CrawlResultDTO;
import com.rpa.management.entity.Task;
import com.rpa.management.entity.TaskRun;
import com.rpa.management.entity.WorkflowDebugRun;
import com.rpa.management.entity.WorkflowStepRun;
import com.rpa.management.service.CrawlResultService;
import com.rpa.management.service.ExecutionLogService;
import com.rpa.management.service.TaskRunService;
import com.rpa.management.service.WorkflowDebugRunService;
import com.rpa.management.service.WorkflowService;
import com.rpa.management.service.WorkflowStepRunService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final TaskRunService taskRunService;
    private final WorkflowDebugRunService workflowDebugRunService;
    private final WorkflowStepRunService workflowStepRunService;
    private final WorkflowService workflowService;
    private final ExecutionLogService executionLogService;
    private final SpiderApiClient spiderApiClient;
    private final AgentApiClient agentApiClient;
    private final CrawlResultService crawlResultService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

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
            public WorkflowStepRun createStepRun(GraphNode node, String branchKey, String inputSnapshot) {
                return workflowStepRunService.createTaskStepRun(
                        run.getId(),
                        node.id,
                        node.type,
                        node.label,
                        branchKey,
                        inputSnapshot
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
            public WorkflowStepRun createStepRun(GraphNode node, String branchKey, String inputSnapshot) {
                return workflowStepRunService.createDebugStepRun(
                        debugRun.getId(),
                        node.id,
                        node.type,
                        node.label,
                        branchKey,
                        inputSnapshot
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
                WorkflowStepRun skippedStep = parentContext.createStepRun(
                        node,
                        resolveBranchKey(incoming),
                        buildNodeInputSnapshot(context, node, incoming)
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
            WorkflowStepRun stepRun = parentContext.createStepRun(node, branchKey, inputSnapshot);
            workflowStepRunService.start(stepRun);
            executionLogService.info(
                    parentContext.logTaskId(),
                    parentContext.logTaskRunId(),
                    parentContext.logTaskCode(),
                    parentContext.logTaskName(),
                    null,
                    null,
                    "Execute workflow node: " + node.id + " / " + node.type
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
            body = renderTemplate(textBody, context);
        }
        HttpEntity<Object> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, method, requestEntity, String.class);

        JSONObject output = new JSONObject();
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
            crawlConfig.put("extractionRules", parseJsonArray(config.get("extractionRules")));
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

        String raw = agentApiClient.chatCompletion(List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ), model);

        String outputFormat = config.getString("outputFormat");
        Object structured = null;
        if ("json".equalsIgnoreCase(outputFormat)) {
            try {
                structured = JSON.parse(raw);
            } catch (Exception ignored) {
            }
        }

        JSONObject wrapped = new JSONObject();
        wrapped.put("model", model);
        wrapped.put("content", raw);
        wrapped.put("structured", structured);
        return NodeExecutionResult.success(wrapped, Set.of("success", "out"));
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

    private JSONObject createInitialContext(String rawInputConfig) {
        JSONObject context = new JSONObject();
        context.put("input", parseJsonObject(rawInputConfig));
        context.put("nodes", new JSONObject());
        context.put("notifications", new JSONArray());
        return context;
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

    private GraphModel parseGraph(String rawGraph) {
        try {
            JsonNode root = objectMapper.readTree(rawGraph);
            GraphModel model = new GraphModel();
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

    private Object readPathValue(Object source, String path) {
        if (source == null || !StringUtils.hasText(path)) {
            return null;
        }
        Object current = source instanceof JSONObject ? source : JSON.toJSON(source);
        for (String segment : path.split("\\.")) {
            if (current instanceof JSONObject jsonObject) {
                current = jsonObject.get(segment);
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

        WorkflowStepRun createStepRun(GraphNode node, String branchKey, String inputSnapshot);

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
    }

    private record NodeExecutionResult(JSONObject output, Set<String> chosenHandles) {
        private static NodeExecutionResult success(JSONObject output, Set<String> chosenHandles) {
            return new NodeExecutionResult(output, chosenHandles);
        }
    }
}
