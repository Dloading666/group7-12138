package com.rpa.management.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpa.management.dto.WorkflowDTO;
import com.rpa.management.dto.WorkflowNodeDTO;
import com.rpa.management.entity.Workflow;
import com.rpa.management.entity.WorkflowNode;
import com.rpa.management.repository.WorkflowNodeRepository;
import com.rpa.management.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 流程服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {
    
    private final WorkflowRepository workflowRepository;
    private final WorkflowNodeRepository workflowNodeRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 创建流程
     */
    @Transactional
    public WorkflowDTO createWorkflow(WorkflowDTO dto, Long userId, String userName) {
        Workflow workflow = new Workflow();
        
        // 如果前端传了 workflowCode，使用前端的；否则自动生成
        String workflowCode = dto.getWorkflowCode();
        if (workflowCode == null || workflowCode.trim().isEmpty()) {
            workflowCode = generateWorkflowCode();
        } else {
            // 检查编码是否已存在
            if (workflowRepository.findByWorkflowCode(workflowCode).isPresent()) {
                throw new RuntimeException("流程编码已存在: " + workflowCode);
            }
        }
        
        workflow.setWorkflowCode(workflowCode);
        workflow.setName(dto.getName());
        workflow.setDescription(dto.getDescription());
        workflow.setStatus(dto.getStatus() != null ? dto.getStatus() : "draft");
        workflow.setVersion(1);
        workflow.setUserId(userId);
        workflow.setUserName(userName);
        
        workflow = workflowRepository.save(workflow);
        log.info("创建流程成功: {}", workflow.getWorkflowCode());
        
        return toDTO(workflow);
    }
    
    /**
     * 更新流程
     */
    @Transactional
    public WorkflowDTO updateWorkflow(Long id, WorkflowDTO dto) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("流程不存在: " + id));
        
        workflow.setName(dto.getName());
        workflow.setDescription(dto.getDescription());
        
        // 保存节点配置
        if (dto.getConfig() != null) {
            workflow.setConfig(dto.getConfig());
        }
        
        workflow = workflowRepository.save(workflow);
        log.info("更新流程成功: {}", workflow.getWorkflowCode());
        
        return toDTO(workflow);
    }
    
    /**
     * 发布流程
     */
    @Transactional
    public WorkflowDTO publishWorkflow(Long id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("流程不存在: " + id));
        
        workflow.setStatus("published");
        workflow.setPublishTime(LocalDateTime.now());
        workflow.setVersion(workflow.getVersion() + 1);
        
        workflow = workflowRepository.save(workflow);
        log.info("发布流程成功: {}", workflow.getWorkflowCode());
        
        return toDTO(workflow);
    }
    
    /**
     * 删除流程
     */
    @Transactional
    public void deleteWorkflow(Long id) {
        // 先删除节点
        workflowNodeRepository.deleteByWorkflowId(id);
        // 再删除流程
        workflowRepository.deleteById(id);
        log.info("删除流程成功: {}", id);
    }
    
    /**
     * 获取流程详情
     */
    public WorkflowDTO getWorkflowById(Long id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("流程不存在: " + id));
        
        WorkflowDTO dto = toDTO(workflow);
        
        // 加载节点
        List<WorkflowNode> nodes = workflowNodeRepository.findByWorkflowIdOrderByOrderAsc(id);
        dto.setNodes(nodes.stream().map(this::toNodeDTO).collect(Collectors.toList()));
        
        return dto;
    }
    
    /**
     * 分页查询流程
     */
    public Page<WorkflowDTO> getWorkflowsByPage(String name, String status, Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Workflow> workflowPage = workflowRepository.findByConditions(name, status, userId, pageable);
        
        return workflowPage.map(this::toDTO);
    }
    
    /**
     * 获取所有流程
     */
    public List<WorkflowDTO> getAllWorkflows() {
        return workflowRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 生成流程编号
     */
    private String generateWorkflowCode() {
        return "WF" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * 转换为DTO
     */
    private WorkflowDTO toDTO(Workflow workflow) {
        WorkflowDTO dto = new WorkflowDTO();
        dto.setId(workflow.getId());
        dto.setWorkflowCode(workflow.getWorkflowCode());
        dto.setName(workflow.getName());
        dto.setDescription(workflow.getDescription());
        dto.setStatus(workflow.getStatus());
        dto.setVersion(workflow.getVersion());
        dto.setUserId(workflow.getUserId());
        dto.setUserName(workflow.getUserName());
        dto.setPublishTime(workflow.getPublishTime());
        dto.setConfig(workflow.getConfig());
        dto.setCreateTime(workflow.getCreateTime());
        dto.setUpdateTime(workflow.getUpdateTime());
        
        // 计算步骤数
        dto.setStepCount(calculateStepCount(workflow.getConfig()));
        
        return dto;
    }
    
    /**
     * 计算步骤数
     */
    private Integer calculateStepCount(String config) {
        if (config == null || config.isEmpty()) {
            return 0;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode configNode = objectMapper.readTree(config);
            if (configNode.has("nodes") && configNode.get("nodes").isArray()) {
                return configNode.get("nodes").size();
            }
        } catch (Exception e) {
            log.warn("解析流程配置失败: {}", e.getMessage());
        }
        return 0;
    }
    
    /**
     * 转换节点为DTO
     */
    private WorkflowNodeDTO toNodeDTO(WorkflowNode node) {
        WorkflowNodeDTO dto = new WorkflowNodeDTO();
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
}
