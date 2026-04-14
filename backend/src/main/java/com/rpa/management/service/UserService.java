package com.rpa.management.service;

import com.rpa.management.dto.LoginRequest;
import com.rpa.management.dto.LoginResponse;
import com.rpa.management.dto.UserDTO;
import com.rpa.management.entity.User;
import com.rpa.management.entity.UserRole;
import com.rpa.management.repository.UserRepository;
import com.rpa.management.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    
    /**
     * 用户登录
     */
    @Transactional
    public LoginResponse login(LoginRequest request, String clientIp) {
        // 查询用户
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));
        
        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        // 检查账号状态
        if (!"active".equals(user.getStatus())) {
            throw new RuntimeException("账号已被禁用，请联系管理员");
        }
        
        // 更新登录信息
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(clientIp);
        userRepository.save(user);
        
        // 生成Token
        String token = jwtUtils.generateToken(
                user.getUsername(),
                user.getId(),
                user.getRole().name()
        );
        
        // 构建响应
        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getExpiration())
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .roleDisplayName(user.getRole().getDisplayName())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .build();
    }
    
    /**
     * 创建用户
     */
    @Transactional
    public UserDTO createUser(UserDTO dto) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("用户名已存在: " + dto.getUsername());
        }
        
        // 检查密码是否为空
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new RuntimeException("密码不能为空");
        }
        
        // 检查邮箱是否已存在
        if (StringUtils.hasText(dto.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("邮箱已被使用: " + dto.getEmail());
        }
        
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setRole(StringUtils.hasText(dto.getRole()) ? UserRole.valueOf(dto.getRole()) : UserRole.USER);
        user.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "active");
        user.setAvatar(dto.getAvatar());
        
        user = userRepository.save(user);
        log.info("创建用户成功: {}", user.getUsername());
        
        return toDTO(user);
    }
    
    /**
     * 更新用户
     */
    @Transactional
    public UserDTO updateUser(Long id, UserDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + id));
        
        // 如果提供了用户名，检查是否被其他用户使用
        if (StringUtils.hasText(dto.getUsername()) && 
            !dto.getUsername().equals(user.getUsername()) &&
            userRepository.existsByUsernameAndIdNot(dto.getUsername(), id)) {
            throw new RuntimeException("用户名已被其他用户使用: " + dto.getUsername());
        }
        
        // 更新用户名
        if (StringUtils.hasText(dto.getUsername())) {
            user.setUsername(dto.getUsername());
        }
        
        // 如果提供了新密码，则更新密码
        if (StringUtils.hasText(dto.getPassword())) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        
        // 更新真实姓名
        if (dto.getRealName() != null) {
            user.setRealName(dto.getRealName());
        }
        
        // 更新邮箱
        if (dto.getEmail() != null) {
            // 检查邮箱是否被其他用户使用
            if (StringUtils.hasText(dto.getEmail()) && 
                !dto.getEmail().equals(user.getEmail()) &&
                userRepository.existsByEmail(dto.getEmail())) {
                throw new RuntimeException("邮箱已被使用: " + dto.getEmail());
            }
            user.setEmail(dto.getEmail());
        }
        
        // 更新手机号
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        
        if (StringUtils.hasText(dto.getRole())) {
            user.setRole(UserRole.valueOf(dto.getRole()));
        }
        
        if (StringUtils.hasText(dto.getStatus())) {
            // 检查是否禁用管理员
            if ("inactive".equals(dto.getStatus()) && UserRole.ADMIN.equals(user.getRole())) {
                throw new RuntimeException("不能禁用管理员账号");
            }
            user.setStatus(dto.getStatus());
        }
        
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        
        user = userRepository.save(user);
        log.info("更新用户成功: {}", user.getUsername());
        
        return toDTO(user);
    }
    
    /**
     * 删除用户
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + id));
        
        // 不能删除管理员
        if (UserRole.ADMIN.equals(user.getRole())) {
            throw new RuntimeException("不能删除管理员账号");
        }
        
        // 不能删除自己
        // Long currentUserId = getCurrentUserId(); // 需要从上下文获取
        // if (currentUserId.equals(id)) {
        //     throw new RuntimeException("不能删除自己的账号");
        // }
        
        userRepository.deleteById(id);
        log.info("删除用户成功: {}", user.getUsername());
    }
    
    /**
     * 批量删除用户
     */
    @Transactional
    public void deleteUsers(List<Long> ids) {
        List<User> users = userRepository.findAllById(ids);
        
        // 检查是否包含管理员
        List<User> admins = users.stream()
                .filter(u -> UserRole.ADMIN.equals(u.getRole()))
                .collect(Collectors.toList());
        
        if (!admins.isEmpty()) {
            throw new RuntimeException("不能删除管理员账号: " + 
                    admins.stream().map(User::getUsername).collect(Collectors.joining(", ")));
        }
        
        userRepository.deleteAllById(ids);
        log.info("批量删除用户成功, 数量: {}", ids.size());
    }
    
    /**
     * 根据ID查询用户
     */
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + id));
        return toDTO(user);
    }
    
    /**
     * 根据用户名查询用户
     */
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
    
    /**
     * 查询所有用户
     */
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll(Sort.by("createTime").descending()).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 分页查询用户
     */
    public Page<UserDTO> getUsersByPage(String username, String realName, String role, 
                                         String status, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createTime").descending());
        UserRole roleEnum = StringUtils.hasText(role) ? UserRole.valueOf(role) : null;
        String usernameParam = StringUtils.hasText(username) ? username : null;
        String realNameParam = StringUtils.hasText(realName) ? realName : null;
        String statusParam = StringUtils.hasText(status) ? status : null;
        Page<User> userPage = userRepository.findByConditions(usernameParam, realNameParam, roleEnum, statusParam, pageable);
        return userPage.map(this::toDTO);
    }
    
    /**
     * 更新用户状态
     */
    @Transactional
    public void updateStatus(Long id, String status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + id));
        
        // 不能禁用管理员
        if ("inactive".equals(status) && UserRole.ADMIN.equals(user.getRole())) {
            throw new RuntimeException("不能禁用管理员账号");
        }
        
        user.setStatus(status);
        userRepository.save(user);
        log.info("更新用户状态成功: {}, 状态: {}", user.getUsername(), status);
    }
    
    /**
     * 重置密码
     */
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + id));
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("重置密码成功: {}", user.getUsername());
    }
    
    /**
     * 获取用户统计数据
     */
    public UserStats getUserStats() {
        UserStats stats = new UserStats();
        stats.setTotal(userRepository.count());
        stats.setActive(userRepository.countByStatus("active"));
        stats.setInactive(userRepository.countByStatus("inactive"));
        return stats;
    }
    
    /**
     * 转换为DTO
     */
    private UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .status(user.getStatus())
                .avatar(user.getAvatar())
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .lastLoginTime(user.getLastLoginTime())
                .lastLoginIp(user.getLastLoginIp())
                .build();
    }
    
    /**
     * 用户统计数据
     */
    @lombok.Data
    public static class UserStats {
        private long total;
        private long active;
        private long inactive;
    }
}
