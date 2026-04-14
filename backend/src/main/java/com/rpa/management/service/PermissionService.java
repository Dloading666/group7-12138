package com.rpa.management.service;

import com.rpa.management.dto.PermissionDTO;
import com.rpa.management.entity.Permission;
import com.rpa.management.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 权限服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {
    
    private final PermissionRepository permissionRepository;
    
    /**
     * 创建权限
     */
    @Transactional
    public PermissionDTO createPermission(PermissionDTO dto) {
        if (permissionRepository.existsByCode(dto.getCode())) {
            throw new RuntimeException("权限编码已存在: " + dto.getCode());
        }
        
        Permission permission = new Permission();
        permission.setName(dto.getName());
        permission.setCode(dto.getCode());
        permission.setType(dto.getType() != null ? dto.getType() : "menu");
        permission.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        permission.setPath(dto.getPath());
        permission.setIcon(dto.getIcon());
        permission.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        permission.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "active");
        permission.setDescription(dto.getDescription());
        
        permission = permissionRepository.save(permission);
        log.info("创建权限成功: {}", permission.getCode());
        
        return toDTO(permission);
    }
    
    /**
     * 更新权限
     */
    @Transactional
    public PermissionDTO updatePermission(Long id, PermissionDTO dto) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("权限不存在: " + id));
        
        // 检查编码是否被其他权限使用
        if (permissionRepository.existsByCode(dto.getCode()) && !permission.getCode().equals(dto.getCode())) {
            throw new RuntimeException("权限编码已被使用: " + dto.getCode());
        }
        
        permission.setName(dto.getName());
        permission.setCode(dto.getCode());
        permission.setType(dto.getType());
        permission.setParentId(dto.getParentId());
        permission.setPath(dto.getPath());
        permission.setIcon(dto.getIcon());
        permission.setSortOrder(dto.getSortOrder());
        if (StringUtils.hasText(dto.getStatus())) {
            permission.setStatus(dto.getStatus());
        }
        permission.setDescription(dto.getDescription());
        
        permission = permissionRepository.save(permission);
        log.info("更新权限成功: {}", permission.getCode());
        
        return toDTO(permission);
    }
    
    /**
     * 删除权限
     */
    @Transactional
    public void deletePermission(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("权限不存在: " + id));
        
        // 检查是否有子权限
        List<Permission> children = permissionRepository.findByParentIdOrderBySortOrderAsc(id);
        if (!children.isEmpty()) {
            throw new RuntimeException("存在子权限，请先删除子权限");
        }
        
        permissionRepository.deleteById(id);
        log.info("删除权限成功: {}", permission.getCode());
    }
    
    /**
     * 批量删除权限
     */
    @Transactional
    public void deletePermissions(List<Long> ids) {
        for (Long id : ids) {
            try {
                deletePermission(id);
            } catch (Exception e) {
                log.warn("删除权限失败: id={}, reason={}", id, e.getMessage());
            }
        }
    }
    
    /**
     * 根据ID查询权限
     */
    public PermissionDTO getPermissionById(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("权限不存在: " + id));
        return toDTO(permission);
    }
    
    /**
     * 查询所有权限
     */
    public List<PermissionDTO> getAllPermissions() {
        return permissionRepository.findAllByOrderBySortOrderAsc().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取权限树形结构
     */
    public List<PermissionDTO> getPermissionTree() {
        List<Permission> allPermissions = permissionRepository.findAllByOrderBySortOrderAsc();
        return buildTree(allPermissions, 0L);
    }
    
    /**
     * 根据ID列表获取权限
     */
    public List<PermissionDTO> getPermissionsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        return permissionRepository.findByIdIn(ids).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 更新权限状态
     */
    @Transactional
    public void updateStatus(Long id, String status) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("权限不存在: " + id));
        
        permission.setStatus(status);
        permissionRepository.save(permission);
        log.info("更新权限状态成功: {}, 状态: {}", permission.getCode(), status);
    }
    
    /**
     * 构建树形结构
     */
    private List<PermissionDTO> buildTree(List<Permission> permissions, Long parentId) {
        List<PermissionDTO> tree = new ArrayList<>();
        
        for (Permission permission : permissions) {
            if ((parentId == 0 && (permission.getParentId() == null || permission.getParentId() == 0))
                    || (parentId != 0 && parentId.equals(permission.getParentId()))) {
                PermissionDTO dto = toDTO(permission);
                
                // 递归查找子权限
                List<PermissionDTO> children = buildTree(permissions, permission.getId());
                if (!children.isEmpty()) {
                    dto.setChildren(children);
                }
                
                tree.add(dto);
            }
        }
        
        return tree;
    }
    
    /**
     * 转换为DTO
     */
    private PermissionDTO toDTO(Permission permission) {
        return PermissionDTO.builder()
                .id(permission.getId())
                .name(permission.getName())
                .code(permission.getCode())
                .type(permission.getType())
                .parentId(permission.getParentId())
                .path(permission.getPath())
                .icon(permission.getIcon())
                .sortOrder(permission.getSortOrder())
                .status(permission.getStatus())
                .description(permission.getDescription())
                .createTime(permission.getCreateTime())
                .build();
    }
}
