package com.rpa.management.dto;

import com.rpa.management.common.enums.UserStatus;
import com.rpa.management.entity.Role;
import com.rpa.management.entity.User;

import java.time.LocalDateTime;

public record UserDto(
    Long id,
    String username,
    String realName,
    String email,
    String phone,
    String avatar,
    Long roleId,
    String roleCode,
    String roleName,
    UserStatus status,
    boolean superAdmin,
    LocalDateTime lastLoginAt,
    String lastLoginIp
) {
    public static UserDto from(User user, Role role) {
        return new UserDto(
            user.getId(),
            user.getUsername(),
            user.getRealName(),
            user.getEmail(),
            user.getPhone(),
            user.getAvatar(),
            role == null ? null : role.getId(),
            role == null ? null : role.getCode(),
            role == null ? null : role.getName(),
            user.getStatus(),
            user.isSuperAdmin(),
            user.getLastLoginAt(),
            user.getLastLoginIp()
        );
    }
}
