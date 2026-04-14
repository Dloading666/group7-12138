package com.rpa.management.controller;

import com.rpa.management.dto.ApiResponse;
import com.rpa.management.dto.UserDTO;
import com.rpa.management.service.UserService;
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
 * 用户管理控制器
 */
@Slf4j
@Tag(name = "用户管理", description = "用户的增删改查接口")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    /**
     * 创建用户
     */
    @Operation(summary = "创建用户", description = "创建新用户")
    @PostMapping
    public ApiResponse<UserDTO> createUser(@RequestBody UserDTO dto) {
        try {
            UserDTO user = userService.createUser(dto);
            return ApiResponse.success("创建用户成功", user);
        } catch (Exception e) {
            log.error("创建用户失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 更新用户
     */
    @Operation(summary = "更新用户", description = "更新指定用户的信息")
    @PutMapping("/{id}")
    public ApiResponse<UserDTO> updateUser(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @RequestBody UserDTO dto) {
        try {
            UserDTO user = userService.updateUser(id, dto);
            return ApiResponse.success("更新用户成功", user);
        } catch (Exception e) {
            log.error("更新用户失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 删除用户
     */
    @Operation(summary = "删除用户", description = "删除指定用户")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@Parameter(description = "用户ID") @PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ApiResponse.success("删除用户成功", null);
        } catch (Exception e) {
            log.error("删除用户失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 批量删除用户
     */
    @Operation(summary = "批量删除用户", description = "批量删除多个用户")
    @DeleteMapping("/batch")
    public ApiResponse<Void> deleteUsers(@RequestBody List<Long> ids) {
        try {
            userService.deleteUsers(ids);
            return ApiResponse.success("批量删除成功", null);
        } catch (Exception e) {
            log.error("批量删除用户失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 根据ID查询用户
     */
    @Operation(summary = "查询用户详情", description = "根据ID查询用户详细信息")
    @GetMapping("/{id}")
    public ApiResponse<UserDTO> getUserById(@Parameter(description = "用户ID") @PathVariable Long id) {
        try {
            UserDTO user = userService.getUserById(id);
            return ApiResponse.success(user);
        } catch (Exception e) {
            log.error("查询用户失败: {}", e.getMessage());
            return ApiResponse.error(404, e.getMessage());
        }
    }
    
    /**
     * 查询所有用户
     */
    @Operation(summary = "查询所有用户", description = "获取所有用户列表")
    @GetMapping("/all")
    public ApiResponse<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ApiResponse.success(users);
    }
    
    /**
     * 分页查询用户
     */
    @Operation(summary = "分页查询用户", description = "分页查询用户列表，支持条件筛选")
    @GetMapping
    public ApiResponse<Page<UserDTO>> getUsersByPage(
            @Parameter(description = "用户名") @RequestParam(required = false) String username,
            @Parameter(description = "真实姓名") @RequestParam(required = false) String realName,
            @Parameter(description = "角色") @RequestParam(required = false) String role,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        Page<UserDTO> userPage = userService.getUsersByPage(username, realName, role, status, page, size);
        return ApiResponse.success(userPage);
    }
    
    /**
     * 更新用户状态
     */
    @Operation(summary = "更新用户状态", description = "启用或禁用用户")
    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "状态") @RequestParam String status) {
        try {
            userService.updateStatus(id, status);
            return ApiResponse.success("状态更新成功", null);
        } catch (Exception e) {
            log.error("更新用户状态失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 重置密码
     */
    @Operation(summary = "重置密码", description = "重置用户密码")
    @PutMapping("/{id}/password")
    public ApiResponse<Void> resetPassword(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "新密码") @RequestParam String password) {
        try {
            userService.resetPassword(id, password);
            return ApiResponse.success("密码重置成功", null);
        } catch (Exception e) {
            log.error("重置密码失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 获取用户统计数据
     */
    @Operation(summary = "获取用户统计", description = "获取用户统计数据")
    @GetMapping("/stats")
    public ApiResponse<UserService.UserStats> getUserStats() {
        UserService.UserStats stats = userService.getUserStats();
        return ApiResponse.success(stats);
    }
}
