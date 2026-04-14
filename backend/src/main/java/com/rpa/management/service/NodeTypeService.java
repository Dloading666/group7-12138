package com.rpa.management.service;

import com.rpa.management.dto.NodeTypeDTO;
import com.rpa.management.entity.NodeType;
import com.rpa.management.repository.NodeTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 节点类型服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NodeTypeService {
    
    private final NodeTypeRepository nodeTypeRepository;
    
    /**
     * 获取所有启用的节点类型
     */
    public List<NodeTypeDTO> getEnabledNodeTypes() {
        return nodeTypeRepository.findByEnabledTrueOrderBySortOrderAsc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有节点类型
     */
    public List<NodeTypeDTO> getAllNodeTypes() {
        return nodeTypeRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 创建节点类型
     */
    @Transactional
    public NodeTypeDTO createNodeType(NodeTypeDTO dto) {
        if (nodeTypeRepository.existsByType(dto.getType())) {
            throw new RuntimeException("节点类型代码已存在: " + dto.getType());
        }
        
        NodeType nodeType = new NodeType();
        nodeType.setType(dto.getType());
        nodeType.setName(dto.getName());
        nodeType.setIcon(dto.getIcon());
        nodeType.setColor(dto.getColor());
        nodeType.setCategory(dto.getCategory());
        nodeType.setSortOrder(dto.getSortOrder());
        nodeType.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
        nodeType.setDefaultConfig(dto.getDefaultConfig());
        nodeType.setDescription(dto.getDescription());
        
        nodeType = nodeTypeRepository.save(nodeType);
        log.info("创建节点类型成功: {}", nodeType.getType());
        
        return toDTO(nodeType);
    }
    
    /**
     * 更新节点类型
     */
    @Transactional
    public NodeTypeDTO updateNodeType(Long id, NodeTypeDTO dto) {
        NodeType nodeType = nodeTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("节点类型不存在: " + id));
        
        nodeType.setName(dto.getName());
        nodeType.setIcon(dto.getIcon());
        nodeType.setColor(dto.getColor());
        nodeType.setCategory(dto.getCategory());
        nodeType.setSortOrder(dto.getSortOrder());
        nodeType.setEnabled(dto.getEnabled());
        nodeType.setDefaultConfig(dto.getDefaultConfig());
        nodeType.setDescription(dto.getDescription());
        
        nodeType = nodeTypeRepository.save(nodeType);
        log.info("更新节点类型成功: {}", nodeType.getType());
        
        return toDTO(nodeType);
    }
    
    /**
     * 删除节点类型
     */
    @Transactional
    public void deleteNodeType(Long id) {
        nodeTypeRepository.deleteById(id);
        log.info("删除节点类型成功: {}", id);
    }
    
    /**
     * 转换为DTO
     */
    private NodeTypeDTO toDTO(NodeType nodeType) {
        NodeTypeDTO dto = new NodeTypeDTO();
        dto.setId(nodeType.getId());
        dto.setType(nodeType.getType());
        dto.setName(nodeType.getName());
        dto.setIcon(nodeType.getIcon());
        dto.setColor(nodeType.getColor());
        dto.setCategory(nodeType.getCategory());
        dto.setSortOrder(nodeType.getSortOrder());
        dto.setEnabled(nodeType.getEnabled());
        dto.setDefaultConfig(nodeType.getDefaultConfig());
        dto.setDescription(nodeType.getDescription());
        dto.setCreateTime(nodeType.getCreateTime());
        dto.setUpdateTime(nodeType.getUpdateTime());
        return dto;
    }
}
