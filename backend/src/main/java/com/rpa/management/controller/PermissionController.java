package com.rpa.management.controller;

import com.rpa.management.dto.ApiResponse;
import com.rpa.management.dto.PermissionDTO;
import com.rpa.management.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理控制器
 */
@Slf4j
@Tag(name = "权限管理", description = "权限资源的增删改查接口")
@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class PermissionController {
    
    private final PermissionService permissionService;
    
    /**
     * 创建权限
     */
    @Operation(summary = "创建权限", description = "创建新的权限资源")
    @PostMapping
    public ApiResponse<PermissionDTO> createPermission(@Valid @RequestBody PermissionDTO dto) {
        try {
            PermissionDTO permission = permissionService.createPermission(dto);
            return ApiResponse.success("创建权限成功", permission);
        } catch (Exception e) {
            log.error("创建权限失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 更新权限
     */
    @Operation(summary = "更新权限", description = "更新指定权限的信息")
    @PutMapping("/{id}")
    public ApiResponse<PermissionDTO> updatePermission(
            @Parameter(description = "权限ID") @PathVariable Long id,
            @Valid @RequestBody PermissionDTO dto) {
        try {
            PermissionDTO permission = permissionService.updatePermission(id, dto);
            return ApiResponse.success("更新权限成功", permission);
        } catch (Exception e) {
            log.error("更新权限失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 删除权限
     */
    @Operation(summary = "删除权限", description = "删除指定权限")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePermission(@Parameter(description = "权限ID") @PathVariable Long id) {
        try {
            permissionService.deletePermission(id);
            return ApiResponse.success("删除权限成功", null);
        } catch (Exception e) {
            log.error("删除权限失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 批量删除权限
     */
    @Operation(summary = "批量删除权限", description = "批量删除多个权限")
    @DeleteMapping("/batch")
    public ApiResponse<Void> deletePermissions(@RequestBody List<Long> ids) {
        try {
            permissionService.deletePermissions(ids);
            return ApiResponse.success("批量删除成功", null);
        } catch (Exception e) {
            log.error("批量删除权限失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 根据ID查询权限
     */
    @Operation(summary = "查询权限详情", description = "根据ID查询权限详细信息")
    @GetMapping("/{id}")
    public ApiResponse<PermissionDTO> getPermissionById(@Parameter(description = "权限ID") @PathVariable Long id) {
        try {
            PermissionDTO permission = permissionService.getPermissionById(id);
            return ApiResponse.success(permission);
        } catch (Exception e) {
            log.error("查询权限失败: {}", e.getMessage());
            return ApiResponse.error(404, e.getMessage());
        }
    }
    
    /**
     * 查询所有权限
     */
    @Operation(summary = "查询所有权限", description = "获取所有权限列表")
    @GetMapping("/all")
    public ApiResponse<List<PermissionDTO>> getAllPermissions() {
        List<PermissionDTO> permissions = permissionService.getAllPermissions();
        return ApiResponse.success(permissions);
    }
    
    /**
     * 获取权限树形结构
     */
    @Operation(summary = "获取权限树", description = "获取树形结构的权限列表")
    @GetMapping("/tree")
    public ApiResponse<List<PermissionDTO>> getPermissionTree() {
        List<PermissionDTO> tree = permissionService.getPermissionTree();
        return ApiResponse.success(tree);
    }
    
    /**
     * 更新权限状态
     */
    @Operation(summary = "更新权限状态", description = "启用或禁用权限")
    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(
            @Parameter(description = "权限ID") @PathVariable Long id,
            @Parameter(description = "状态") @RequestParam String status) {
        try {
            permissionService.updateStatus(id, status);
            return ApiResponse.success("状态更新成功", null);
        } catch (Exception e) {
            log.error("更新权限状态失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
}
