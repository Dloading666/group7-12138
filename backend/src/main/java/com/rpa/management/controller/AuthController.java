package com.rpa.management.controller;

import com.rpa.management.dto.ApiResponse;
import com.rpa.management.dto.LoginRequest;
import com.rpa.management.dto.LoginResponse;
import com.rpa.management.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Slf4j
@Tag(name = "认证管理", description = "用户登录、登出等认证相关接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    
    /**
     * 用户登录
     */
    @Operation(summary = "用户登录", description = "管理员和普通用户登录接口")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            String clientIp = getClientIp(httpRequest);
            LoginResponse response = userService.login(request, clientIp);
            log.info("用户登录成功: {}, IP: {}", request.getUsername(), clientIp);
            
            return ApiResponse.success("登录成功", response);
        } catch (Exception e) {
            log.error("用户登录失败: {}", e.getMessage());
            return ApiResponse.error(401, e.getMessage());
        }
    }
    
    /**
     * 获取当前用户信息
     */
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    @GetMapping("/userinfo")
    public ApiResponse<LoginResponse> getUserInfo(HttpServletRequest request) {
        // 从请求属性中获取用户信息（由拦截器设置）
        String username = (String) request.getAttribute("username");
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        
        // 这里可以根据需要查询更多用户信息
        LoginResponse response = LoginResponse.builder()
                .userId(userId)
                .username(username)
                .role(com.rpa.management.entity.UserRole.valueOf(role))
                .build();
        
        return ApiResponse.success(response);
    }
    
    /**
     * 登出
     */
    @Operation(summary = "用户登出", description = "退出登录")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        log.info("用户登出: {}", username);
        // JWT是无状态的，客户端删除Token即可
        return ApiResponse.success("登出成功", null);
    }
    
    /**
     * 健康检查（供 Docker healthcheck 和负载均衡器使用，无需认证）
     */
    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("ok");
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
