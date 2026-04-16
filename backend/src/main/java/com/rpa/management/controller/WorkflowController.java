package com.rpa.management.controller;

import com.alibaba.fastjson2.JSONObject;
import com.rpa.management.client.AgentApiClient;
import com.rpa.management.dto.ApiResponse;
import com.rpa.management.dto.NodeTypeDTO;
import com.rpa.management.dto.WorkflowDTO;
import com.rpa.management.entity.Task;
import com.rpa.management.repository.TaskRepository;
import com.rpa.management.service.NodeTypeService;
import com.rpa.management.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 流程管理控制器
 */
@Slf4j
@Tag(name = "流程管理", description = "流程和节点类型的增删改查接口")
@RestController
@RequestMapping("/workflows")
@RequiredArgsConstructor
public class WorkflowController {
    
    private final WorkflowService workflowService;
    private final NodeTypeService nodeTypeService;
    private final AgentApiClient agentApiClient;
    private final TaskRepository taskRepository;
    
    // ==================== 流程接口 ====================
    
    /**
     * 创建流程
     */
    @Operation(summary = "创建流程", description = "创建新的流程")
    @PostMapping
    public ApiResponse<WorkflowDTO> createWorkflow(@Valid @RequestBody WorkflowDTO dto, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            String userName = (String) request.getAttribute("username");
            
            WorkflowDTO workflow = workflowService.createWorkflow(dto, userId, userName);
            return ApiResponse.success("创建流程成功", workflow);
        } catch (Exception e) {
            log.error("创建流程失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 更新流程
     */
    @Operation(summary = "更新流程", description = "更新流程信息")
    @PutMapping("/{id}")
    public ApiResponse<WorkflowDTO> updateWorkflow(@PathVariable Long id, @Valid @RequestBody WorkflowDTO dto) {
        try {
            WorkflowDTO workflow = workflowService.updateWorkflow(id, dto);
            return ApiResponse.success("更新流程成功", workflow);
        } catch (Exception e) {
            log.error("更新流程失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 发布流程
     */
    @Operation(summary = "发布流程", description = "发布流程")
    @PostMapping("/{id}/publish")
    public ApiResponse<WorkflowDTO> publishWorkflow(@PathVariable Long id) {
        try {
            WorkflowDTO workflow = workflowService.publishWorkflow(id);
            return ApiResponse.success("流程发布成功", workflow);
        } catch (Exception e) {
            log.error("发布流程失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 删除流程
     */
    @Operation(summary = "删除流程", description = "删除流程")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteWorkflow(@PathVariable Long id) {
        try {
            workflowService.deleteWorkflow(id);
            return ApiResponse.success("删除流程成功", null);
        } catch (Exception e) {
            log.error("删除流程失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 获取流程详情
     */
    @Operation(summary = "获取流程详情", description = "根据ID获取流程详细信息")
    @GetMapping("/{id}")
    public ApiResponse<WorkflowDTO> getWorkflowById(@PathVariable Long id) {
        try {
            WorkflowDTO workflow = workflowService.getWorkflowById(id);
            return ApiResponse.success(workflow);
        } catch (Exception e) {
            log.error("获取流程失败: {}", e.getMessage());
            return ApiResponse.error(404, e.getMessage());
        }
    }
    
    /**
     * 分页查询流程
     */
    @Operation(summary = "分页查询流程", description = "分页查询流程列表")
    @GetMapping
    public ApiResponse<Page<WorkflowDTO>> getWorkflowsByPage(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<WorkflowDTO> workflowPage = workflowService.getWorkflowsByPage(name, status, null, page, size);
        return ApiResponse.success(workflowPage);
    }
    
    /**
     * 获取所有流程
     */
    @Operation(summary = "获取所有流程", description = "获取所有流程列表")
    @GetMapping("/all")
    public ApiResponse<List<WorkflowDTO>> getAllWorkflows() {
        List<WorkflowDTO> workflows = workflowService.getAllWorkflows();
        return ApiResponse.success(workflows);
    }
    
    /**
     * 执行流程（提交到 Python Agent）
     * 创建 ai_workflow 类型任务并异步提交，通过 /api/agent/callback 接收执行结果
     */
    @Operation(summary = "执行流程", description = "将已发布的流程提交到 Python Agent 异步执行")
    @PostMapping("/{id}/execute")
    public ApiResponse<Map<String, Object>> executeWorkflow(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> params,
            HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            String userName = (String) request.getAttribute("username");

            WorkflowDTO workflow = workflowService.getWorkflowById(id);
            if (!"published".equals(workflow.getStatus())) {
                return ApiResponse.error(400, "只有已发布的流程才能执行，当前状态: " + workflow.getStatus());
            }

            // 创建任务记录
            String taskId = "TASK-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                    + "-" + id;
            Task task = new Task();
            task.setTaskId(taskId);
            task.setName("流程执行: " + workflow.getName());
            task.setType("ai_workflow");
            task.setStatus("running");
            task.setProgress(0);
            task.setUserId(userId);
            task.setUserName(userName);
            task.setDescription("执行流程: " + workflow.getName() + " (v" + workflow.getVersion() + ")");
            task.setStartTime(LocalDateTime.now());

            JSONObject paramsJson = params != null ? new JSONObject(params) : new JSONObject();
            paramsJson.put("workflowId", id);
            paramsJson.put("workflowCode", workflow.getWorkflowCode());
            paramsJson.put("workflowConfig", workflow.getConfig());
            task.setParams(paramsJson.toJSONString());

            task = taskRepository.save(task);

            // 提交到 Python Agent
            agentApiClient.submitWorkflowTask(taskId, id, paramsJson);

            log.info("流程 {} 已提交执行, taskId={}", workflow.getName(), taskId);
            return ApiResponse.success("流程执行已提交", Map.of(
                    "taskId", taskId,
                    "taskRecordId", task.getId(),
                    "message", "流程已异步提交，请通过任务ID查询执行状态"
            ));
        } catch (Exception e) {
            log.error("执行流程失败: {}", e.getMessage());
            return ApiResponse.error(500, "执行流程失败: " + e.getMessage());
        }
    }

    // ==================== 节点类型接口 ====================
    
    /**
     * 获取所有启用的节点类型
     */
    @Operation(summary = "获取节点类型", description = "获取所有启用的节点类型")
    @GetMapping("/node-types")
    public ApiResponse<List<NodeTypeDTO>> getEnabledNodeTypes() {
        List<NodeTypeDTO> nodeTypes = nodeTypeService.getEnabledNodeTypes();
        return ApiResponse.success(nodeTypes);
    }
    
    /**
     * 获取所有节点类型（包括禁用的）
     */
    @Operation(summary = "获取所有节点类型", description = "获取所有节点类型")
    @GetMapping("/node-types/all")
    public ApiResponse<List<NodeTypeDTO>> getAllNodeTypes() {
        List<NodeTypeDTO> nodeTypes = nodeTypeService.getAllNodeTypes();
        return ApiResponse.success(nodeTypes);
    }
    
    /**
     * 创建节点类型
     */
    @Operation(summary = "创建节点类型", description = "创建新的节点类型")
    @PostMapping("/node-types")
    public ApiResponse<NodeTypeDTO> createNodeType(@Valid @RequestBody NodeTypeDTO dto) {
        try {
            NodeTypeDTO nodeType = nodeTypeService.createNodeType(dto);
            return ApiResponse.success("创建节点类型成功", nodeType);
        } catch (Exception e) {
            log.error("创建节点类型失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 更新节点类型
     */
    @Operation(summary = "更新节点类型", description = "更新节点类型信息")
    @PutMapping("/node-types/{id}")
    public ApiResponse<NodeTypeDTO> updateNodeType(@PathVariable Long id, @Valid @RequestBody NodeTypeDTO dto) {
        try {
            NodeTypeDTO nodeType = nodeTypeService.updateNodeType(id, dto);
            return ApiResponse.success("更新节点类型成功", nodeType);
        } catch (Exception e) {
            log.error("更新节点类型失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 删除节点类型
     */
    @Operation(summary = "删除节点类型", description = "删除节点类型")
    @DeleteMapping("/node-types/{id}")
    public ApiResponse<Void> deleteNodeType(@PathVariable Long id) {
        try {
            nodeTypeService.deleteNodeType(id);
            return ApiResponse.success("删除节点类型成功", null);
        } catch (Exception e) {
            log.error("删除节点类型失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
}
