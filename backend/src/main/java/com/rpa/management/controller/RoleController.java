package com.rpa.management.controller;

import com.rpa.management.dto.ApiResponse;
import com.rpa.management.dto.RoleDTO;
import com.rpa.management.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 */
@Slf4j
@Tag(name = "角色管理", description = "角色的增删改查接口")
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {
    
    private final RoleService roleService;
    
    /**
     * 创建角色
     */
    @Operation(summary = "创建角色", description = "创建新的角色")
    @PostMapping
    public ApiResponse<RoleDTO> createRole(@Valid @RequestBody RoleDTO dto) {
        try {
            RoleDTO role = roleService.createRole(dto);
            return ApiResponse.success("创建角色成功", role);
        } catch (Exception e) {
            log.error("创建角色失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 更新角色
     */
    @Operation(summary = "更新角色", description = "更新指定角色的信息")
    @PutMapping("/{id}")
    public ApiResponse<RoleDTO> updateRole(
            @Parameter(description = "角色ID") @PathVariable Long id,
            @Valid @RequestBody RoleDTO dto) {
        try {
            RoleDTO role = roleService.updateRole(id, dto);
            return ApiResponse.success("更新角色成功", role);
        } catch (Exception e) {
            log.error("更新角色失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 删除角色
     */
    @Operation(summary = "删除角色", description = "删除指定角色")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRole(@Parameter(description = "角色ID") @PathVariable Long id) {
        try {
            roleService.deleteRole(id);
            return ApiResponse.success("删除角色成功", null);
        } catch (Exception e) {
            log.error("删除角色失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 批量删除角色
     */
    @Operation(summary = "批量删除角色", description = "批量删除多个角色")
    @DeleteMapping("/batch")
    public ApiResponse<Void> deleteRoles(@RequestBody List<Long> ids) {
        try {
            roleService.deleteRoles(ids);
            return ApiResponse.success("批量删除成功", null);
        } catch (Exception e) {
            log.error("批量删除角色失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 根据ID查询角色
     */
    @Operation(summary = "查询角色详情", description = "根据ID查询角色详细信息")
    @GetMapping("/{id}")
    public ApiResponse<RoleDTO> getRoleById(@Parameter(description = "角色ID") @PathVariable Long id) {
        try {
            RoleDTO role = roleService.getRoleById(id);
            return ApiResponse.success(role);
        } catch (Exception e) {
            log.error("查询角色失败: {}", e.getMessage());
            return ApiResponse.error(404, e.getMessage());
        }
    }
    
    /**
     * 查询所有角色
     */
    @Operation(summary = "查询所有角色", description = "获取所有角色列表")
    @GetMapping("/all")
    public ApiResponse<List<RoleDTO>> getAllRoles() {
        List<RoleDTO> roles = roleService.getAllRoles();
        return ApiResponse.success(roles);
    }
    
    /**
     * 查询启用的角色
     */
    @Operation(summary = "查询启用的角色", description = "获取所有启用状态的角色列表")
    @GetMapping("/active")
    public ApiResponse<List<RoleDTO>> getActiveRoles() {
        List<RoleDTO> roles = roleService.getActiveRoles();
        return ApiResponse.success(roles);
    }
    
    /**
     * 分页查询角色
     */
    @Operation(summary = "分页查询角色", description = "分页查询角色列表，支持条件筛选")
    @GetMapping
    public ApiResponse<Page<RoleDTO>> getRolesByPage(
            @Parameter(description = "角色名称") @RequestParam(required = false) String name,
            @Parameter(description = "角色编码") @RequestParam(required = false) String code,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        Page<RoleDTO> rolePage = roleService.getRolesByPage(name, code, status, page, size);
        return ApiResponse.success(rolePage);
    }
    
    /**
     * 更新角色状态
     */
    @Operation(summary = "更新角色状态", description = "启用或禁用角色")
    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(
            @Parameter(description = "角色ID") @PathVariable Long id,
            @Parameter(description = "状态") @RequestParam String status) {
        try {
            roleService.updateStatus(id, status);
            return ApiResponse.success("状态更新成功", null);
        } catch (Exception e) {
            log.error("更新角色状态失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 为角色分配权限
     */
    @Operation(summary = "分配权限", description = "为指定角色分配权限")
    @PutMapping("/{id}/permissions")
    public ApiResponse<RoleDTO> assignPermissions(
            @Parameter(description = "角色ID") @PathVariable Long id,
            @RequestBody List<Long> permissionIds) {
        try {
            RoleDTO role = roleService.assignPermissions(id, permissionIds);
            return ApiResponse.success("分配权限成功", role);
        } catch (Exception e) {
            log.error("分配权限失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 获取角色的权限ID列表
     */
    @Operation(summary = "获取角色权限", description = "获取指定角色的权限ID列表")
    @GetMapping("/{id}/permissions")
    public ApiResponse<List<Long>> getRolePermissions(@Parameter(description = "角色ID") @PathVariable Long id) {
        try {
            List<Long> permissionIds = roleService.getRolePermissionIds(id);
            return ApiResponse.success(permissionIds);
        } catch (Exception e) {
            log.error("获取角色权限失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
}
