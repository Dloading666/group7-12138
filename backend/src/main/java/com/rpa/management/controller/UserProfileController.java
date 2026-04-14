package com.rpa.management.controller;

import com.rpa.management.dto.ApiResponse;
import com.rpa.management.dto.PermissionDTO;
import com.rpa.management.dto.UserDTO;
import com.rpa.management.entity.Permission;
import com.rpa.management.entity.Role;
import com.rpa.management.entity.User;
import com.rpa.management.repository.PermissionRepository;
import com.rpa.management.repository.RoleRepository;
import com.rpa.management.repository.UserRepository;
import com.rpa.management.service.PermissionService;
import com.rpa.management.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户个人中心控制器
 */
@Slf4j
@Tag(name = "个人中心", description = "用户个人资料管理接口")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer")
public class UserProfileController {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserService userService;
    private final PermissionService permissionService;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * 获取个人资料
     */
    @Operation(summary = "获取个人资料", description = "获取当前登录用户的个人资料")
    @GetMapping("/profile")
    public ApiResponse<UserDTO> getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }
        
        UserDTO user = userService.getUserById(userId);
        return ApiResponse.success(user);
    }
    
    /**
     * 更新个人资料
     */
    @Operation(summary = "更新个人资料", description = "更新当前用户的个人资料")
    @PutMapping("/profile")
    public ApiResponse<UserDTO> updateProfile(
            HttpServletRequest request,
            @RequestBody Map<String, String> updates) {
        
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }
        
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            // 更新允许修改的字段
            if (updates.containsKey("realName")) {
                user.setRealName(updates.get("realName"));
            }
            if (updates.containsKey("email")) {
                user.setEmail(updates.get("email"));
            }
            if (updates.containsKey("phone")) {
                user.setPhone(updates.get("phone"));
            }
            if (updates.containsKey("avatar")) {
                user.setAvatar(updates.get("avatar"));
            }
            
            userRepository.save(user);
            log.info("用户资料更新成功: userId={}", userId);
            
            return ApiResponse.success("更新成功", userService.getUserById(userId));
        } catch (Exception e) {
            log.error("更新个人资料失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 修改密码
     */
    @Operation(summary = "修改密码", description = "修改当前用户的密码")
    @PutMapping("/password")
    public ApiResponse<Void> changePassword(
            HttpServletRequest request,
            @RequestBody Map<String, String> passwordData) {
        
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }
        
        String oldPassword = passwordData.get("oldPassword");
        String newPassword = passwordData.get("newPassword");
        
        if (oldPassword == null || newPassword == null) {
            return ApiResponse.error(400, "原密码和新密码不能为空");
        }
        
        if (newPassword.length() < 6 || newPassword.length() > 20) {
            return ApiResponse.error(400, "密码长度必须在6-20个字符之间");
        }
        
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            // 验证原密码
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                return ApiResponse.error(400, "原密码错误");
            }
            
            // 更新密码
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            
            log.info("用户密码修改成功: userId={}", userId);
            return ApiResponse.success("密码修改成功", null);
        } catch (Exception e) {
            log.error("修改密码失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    /**
     * 获取当前用户的权限列表
     */
    @Operation(summary = "获取用户权限", description = "获取当前登录用户的权限列表")
    @GetMapping("/permissions")
    public ApiResponse<List<PermissionDTO>> getUserPermissions(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String roleCode = (String) request.getAttribute("role");
        
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }
        
        try {
            // 管理员拥有所有权限
            if ("ADMIN".equals(roleCode)) {
                List<PermissionDTO> allPermissions = permissionService.getAllPermissions();
                return ApiResponse.success(allPermissions);
            }
            
            // 获取用户角色对应的权限
            Role role = roleRepository.findByCode(roleCode).orElse(null);
            if (role == null || role.getPermissions() == null || role.getPermissions().isEmpty()) {
                return ApiResponse.success(new ArrayList<>());
            }
            
            // 从角色中获取权限详情
            List<PermissionDTO> permissions = role.getPermissions().stream()
                    .map(permission -> PermissionDTO.builder()
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
                            .build())
                    .collect(java.util.stream.Collectors.toList());
            
            return ApiResponse.success(permissions);
        } catch (Exception e) {
            log.error("获取用户权限失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }
}
