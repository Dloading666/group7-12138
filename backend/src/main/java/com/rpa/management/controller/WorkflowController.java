package com.rpa.management.controller;

import com.alibaba.fastjson2.JSON;
import com.rpa.management.dto.ApiResponse;
import com.rpa.management.dto.NodeTypeDTO;
import com.rpa.management.dto.TaskDTO;
import com.rpa.management.dto.WorkflowDTO;
import com.rpa.management.dto.WorkflowDebugRunDTO;
import com.rpa.management.dto.WorkflowVersionDTO;
import com.rpa.management.engine.WorkflowRunExecutor;
import com.rpa.management.service.NodeTypeService;
import com.rpa.management.service.TaskService;
import com.rpa.management.service.WorkflowDebugRunService;
import com.rpa.management.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/workflows")
@Tag(name = "工作流管理", description = "工作流草稿、版本、调试与节点类型接口")
public class WorkflowController {

    private final WorkflowService workflowService;
    private final NodeTypeService nodeTypeService;
    private final TaskService taskService;
    private final WorkflowDebugRunService workflowDebugRunService;
    private final WorkflowRunExecutor workflowRunExecutor;

    @PostMapping
    @Operation(summary = "创建工作流")
    public ApiResponse<WorkflowDTO> createWorkflow(@Valid @RequestBody WorkflowDTO dto, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            String userName = (String) request.getAttribute("username");
            return ApiResponse.success("创建工作流成功", workflowService.createWorkflow(dto, userId, userName));
        } catch (Exception ex) {
            log.error("Create workflow failed", ex);
            return ApiResponse.error(400, ex.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新工作流草稿")
    public ApiResponse<WorkflowDTO> updateWorkflow(@PathVariable Long id, @Valid @RequestBody WorkflowDTO dto) {
        try {
            return ApiResponse.success("更新工作流成功", workflowService.updateWorkflow(id, dto));
        } catch (Exception ex) {
            log.error("Update workflow failed", ex);
            return ApiResponse.error(400, ex.getMessage());
        }
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "发布工作流")
    public ApiResponse<WorkflowDTO> publishWorkflow(@PathVariable Long id) {
        try {
            return ApiResponse.success("发布工作流成功", workflowService.publishWorkflow(id));
        } catch (Exception ex) {
            log.error("Publish workflow failed", ex);
            return ApiResponse.error(400, ex.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除工作流")
    public ApiResponse<Void> deleteWorkflow(@PathVariable Long id) {
        try {
            workflowService.deleteWorkflow(id);
            return ApiResponse.success("删除工作流成功", null);
        } catch (Exception ex) {
            log.error("Delete workflow failed", ex);
            return ApiResponse.error(400, ex.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取工作流详情")
    public ApiResponse<WorkflowDTO> getWorkflowById(@PathVariable Long id) {
        try {
            return ApiResponse.success(workflowService.getWorkflowById(id));
        } catch (Exception ex) {
            log.error("Get workflow failed", ex);
            return ApiResponse.error(404, ex.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "分页查询工作流")
    public ApiResponse<Page<WorkflowDTO>> getWorkflowsByPage(@RequestParam(required = false) String name,
                                                             @RequestParam(required = false) String status,
                                                             @RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(workflowService.getWorkflowsByPage(name, status, null, page, size));
    }

    @GetMapping("/all")
    @Operation(summary = "获取全部工作流")
    public ApiResponse<List<WorkflowDTO>> getAllWorkflows() {
        return ApiResponse.success(workflowService.getAllWorkflows());
    }

    @GetMapping("/published-versions")
    @Operation(summary = "获取已发布工作流版本")
    public ApiResponse<List<WorkflowVersionDTO>> getPublishedVersions() {
        return ApiResponse.success(workflowService.getPublishedWorkflowVersions());
    }

    @GetMapping("/versions/{id}")
    @Operation(summary = "获取工作流版本详情")
    public ApiResponse<WorkflowVersionDTO> getWorkflowVersion(@PathVariable Long id) {
        return ApiResponse.success(workflowService.getWorkflowVersionById(id));
    }

    @PostMapping("/{id}/debug-runs")
    @Operation(summary = "创建草稿调试运行")
    public ApiResponse<WorkflowDebugRunDTO> createDebugRun(@PathVariable Long id,
                                                           @RequestBody(required = false) Map<String, Object> payload,
                                                           HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            String userName = (String) request.getAttribute("username");
            workflowService.getWorkflowById(id);

            Object inputPayload = payload != null ? payload.get("inputConfig") : null;
            String inputConfig;
            if (inputPayload == null) {
                inputConfig = "{}";
            } else if (inputPayload instanceof String text) {
                inputConfig = text;
            } else {
                inputConfig = JSON.toJSONString(inputPayload);
            }

            var run = workflowDebugRunService.createRun(id, inputConfig, userId, userName);
            workflowRunExecutor.executeDebugRunAsync(run.getId());
            return ApiResponse.success("草稿调试已启动", workflowDebugRunService.toDTO(run));
        } catch (Exception ex) {
            log.error("Create debug run failed", ex);
            return ApiResponse.error(400, ex.getMessage());
        }
    }

    @GetMapping("/{id}/debug-runs")
    @Operation(summary = "获取草稿调试运行列表")
    public ApiResponse<List<WorkflowDebugRunDTO>> getDebugRuns(@PathVariable Long id) {
        return ApiResponse.success(workflowDebugRunService.listRuns(id));
    }

    @GetMapping("/debug-runs/{runId}")
    @Operation(summary = "获取草稿调试运行详情")
    public ApiResponse<WorkflowDebugRunDTO> getDebugRun(@PathVariable String runId) {
        return ApiResponse.success(workflowDebugRunService.getByRunId(runId));
    }

    @PostMapping("/{id}/execute")
    @Operation(summary = "兼容旧接口执行工作流")
    public ApiResponse<Map<String, Object>> executeWorkflow(@PathVariable Long id,
                                                            @RequestBody(required = false) Map<String, Object> params,
                                                            HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            String userName = (String) request.getAttribute("username");
            WorkflowDTO workflow = workflowService.getWorkflowById(id);
            if (!"published".equals(workflow.getStatus()) || workflow.getLatestVersionId() == null) {
                return ApiResponse.error(400, "只有已发布且存在版本快照的工作流才能执行");
            }

            TaskDTO task = TaskDTO.builder()
                    .name(workflow.getName())
                    .workflowId(workflow.getId())
                    .workflowVersionId(workflow.getLatestVersionId())
                    .workflowName(workflow.getName())
                    .workflowCategory(workflow.getCategory())
                    .description(workflow.getDescription())
                    .inputConfig(params != null ? JSON.toJSONString(params) : "{}")
                    .executeType("immediate")
                    .build();

            TaskDTO created = taskService.createTask(task, userId, userName);
            TaskDTO started = taskService.startTask(created.getId());
            return ApiResponse.success("工作流任务已创建并启动", Map.of(
                    "taskId", started.getTaskId(),
                    "taskRecordId", started.getId(),
                    "latestRunId", started.getLatestRunId()
            ));
        } catch (Exception ex) {
            log.error("Execute workflow failed", ex);
            return ApiResponse.error(500, "执行工作流失败: " + ex.getMessage());
        }
    }

    @GetMapping("/node-types")
    @Operation(summary = "获取启用节点类型")
    public ApiResponse<List<NodeTypeDTO>> getEnabledNodeTypes() {
        return ApiResponse.success(nodeTypeService.getEnabledNodeTypes());
    }

    @GetMapping("/node-types/all")
    @Operation(summary = "获取全部节点类型")
    public ApiResponse<List<NodeTypeDTO>> getAllNodeTypes() {
        return ApiResponse.success(nodeTypeService.getAllNodeTypes());
    }

    @PostMapping("/node-types")
    @Operation(summary = "创建节点类型")
    public ApiResponse<NodeTypeDTO> createNodeType(@Valid @RequestBody NodeTypeDTO dto) {
        try {
            return ApiResponse.success("创建节点类型成功", nodeTypeService.createNodeType(dto));
        } catch (Exception ex) {
            log.error("Create node type failed", ex);
            return ApiResponse.error(400, ex.getMessage());
        }
    }

    @PutMapping("/node-types/{id}")
    @Operation(summary = "更新节点类型")
    public ApiResponse<NodeTypeDTO> updateNodeType(@PathVariable Long id, @Valid @RequestBody NodeTypeDTO dto) {
        try {
            return ApiResponse.success("更新节点类型成功", nodeTypeService.updateNodeType(id, dto));
        } catch (Exception ex) {
            log.error("Update node type failed", ex);
            return ApiResponse.error(400, ex.getMessage());
        }
    }

    @DeleteMapping("/node-types/{id}")
    @Operation(summary = "删除节点类型")
    public ApiResponse<Void> deleteNodeType(@PathVariable Long id) {
        try {
            nodeTypeService.deleteNodeType(id);
            return ApiResponse.success("删除节点类型成功", null);
        } catch (Exception ex) {
            log.error("Delete node type failed", ex);
            return ApiResponse.error(400, ex.getMessage());
        }
    }
}
