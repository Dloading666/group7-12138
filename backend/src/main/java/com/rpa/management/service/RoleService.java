package com.rpa.management.service;

import com.rpa.management.dto.RoleDTO;
import com.rpa.management.entity.Permission;
import com.rpa.management.entity.Role;
import com.rpa.management.repository.PermissionRepository;
import com.rpa.management.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {
    
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    
    /**
     * 创建角色
     */
    @Transactional
    public RoleDTO createRole(RoleDTO dto) {
        // 检查编码是否已存在
        if (roleRepository.existsByCode(dto.getCode())) {
            throw new RuntimeException("角色编码已存在: " + dto.getCode());
        }
        
        Role role = new Role();
        role.setName(dto.getName());
        role.setCode(dto.getCode());
        role.setDescription(dto.getDescription());
        role.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "active");
        role.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        
        // 分配权限
        if (dto.getPermissionIds() != null && !dto.getPermissionIds().isEmpty()) {
            Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(dto.getPermissionIds()));
            role.setPermissions(permissions);
        }
        
        role = roleRepository.save(role);
        log.info("创建角色成功: {}", role.getCode());
        
        return toDTO(role);
    }
    
    /**
     * 更新角色
     */
    @Transactional
    public RoleDTO updateRole(Long id, RoleDTO dto) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在: " + id));
        
        // 检查编码是否被其他角色使用
        if (roleRepository.existsByCodeAndIdNot(dto.getCode(), id)) {
            throw new RuntimeException("角色编码已被其他角色使用: " + dto.getCode());
        }
        
        role.setName(dto.getName());
        role.setCode(dto.getCode());
        role.setDescription(dto.getDescription());
        if (StringUtils.hasText(dto.getStatus())) {
            role.setStatus(dto.getStatus());
        }
        if (dto.getSortOrder() != null) {
            role.setSortOrder(dto.getSortOrder());
        }
        
        // 更新权限
        if (dto.getPermissionIds() != null) {
            Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(dto.getPermissionIds()));
            role.setPermissions(permissions);
        }
        
        role = roleRepository.save(role);
        log.info("更新角色成功: {}", role.getCode());
        
        return toDTO(role);
    }
    
    /**
     * 删除角色
     */
    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在: " + id));
        
        // 检查是否为系统内置角色
        if ("ADMIN".equals(role.getCode()) || "USER".equals(role.getCode())) {
            throw new RuntimeException("系统内置角色不能删除");
        }
        
        roleRepository.deleteById(id);
        log.info("删除角色成功: {}", role.getCode());
    }
    
    /**
     * 批量删除角色
     */
    @Transactional
    public void deleteRoles(List<Long> ids) {
        List<Role> roles = roleRepository.findByIdIn(ids);
        
        // 过滤系统内置角色
        List<Role> systemRoles = roles.stream()
                .filter(r -> "ADMIN".equals(r.getCode()) || "USER".equals(r.getCode()))
                .collect(Collectors.toList());
        
        if (!systemRoles.isEmpty()) {
            throw new RuntimeException("系统内置角色不能删除: " + 
                    systemRoles.stream().map(Role::getCode).collect(Collectors.joining(", ")));
        }
        
        roleRepository.deleteAllById(ids);
        log.info("批量删除角色成功, 数量: {}", ids.size());
    }
    
    /**
     * 根据ID查询角色
     */
    public RoleDTO getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在: " + id));
        return toDTO(role);
    }
    
    /**
     * 查询所有角色
     */
    public List<RoleDTO> getAllRoles() {
        return roleRepository.findAllByOrderBySortOrderAsc().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 查询启用的角色
     */
    public List<RoleDTO> getActiveRoles() {
        return roleRepository.findByStatusOrderBySortOrderAsc("active").stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 分页查询角色
     */
    public Page<RoleDTO> getRolesByPage(String name, String code, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("sortOrder").ascending());
        Page<Role> rolePage = roleRepository.findByConditions(name, code, status, pageable);
        return rolePage.map(this::toDTO);
    }
    
    /**
     * 更新角色状态
     */
    @Transactional
    public void updateStatus(Long id, String status) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在: " + id));
        
        // 检查是否为系统内置角色
        if ("ADMIN".equals(role.getCode())) {
            throw new RuntimeException("管理员角色不能禁用");
        }
        
        role.setStatus(status);
        roleRepository.save(role);
        log.info("更新角色状态成功: {}, 状态: {}", role.getCode(), status);
    }
    
    /**
     * 为角色分配权限
     */
    @Transactional
    public RoleDTO assignPermissions(Long roleId, List<Long> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("角色不存在: " + roleId));
        
        // 清空原有权限
        role.getPermissions().clear();
        
        // 添加新权限
        if (permissionIds != null && !permissionIds.isEmpty()) {
            Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(permissionIds));
            role.getPermissions().addAll(permissions);
        }
        
        role = roleRepository.save(role);
        log.info("为角色分配权限成功: roleId={}, permissionIds={}", roleId, permissionIds);
        
        return toDTO(role);
    }
    
    /**
     * 获取角色的权限ID列表
     */
    public List<Long> getRolePermissionIds(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("角色不存在: " + roleId));
        
        return role.getPermissions().stream()
                .map(Permission::getId)
                .collect(Collectors.toList());
    }
    
    /**
     * 转换为DTO
     */
    private RoleDTO toDTO(Role role) {
        List<Long> permissionIds = role.getPermissions().stream()
                .map(Permission::getId)
                .collect(Collectors.toList());
        
        return RoleDTO.builder()
                .id(role.getId())
                .name(role.getName())
                .code(role.getCode())
                .description(role.getDescription())
                .status(role.getStatus())
                .sortOrder(role.getSortOrder())
                .permissionIds(permissionIds.isEmpty() ? null : permissionIds)
                .createTime(role.getCreateTime())
                .updateTime(role.getUpdateTime())
                .build();
    }
}
