package com.rpa.management.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rpa.management.dto.WorkflowDTO;
import com.rpa.management.dto.WorkflowVersionDTO;
import com.rpa.management.entity.Workflow;
import com.rpa.management.entity.WorkflowNode;
import com.rpa.management.entity.WorkflowVersion;
import com.rpa.management.repository.WorkflowNodeRepository;
import com.rpa.management.repository.WorkflowRepository;
import com.rpa.management.repository.WorkflowVersionRepository;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private static final Set<String> EXECUTABLE_NODE_TYPES = Set.of(
            "start",
            "end",
            "http_request",
            "web_crawl",
            "ai_filter",
            "condition",
            "parallel_split",
            "merge",
            "transform",
            "notification"
    );

    private final WorkflowRepository workflowRepository;
    private final WorkflowNodeRepository workflowNodeRepository;
    private final WorkflowVersionRepository workflowVersionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public WorkflowDTO createWorkflow(WorkflowDTO dto, Long userId, String userName) {
        Workflow workflow = new Workflow();
        String workflowCode = dto.getWorkflowCode();
        if (!StringUtils.hasText(workflowCode)) {
            workflowCode = generateWorkflowCode();
        } else if (workflowRepository.findByWorkflowCode(workflowCode).isPresent()) {
            throw new RuntimeException("Workflow code already exists: " + workflowCode);
        }

        workflow.setWorkflowCode(workflowCode);
        workflow.setName(dto.getName());
        workflow.setDescription(dto.getDescription());
        workflow.setCategory(dto.getCategory());
        workflow.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "draft");
        workflow.setVersion(dto.getVersion() != null ? dto.getVersion() : 1);
        workflow.setUserId(userId);
        workflow.setUserName(userName);
        applyDefinition(workflow, dto);
        workflow = workflowRepository.save(workflow);
        return toDTO(workflow);
    }

    @Transactional
    public WorkflowDTO updateWorkflow(Long id, WorkflowDTO dto) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found: " + id));

        workflow.setName(dto.getName());
        workflow.setDescription(dto.getDescription());
        workflow.setCategory(dto.getCategory());
        if (StringUtils.hasText(dto.getStatus()) && !"published".equals(workflow.getStatus())) {
            workflow.setStatus(dto.getStatus());
        }
        applyDefinition(workflow, dto);
        workflow = workflowRepository.save(workflow);
        return toDTO(workflow);
    }

    @Transactional
    public WorkflowDTO publishWorkflow(Long id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found: " + id));

        String graph = normalizeGraph(resolveGraph(workflow));
        validateExecutableGraph(graph);

        int versionNumber = resolveNextVersionNumber(workflow);
        WorkflowVersion version = new WorkflowVersion();
        version.setWorkflowId(workflow.getId());
        version.setVersionNumber(versionNumber);
        version.setWorkflowCode(workflow.getWorkflowCode());
        version.setName(workflow.getName());
        version.setDescription(workflow.getDescription());
        version.setCategory(workflow.getCategory());
        version.setPublishStatus("published");
        version.setUserId(workflow.getUserId());
        version.setUserName(workflow.getUserName());
        version.setInputSchema(normalizeInputSchema(workflow.getInputSchema(), graph));
        version.setGraph(graph);
        version = workflowVersionRepository.save(version);

        workflow.setStatus("published");
        workflow.setPublishTime(LocalDateTime.now());
        workflow.setVersion(versionNumber);
        workflow.setLatestVersionId(version.getId());
        workflow.setGraph(graph);
        workflow.setConfig(graph);
        workflow.setInputSchema(version.getInputSchema());
        workflowRepository.save(workflow);

        return toDTO(workflow);
    }

    @Transactional
    public void deleteWorkflow(Long id) {
        workflowNodeRepository.deleteByWorkflowId(id);
        workflowRepository.deleteById(id);
    }

    public WorkflowDTO getWorkflowById(Long id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found: " + id));
        WorkflowDTO dto = toDTO(workflow);
        List<WorkflowNode> nodes = workflowNodeRepository.findByWorkflowIdOrderByOrderAsc(id);
        if (!nodes.isEmpty()) {
            dto.setNodes(nodes.stream().map(this::toLegacyNodeDTO).collect(Collectors.toList()));
        }
        return dto;
    }

    public WorkflowVersionDTO getWorkflowVersionById(Long id) {
        WorkflowVersion version = workflowVersionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow version not found: " + id));
        version.setGraph(normalizeGraph(version.getGraph()));
        return toVersionDTO(version);
    }

    public List<WorkflowVersionDTO> getPublishedWorkflowVersions() {
        return workflowVersionRepository.findPublishedVersionsForActiveWorkflows("published")
                .stream()
                .map(version -> {
                    version.setGraph(normalizeGraph(version.getGraph()));
                    return toVersionDTO(version);
                })
                .collect(Collectors.toList());
    }

    public Page<WorkflowDTO> getWorkflowsByPage(String name, String status, Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createTime"));
        return workflowRepository.findByConditions(emptyToNull(name), emptyToNull(status), userId, pageable)
                .map(this::toDTO);
    }

    public List<WorkflowDTO> getAllWorkflows() {
        return workflowRepository.findAll(Sort.by(Sort.Direction.DESC, "createTime"))
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public String normalizeGraph(String rawGraph) {
        try {
            ObjectNode root = readGraphObject(rawGraph);
            ArrayNode normalizedNodes = objectMapper.createArrayNode();
            ArrayNode normalizedEdges = objectMapper.createArrayNode();
            ObjectNode robotBindings = normalizeRobotBindings(root.path("robotBindings"));

            JsonNode rawNodes = root.path("nodes");
            if (!rawNodes.isArray() || rawNodes.isEmpty()) {
                return defaultGraph(robotBindings);
            }

            Map<String, ObjectNode> nodeMap = new LinkedHashMap<>();
            int index = 0;
            for (JsonNode rawNode : rawNodes) {
                ObjectNode normalizedNode = objectMapper.createObjectNode();
                String id = textOrDefault(rawNode.path("id").asText(), "node_" + (index + 1));
                String type = normalizeNodeType(rawNode.path("type").asText());
                String label = textOrDefault(rawNode.path("label").asText(), rawNode.path("name").asText());
                ObjectNode position = objectMapper.createObjectNode();
                JsonNode rawPosition = rawNode.path("position");
                position.put("x", rawPosition.path("x").isNumber() ? rawPosition.path("x").asInt() : 140 + (index * 260));
                position.put("y", rawPosition.path("y").isNumber() ? rawPosition.path("y").asInt() : 160);

                normalizedNode.put("id", id);
                normalizedNode.put("type", StringUtils.hasText(type) ? type : "transform");
                normalizedNode.put("label", StringUtils.hasText(label) ? label : id);
                normalizedNode.put("description", rawNode.path("description").asText(""));
                normalizedNode.set("position", position);
                normalizedNode.set("config", rawNode.path("config").isObject() ? rawNode.path("config") : objectMapper.createObjectNode());
                normalizedNodes.add(normalizedNode);
                nodeMap.put(id, normalizedNode);
                index += 1;
            }

            JsonNode rawEdges = root.path("edges");
            if (rawEdges.isArray() && !rawEdges.isEmpty()) {
                int edgeIndex = 0;
                Map<String, Integer> sourceEdgeCounters = new HashMap<>();
                for (JsonNode rawEdge : rawEdges) {
                    String source = rawEdge.path("source").asText();
                    String target = rawEdge.path("target").asText();
                    if (!nodeMap.containsKey(source) || !nodeMap.containsKey(target)) {
                        continue;
                    }

                    ObjectNode edge = objectMapper.createObjectNode();
                    edge.put("id", textOrDefault(rawEdge.path("id").asText(), "edge_" + (++edgeIndex)));
                    edge.put("source", source);
                    edge.put("target", target);
                    int sourceEdgeIndex = sourceEdgeCounters.merge(source, 1, Integer::sum);
                    edge.put("sourceHandle", resolveSourceHandle(nodeMap.get(source), rawEdge.path("sourceHandle").asText(), sourceEdgeIndex));
                    edge.put("targetHandle", resolveTargetHandle(rawEdge.path("targetHandle").asText()));
                    normalizedEdges.add(edge);
                }
            }

            if (normalizedEdges.isEmpty()) {
                for (int i = 1; i < normalizedNodes.size(); i += 1) {
                    ObjectNode sourceNode = (ObjectNode) normalizedNodes.get(i - 1);
                    ObjectNode targetNode = (ObjectNode) normalizedNodes.get(i);
                    ObjectNode edge = objectMapper.createObjectNode();
                    edge.put("id", "edge_" + sourceNode.path("id").asText() + "_" + targetNode.path("id").asText());
                    edge.put("source", sourceNode.path("id").asText());
                    edge.put("target", targetNode.path("id").asText());
                    edge.put("sourceHandle", resolveSourceHandle(sourceNode, "", i));
                    edge.put("targetHandle", "in");
                    normalizedEdges.add(edge);
                }
            }

            ObjectNode normalized = objectMapper.createObjectNode();
            normalized.put("version", 2);
            normalized.set("robotBindings", robotBindings);
            normalized.set("nodes", normalizedNodes);
            normalized.set("edges", normalizedEdges);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(normalized);
        } catch (Exception ex) {
            log.warn("Failed to normalize workflow graph", ex);
            return defaultGraph(emptyRobotBindings());
        }
    }

    public void validateExecutableGraph(String graph) {
        if (!StringUtils.hasText(graph)) {
            throw new RuntimeException("Workflow graph is empty");
        }
        try {
            JsonNode root = objectMapper.readTree(graph);
            JsonNode nodes = root.path("nodes");
            JsonNode edges = root.path("edges");
            if (!nodes.isArray() || nodes.isEmpty()) {
                throw new RuntimeException("Workflow graph has no executable nodes");
            }

            Map<String, JsonNode> nodeMap = new LinkedHashMap<>();
            Map<String, Integer> inDegree = new HashMap<>();
            Map<String, Integer> outDegree = new HashMap<>();
            int startCount = 0;
            int endCount = 0;

            for (JsonNode node : nodes) {
                String id = node.path("id").asText();
                String type = normalizeNodeType(node.path("type").asText());
                if (!StringUtils.hasText(id)) {
                    throw new RuntimeException("Node id is required");
                }
                if (nodeMap.containsKey(id)) {
                    throw new RuntimeException("Duplicate node id: " + id);
                }
                if (!EXECUTABLE_NODE_TYPES.contains(type)) {
                    throw new RuntimeException("Unsupported executable node type: " + type);
                }
                nodeMap.put(id, node);
                inDegree.put(id, 0);
                outDegree.put(id, 0);
                if ("start".equals(type)) {
                    startCount += 1;
                }
                if ("end".equals(type)) {
                    endCount += 1;
                }
            }

            if (startCount != 1) {
                throw new RuntimeException("Workflow must contain exactly one start node");
            }
            if (endCount < 1) {
                throw new RuntimeException("Workflow must contain at least one end node");
            }
            if (!edges.isArray() || edges.isEmpty()) {
                throw new RuntimeException("Workflow graph has no edges");
            }

            Map<String, Set<String>> outgoingHandleMap = new HashMap<>();
            Map<String, List<String>> adjacency = new HashMap<>();
            for (JsonNode edge : edges) {
                String source = edge.path("source").asText();
                String target = edge.path("target").asText();
                String sourceHandle = edge.path("sourceHandle").asText();
                String targetHandle = edge.path("targetHandle").asText("in");
                if (!nodeMap.containsKey(source) || !nodeMap.containsKey(target)) {
                    throw new RuntimeException("Edge references unknown node");
                }
                if (!"in".equals(targetHandle)) {
                    throw new RuntimeException("Target handle must be in");
                }
                validateSourceHandle(nodeMap.get(source), sourceHandle);

                inDegree.put(target, inDegree.getOrDefault(target, 0) + 1);
                outDegree.put(source, outDegree.getOrDefault(source, 0) + 1);
                outgoingHandleMap.computeIfAbsent(source, key -> new LinkedHashSet<>()).add(sourceHandle);
                adjacency.computeIfAbsent(source, key -> new ArrayList<>()).add(target);
            }

            for (Map.Entry<String, JsonNode> entry : nodeMap.entrySet()) {
                String nodeId = entry.getKey();
                JsonNode node = entry.getValue();
                String type = normalizeNodeType(node.path("type").asText());
                int incoming = inDegree.getOrDefault(nodeId, 0);
                int outgoing = outDegree.getOrDefault(nodeId, 0);

                if ("start".equals(type) && incoming > 0) {
                    throw new RuntimeException("Start node cannot have incoming edges");
                }
                if ("end".equals(type) && outgoing > 0) {
                    throw new RuntimeException("End node cannot have outgoing edges");
                }
                if (!"start".equals(type) && incoming == 0) {
                    throw new RuntimeException("Found isolated node without incoming edges: " + nodeId);
                }
                if (!"end".equals(type) && outgoing == 0) {
                    throw new RuntimeException("Found isolated node without outgoing edges: " + nodeId);
                }
                if ("merge".equals(type) && incoming < 2) {
                    throw new RuntimeException("Merge node must have at least two incoming edges");
                }
                if (!"merge".equals(type) && incoming > 1) {
                    throw new RuntimeException("Only merge nodes can have multiple incoming edges");
                }
                if ("parallel_split".equals(type) && outgoing < 2) {
                    throw new RuntimeException("parallel_split must have at least two outgoing edges");
                }
            }

            validateConditionHandles(nodeMap, outgoingHandleMap);
            ensureDag(nodeMap, adjacency, inDegree);
            validateParallelClosures(nodeMap, edges, adjacency);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Invalid workflow graph format");
        }
    }

    private void applyDefinition(Workflow workflow, WorkflowDTO dto) {
        String normalizedGraph = normalizeGraph(firstNonBlank(dto.getGraph(), dto.getConfig()));
        RobotBindingsSnapshot requestedBindings = RobotBindingsSnapshot.from(dto);
        if (requestedBindings.hasAny()) {
            normalizedGraph = applyRobotBindings(normalizedGraph, requestedBindings);
        }
        workflow.setInputSchema(normalizeInputSchema(dto.getInputSchema(), normalizedGraph));
        workflow.setGraph(normalizedGraph);
        workflow.setConfig(normalizedGraph);
    }

    private int resolveNextVersionNumber(Workflow workflow) {
        if (workflow.getLatestVersionId() == null) {
            return 1;
        }
        return (workflow.getVersion() != null ? workflow.getVersion() : 0) + 1;
    }

    private String resolveGraph(Workflow workflow) {
        return firstNonBlank(workflow.getGraph(), workflow.getConfig());
    }

    private void validateSourceHandle(JsonNode sourceNode, String sourceHandle) {
        String type = normalizeNodeType(sourceNode.path("type").asText());
        String normalizedHandle = textOrDefault(sourceHandle, defaultSourceHandle(type));
        switch (type) {
            case "start" -> requireHandle(type, normalizedHandle, Set.of("out"));
            case "condition" -> requireHandle(type, normalizedHandle, Set.of("true", "false", "error"));
            case "parallel_split" -> {
                if (!StringUtils.hasText(normalizedHandle)) {
                    throw new RuntimeException("parallel_split handle cannot be empty");
                }
            }
            case "merge" -> requireHandle(type, normalizedHandle, Set.of("out"));
            case "end" -> throw new RuntimeException("End node cannot have outgoing edges");
            default -> requireHandle(type, normalizedHandle, Set.of("success", "error", "out"));
        }
    }

    private void validateConditionHandles(Map<String, JsonNode> nodeMap, Map<String, Set<String>> outgoingHandleMap) {
        for (Map.Entry<String, JsonNode> entry : nodeMap.entrySet()) {
            String type = normalizeNodeType(entry.getValue().path("type").asText());
            if ("condition".equals(type)) {
                Set<String> handles = outgoingHandleMap.getOrDefault(entry.getKey(), Set.of());
                if (!handles.contains("true") && !handles.contains("false")) {
                    throw new RuntimeException("Condition node must provide true or false branch");
                }
            }
        }
    }

    private void ensureDag(Map<String, JsonNode> nodeMap, Map<String, List<String>> adjacency, Map<String, Integer> inDegree) {
        Map<String, Integer> degreeCopy = new HashMap<>(inDegree);
        ArrayDeque<String> queue = new ArrayDeque<>();
        degreeCopy.forEach((nodeId, degree) -> {
            if (degree == 0) {
                queue.add(nodeId);
            }
        });

        int visited = 0;
        while (!queue.isEmpty()) {
            String nodeId = queue.removeFirst();
            visited += 1;
            for (String target : adjacency.getOrDefault(nodeId, List.of())) {
                int nextDegree = degreeCopy.getOrDefault(target, 0) - 1;
                degreeCopy.put(target, nextDegree);
                if (nextDegree == 0) {
                    queue.add(target);
                }
            }
        }

        if (visited != nodeMap.size()) {
            throw new RuntimeException("Workflow graph must be a DAG");
        }
    }

    private void validateParallelClosures(Map<String, JsonNode> nodeMap,
                                          JsonNode edges,
                                          Map<String, List<String>> adjacency) {
        Map<String, List<JsonNode>> outgoingEdges = new HashMap<>();
        for (JsonNode edge : edges) {
            outgoingEdges.computeIfAbsent(edge.path("source").asText(), key -> new ArrayList<>()).add(edge);
        }

        for (Map.Entry<String, JsonNode> entry : nodeMap.entrySet()) {
            String nodeId = entry.getKey();
            String nodeType = normalizeNodeType(entry.getValue().path("type").asText());
            if (!"parallel_split".equals(nodeType)) {
                continue;
            }

            List<JsonNode> splitEdges = outgoingEdges.getOrDefault(nodeId, List.of());
            if (splitEdges.size() < 2) {
                continue;
            }

            Set<String> commonMergeTargets = null;
            for (JsonNode edge : splitEdges) {
                String sourceHandle = edge.path("sourceHandle").asText();
                if ("error".equals(sourceHandle)) {
                    continue;
                }
                Set<String> reachableMerges = findReachableMergeNodes(
                        edge.path("target").asText(),
                        nodeMap,
                        adjacency
                );
                if (commonMergeTargets == null) {
                    commonMergeTargets = new LinkedHashSet<>(reachableMerges);
                } else {
                    commonMergeTargets.retainAll(reachableMerges);
                }
            }

            if (commonMergeTargets == null || commonMergeTargets.isEmpty()) {
                throw new RuntimeException("parallel_split must close into a downstream merge node: " + nodeId);
            }
        }
    }

    private Set<String> findReachableMergeNodes(String startNodeId,
                                                Map<String, JsonNode> nodeMap,
                                                Map<String, List<String>> adjacency) {
        Set<String> visited = new LinkedHashSet<>();
        Set<String> merges = new LinkedHashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(startNodeId);

        while (!queue.isEmpty()) {
            String nodeId = queue.removeFirst();
            if (!visited.add(nodeId)) {
                continue;
            }
            JsonNode node = nodeMap.get(nodeId);
            if (node == null) {
                continue;
            }
            String nodeType = normalizeNodeType(node.path("type").asText());
            if ("merge".equals(nodeType)) {
                merges.add(nodeId);
                continue;
            }
            for (String next : adjacency.getOrDefault(nodeId, List.of())) {
                if (!visited.contains(next)) {
                    queue.add(next);
                }
            }
        }
        return merges;
    }

    private void requireHandle(String nodeType, String handle, Set<String> allowed) {
        if (!allowed.contains(handle)) {
            throw new RuntimeException("Invalid handle " + handle + " for node type " + nodeType);
        }
    }

    private ObjectNode readGraphObject(String rawGraph) {
        try {
            if (!StringUtils.hasText(rawGraph)) {
                return (ObjectNode) objectMapper.readTree(defaultGraph(emptyRobotBindings()));
            }
            JsonNode parsed = objectMapper.readTree(rawGraph);
            if (parsed instanceof ObjectNode objectNode) {
                return objectNode;
            }
            return (ObjectNode) objectMapper.readTree(defaultGraph(emptyRobotBindings()));
        } catch (Exception ex) {
            throw new RuntimeException("Invalid workflow graph payload", ex);
        }
    }

    private String resolveSourceHandle(ObjectNode sourceNode, String rawHandle, int edgeIndex) {
        String type = normalizeNodeType(sourceNode.path("type").asText());
        if (StringUtils.hasText(rawHandle)) {
            return rawHandle;
        }
        if ("condition".equals(type)) {
            return edgeIndex == 1 ? "true" : "false";
        }
        return defaultSourceHandle(type);
    }

    private String defaultSourceHandle(String type) {
        return switch (normalizeNodeType(type)) {
            case "start" -> "out";
            case "merge" -> "out";
            case "condition" -> "true";
            default -> "success";
        };
    }

    private String resolveTargetHandle(String targetHandle) {
        return StringUtils.hasText(targetHandle) ? targetHandle : "in";
    }

    private String normalizeInputSchema(String inputSchema, String graph) {
        ObjectNode schema = defaultInputSchemaObject();
        if (StringUtils.hasText(inputSchema)) {
            try {
                JsonNode parsed = objectMapper.readTree(inputSchema);
                if (parsed instanceof ObjectNode objectNode) {
                    schema = objectNode.deepCopy();
                }
            } catch (Exception ex) {
                schema = defaultInputSchemaObject();
            }
        }

        schema.put("type", "object");
        ObjectNode properties = schema.path("properties") instanceof ObjectNode objectNode
                ? objectNode
                : objectMapper.createObjectNode();
        schema.set("properties", properties);
        ArrayNode required = schema.path("required") instanceof ArrayNode arrayNode
                ? arrayNode
                : objectMapper.createArrayNode();
        schema.set("required", required);

        if (graphRequiresRecipientInput(graph) && !properties.has("to_email")) {
            ObjectNode toEmail = objectMapper.createObjectNode();
            toEmail.put("type", "string");
            toEmail.put("title", "目标发送邮箱");
            toEmail.put("description", "收件人邮箱");
            toEmail.put("format", "email");
            properties.set("to_email", toEmail);
        }

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema);
        } catch (Exception ex) {
            return """
                    {
                      "type": "object",
                      "properties": {},
                      "required": []
                    }
                    """.trim();
        }
    }

    private String normalizeNodeType(String type) {
        if (!StringUtils.hasText(type)) {
            return "";
        }
        return switch (type.trim().toLowerCase()) {
            case "http", "http_api" -> "http_request";
            case "web_crawl", "login_crawl", "pagination_crawl", "crawl", "spider" -> "web_crawl";
            case "qq_email", "qq_email_notice", "email" -> "notification";
            case "extract", "structured_extract", "field_transform", "data_process" -> "transform";
            case "task_result", "task_result_read", "ai_workflow", "workflow" -> "ai_filter";
            default -> type.trim().toLowerCase();
        };
    }

    private ObjectNode defaultInputSchemaObject() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.set("properties", objectMapper.createObjectNode());
        schema.set("required", objectMapper.createArrayNode());
        return schema;
    }

    private boolean graphRequiresRecipientInput(String graph) {
        if (!StringUtils.hasText(graph)) {
            return false;
        }
        try {
            JsonNode root = objectMapper.readTree(graph);
            JsonNode nodes = root.path("nodes");
            if (!nodes.isArray()) {
                return false;
            }
            for (JsonNode node : nodes) {
                if (!"http_request".equals(normalizeNodeType(node.path("type").asText()))) {
                    continue;
                }
                JsonNode config = node.path("config");
                if (looksLikeResendEmailNode(config) || looksLikeLegacyEmailNode(config)) {
                    return true;
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to infer recipient input from workflow graph", ex);
        }
        return false;
    }

    private boolean looksLikeResendEmailNode(JsonNode config) {
        return config != null
                && config.isObject()
                && "resend_email".equalsIgnoreCase(config.path("provider").asText());
    }

    private boolean looksLikeLegacyEmailNode(JsonNode config) {
        if (config == null || !config.isObject()) {
            return false;
        }
        String url = config.path("url").asText("");
        if (StringUtils.hasText(url) && (url.contains("api.resend.com/emails") || url.contains("api.mail-service.com/send"))) {
            return true;
        }

        String body = config.path("body").asText("");
        if (StringUtils.hasText(body)
                && body.contains("\"subject\"")
                && (body.contains("\"text\"") || body.contains("\"html\"") || body.contains("\"content\"") || body.contains("\"body\""))) {
            return true;
        }

        JsonNode headers = config.path("headers");
        JsonNode authorization = headers.path("Authorization");
        if (authorization.isMissingNode() || authorization.isNull()) {
            authorization = headers.path("authorization");
        }
        if (authorization.isTextual() && authorization.asText().contains("RESEND_API_KEY")) {
            return true;
        }

        return (config.hasNonNull("to") || body.contains("\"to\""))
                && (config.hasNonNull("subjectTemplate") || config.hasNonNull("subject") || body.contains("\"subject\""))
                && (config.hasNonNull("textTemplate")
                || config.hasNonNull("bodyTemplate")
                || config.hasNonNull("text")
                || config.hasNonNull("body")
                || config.hasNonNull("html"));
    }

    private String defaultGraph(ObjectNode robotBindings) {
        return """
                {
                  "version": 2,
                  "robotBindings": %s,
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
                }
                """.formatted(robotBindings.toPrettyString()).trim();
    }

    private com.rpa.management.dto.WorkflowNodeDTO toLegacyNodeDTO(WorkflowNode node) {
        com.rpa.management.dto.WorkflowNodeDTO dto = new com.rpa.management.dto.WorkflowNodeDTO();
        dto.setId(node.getId());
        dto.setWorkflowId(node.getWorkflowId());
        dto.setNodeTypeId(node.getNodeTypeId());
        dto.setNodeType(node.getNodeType());
        dto.setName(node.getName());
        dto.setDescription(node.getDescription());
        dto.setX(node.getX());
        dto.setY(node.getY());
        dto.setConfig(node.getConfig());
        dto.setTimeout(node.getTimeout());
        dto.setRetryCount(node.getRetryCount());
        dto.setOrder(node.getOrder());
        dto.setCreateTime(node.getCreateTime());
        dto.setUpdateTime(node.getUpdateTime());
        return dto;
    }

    public WorkflowDTO toDTO(Workflow workflow) {
        WorkflowDTO dto = new WorkflowDTO();
        dto.setId(workflow.getId());
        dto.setWorkflowCode(workflow.getWorkflowCode());
        dto.setName(workflow.getName());
        dto.setDescription(workflow.getDescription());
        dto.setCategory(workflow.getCategory());
        dto.setStatus(workflow.getStatus());
        dto.setVersion(workflow.getVersion());
        dto.setUserId(workflow.getUserId());
        dto.setUserName(workflow.getUserName());
        dto.setPublishTime(workflow.getPublishTime());
        dto.setConfig(normalizeGraph(workflow.getConfig()));
        dto.setGraph(normalizeGraph(firstNonBlank(workflow.getGraph(), workflow.getConfig())));
        dto.setInputSchema(normalizeInputSchema(workflow.getInputSchema(), dto.getGraph()));
        RobotBindingsSnapshot bindings = extractRobotBindings(dto.getGraph());
        dto.setCrawlRobotId(bindings.crawlRobotId());
        dto.setAnalysisRobotId(bindings.analysisRobotId());
        dto.setNotificationRobotId(bindings.notificationRobotId());
        dto.setLatestVersionId(workflow.getLatestVersionId());
        dto.setCreateTime(workflow.getCreateTime());
        dto.setUpdateTime(workflow.getUpdateTime());
        dto.setStepCount(calculateStepCount(dto.getGraph()));
        return dto;
    }

    public WorkflowVersionDTO toVersionDTO(WorkflowVersion version) {
        String normalizedGraph = normalizeGraph(version.getGraph());
        RobotBindingsSnapshot bindings = extractRobotBindings(normalizedGraph);
        return WorkflowVersionDTO.builder()
                .id(version.getId())
                .workflowId(version.getWorkflowId())
                .versionNumber(version.getVersionNumber())
                .workflowCode(version.getWorkflowCode())
                .name(version.getName())
                .description(version.getDescription())
                .category(version.getCategory())
                .publishStatus(version.getPublishStatus())
                .userId(version.getUserId())
                .userName(version.getUserName())
                .publishTime(version.getPublishTime())
                .inputSchema(normalizeInputSchema(version.getInputSchema(), normalizedGraph))
                .graph(normalizedGraph)
                .crawlRobotId(bindings.crawlRobotId())
                .analysisRobotId(bindings.analysisRobotId())
                .notificationRobotId(bindings.notificationRobotId())
                .build();
    }

    private String applyRobotBindings(String normalizedGraph, RobotBindingsSnapshot bindings) {
        try {
            ObjectNode root = readGraphObject(normalizedGraph);
            root.set("robotBindings", bindings.toObjectNode(objectMapper));
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception ex) {
            log.warn("Failed to apply robot bindings to workflow graph", ex);
            return normalizedGraph;
        }
    }

    private RobotBindingsSnapshot extractRobotBindings(String graph) {
        try {
            return RobotBindingsSnapshot.from(readGraphObject(graph).path("robotBindings"));
        } catch (Exception ex) {
            return RobotBindingsSnapshot.empty();
        }
    }

    private ObjectNode normalizeRobotBindings(JsonNode rawBindings) {
        return RobotBindingsSnapshot.from(rawBindings).toObjectNode(objectMapper);
    }

    private ObjectNode emptyRobotBindings() {
        return RobotBindingsSnapshot.empty().toObjectNode(objectMapper);
    }

    private Integer calculateStepCount(String graph) {
        if (!StringUtils.hasText(graph)) {
            return 0;
        }
        try {
            JsonNode root = objectMapper.readTree(graph);
            JsonNode nodes = root.path("nodes");
            if (!nodes.isArray()) {
                return 0;
            }
            int count = 0;
            Iterator<JsonNode> iterator = nodes.iterator();
            while (iterator.hasNext()) {
                JsonNode node = iterator.next();
                String type = normalizeNodeType(node.path("type").asText());
                if (!"start".equals(type) && !"end".equals(type)) {
                    count += 1;
                }
            }
            return count;
        } catch (Exception ex) {
            log.warn("Failed to parse workflow graph step count", ex);
            return 0;
        }
    }

    private String generateWorkflowCode() {
        return "WF" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String textOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private record RobotBindingsSnapshot(Long crawlRobotId, Long analysisRobotId, Long notificationRobotId) {
        private static RobotBindingsSnapshot empty() {
            return new RobotBindingsSnapshot(null, null, null);
        }

        private static RobotBindingsSnapshot from(WorkflowDTO dto) {
            return new RobotBindingsSnapshot(dto.getCrawlRobotId(), dto.getAnalysisRobotId(), dto.getNotificationRobotId());
        }

        private static RobotBindingsSnapshot from(JsonNode rawBindings) {
            if (rawBindings == null || !rawBindings.isObject()) {
                return empty();
            }
            return new RobotBindingsSnapshot(
                    asLong(rawBindings.get("crawlRobotId")),
                    asLong(rawBindings.get("analysisRobotId")),
                    asLong(rawBindings.get("notificationRobotId"))
            );
        }

        private static Long asLong(JsonNode value) {
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

        private boolean hasAny() {
            return crawlRobotId != null || analysisRobotId != null || notificationRobotId != null;
        }

        private ObjectNode toObjectNode(ObjectMapper objectMapper) {
            ObjectNode node = objectMapper.createObjectNode();
            if (crawlRobotId != null) {
                node.put("crawlRobotId", crawlRobotId);
            } else {
                node.putNull("crawlRobotId");
            }
            if (analysisRobotId != null) {
                node.put("analysisRobotId", analysisRobotId);
            } else {
                node.putNull("analysisRobotId");
            }
            if (notificationRobotId != null) {
                node.put("notificationRobotId", notificationRobotId);
            } else {
                node.putNull("notificationRobotId");
            }
            return node;
        }
    }
}
