package com.rpa.management.controller;

import com.rpa.management.dto.ApiResponse;
import com.rpa.management.dto.NodeTypeDTO;
import com.rpa.management.dto.WorkflowDTO;
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

import java.util.List;

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
