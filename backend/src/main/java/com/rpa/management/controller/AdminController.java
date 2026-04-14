package com.rpa.management.controller;

import com.rpa.management.dto.ApiResponse;
import com.rpa.management.dto.UserDTO;
import com.rpa.management.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员控制器
 * 仅管理员可访问
 */
@Tag(name = "管理员功能", description = "仅管理员可访问的接口")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer")
public class AdminController {
    
    private final UserService userService;
    
    /**
     * 获取所有用户列表
     */
    @Operation(summary = "获取所有用户", description = "管理员获取所有用户列表")
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ApiResponse.success("获取成功", users);
    }
    
    /**
     * 创建用户
     */
    @Operation(summary = "创建用户", description = "管理员创建新用户")
    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserDTO> createUser(@RequestBody UserDTO request) {
        UserDTO user = userService.createUser(request);
        return ApiResponse.success("创建成功", user);
    }
    
    /**
     * 更新用户
     */
    @Operation(summary = "更新用户", description = "管理员更新用户信息")
    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserDTO> updateUser(@PathVariable Long id, @RequestBody UserDTO request) {
        UserDTO user = userService.updateUser(id, request);
        return ApiResponse.success("更新成功", user);
    }
    
    /**
     * 删除用户
     */
    @Operation(summary = "删除用户", description = "管理员删除用户")
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.success("删除成功", null);
    }
    
    /**
     * 禁用用户
     */
    @Operation(summary = "禁用用户", description = "管理员禁用指定用户")
    @PutMapping("/users/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> disableUser(@PathVariable Long id) {
        userService.updateStatus(id, "inactive");
        return ApiResponse.success("用户已禁用", null);
    }
    
    /**
     * 启用用户
     */
    @Operation(summary = "启用用户", description = "管理员启用指定用户")
    @PutMapping("/users/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> enableUser(@PathVariable Long id) {
        userService.updateStatus(id, "active");
        return ApiResponse.success("用户已启用", null);
    }
    
    /**
     * 重置密码
     */
    @Operation(summary = "重置密码", description = "管理员重置用户密码")
    @PutMapping("/users/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @RequestParam String password) {
        userService.resetPassword(id, password);
        return ApiResponse.success("密码重置成功", null);
    }
    
    /**
     * 系统统计信息
     */
    @Operation(summary = "系统统计", description = "获取系统统计数据")
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserService.UserStats> getStatistics() {
        UserService.UserStats stats = userService.getUserStats();
        return ApiResponse.success(stats);
    }
}
